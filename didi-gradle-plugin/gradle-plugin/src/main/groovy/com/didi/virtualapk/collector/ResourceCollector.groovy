package com.didi.virtualapk.collector

import com.android.build.gradle.tasks.ProcessAndroidResources
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.res.ResourceEntry
import com.didi.virtualapk.collector.res.StyleableEntry
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import org.gradle.api.Project

/**
 * Collect all(host+plugin) resources&styleables in the APK and reassign the resource ID
 *
 * @author zhengtao
 */
class ResourceCollector {

    Project project
    VAExtension virtualApk
    VAExtension.VAContext vaContext

    /**
     * Gradle task of process resource in Android build system
     */
    private ProcessAndroidResources processResTask
    /**
     * R symbol File that records all resources, generated by aapt
     */
    private File allRSymbolFile
    /**
     * Host R symbol File, need saved after the host APK compilation
     */
    private File hostRSymbolFile

    /**
     * Map of all resources, KEY is the resource type, e.g. drawable, VALUE is all entries of this type
     */
    ListMultimap<String, ResourceEntry> allResources = ArrayListMultimap.create()
    /**
     * List of all styleables
     */
    List<StyleableEntry> allStyleables = Lists.newArrayList()


    private ListMultimap<String, ResourceEntry> hostResources = ArrayListMultimap.create()
    private List<StyleableEntry> hostStyleables = Lists.newArrayList()

    /**
     * pluginResources = allResources - hostResources
     */
    ListMultimap<String, ResourceEntry> pluginResources = ArrayListMultimap.create()
    List<StyleableEntry> pluginStyleables = Lists.newArrayList()

    public ResourceCollector(Project project, ProcessAndroidResources par) {

        this.project = project
        virtualApk = project.virtualApk
        vaContext = virtualApk.getVaContext()

        processResTask = par

        allRSymbolFile = par.textSymbolOutputFile
        hostRSymbolFile = vaContext.hostSymbolFile
    }

    /**
     * Perform resource collection and ID redistribution
     */
    def collect() {

        //1、First, collect all resources by parsing the R symbol file.
        parseResEntries(allRSymbolFile, allResources, allStyleables)

        //2、Then, collect host resources by parsing the host apk R symbol file, should be stripped.
        parseResEntries(hostRSymbolFile, hostResources, hostStyleables)

        //3、Compute the resources that should be retained in the plugin apk.
        filterPluginResources()

        //4、Reassign the resource ID. If the resource entry exists in host apk, the reassign ID
        //   should be same with value in host apk; If the resource entry is owned by plugin project,
        //   then we should recalculate the ID value.
        reassignPluginResourceId()

        //5、Collect all the resources in the retained AARs, to regenerate the R java file that uses the new resource ID
        vaContext.retainedAarLibs.each {
            gatherReservedAarResources(it)
        }
    }

    /**
     * Collect resources and styleables by parsing R symbol file
     * @param RSymbolFile R symbol file records the resource entries
     * @param resourcesMap Map used to store resources
     * @param styleableList List used to store styleables
     */
    private void parseResEntries(File RSymbolFile, ListMultimap resourcesMap, List styleableList) {
        if (!RSymbolFile.exists()) {
            return
        }
        RSymbolFile.eachLine { line ->
            /**
             *  Line Content:
             *  Common Res:  int string abc_action_bar_home_description 0x7f090000
             *  Styleable:   int[] styleable TagLayout { 0x010100af, 0x7f0102b5, 0x7f0102b6 }*            or int styleable TagLayout_android_gravity 0
             */
            if (!line.empty) {
                def tokenizer = new StringTokenizer(line)
                def valueType = tokenizer.nextToken()     // value type (int or int[])
                def resType = tokenizer.nextToken()      // resource type (attr/string/color etc.)
                def resName = tokenizer.nextToken()
                def resId = tokenizer.nextToken('\r\n').trim()

                if (resType == 'styleable') {
                    styleableList.add(new StyleableEntry(resName, resId, valueType))
                } else {
                    resourcesMap.put(resType, new ResourceEntry(resType, resName, Integer.decode(resId)))
                }
            }
        }
    }

    /**
     * Filter out the resources that need to be retained in the plugin apk,
     * pluginResources = allResources - hostResources
     */
    private void filterPluginResources() {
        allResources.values().each {
            def index = hostResources.get(it.resourceType).indexOf(it)
            if (index >= 0) {
                /**
                 * If the resource entry exists in host apk, assign the host resource ID of this entry
                 * as the new resource id.
                 * Then replace the object reference in host resource collection with the object
                 * in all resource collection, to make both of them point to the same object
                 */
                it.newResourceId = hostResources.get(it.resourceType).get(index).resourceId
                hostResources.get(it.resourceType).set(index, it)
            } else {
                pluginResources.put(it.resourceType, it)
            }
        }

        allStyleables.each {
            def index = hostStyleables.indexOf(it)
            if (index >= 0) {
                /**
                 * Do not support the same name but different content styleable entry
                 */
                it.value = hostStyleables.get(index).value
                hostStyleables.set(index, it)
            } else {
                pluginStyleables.add(it)
            }
        }
    }

    /**
     * Reassign the ID for resources need retained in the plugin apk
     * Set the packageId specified in the build.gradle file, and reassign type&entry ID
     */
    private void reassignPluginResourceId() {

        def resourceIdList = []
        pluginResources.keySet().each { String resType ->
            List<ResourceEntry> entryList = pluginResources.get(resType)
            resourceIdList.add([resType: resType, typeId: entryList.empty ? -100 : parseTypeIdFromResId(entryList.first().resourceId)])
        }


        resourceIdList.sort { t1, t2 ->
            t1.typeId - t2.typeId
        }

        int lastType = 1
        resourceIdList.each {
            if (it.typeId < 0) {
                return
            }
            def typeId = 0
            def entryId = 0
            typeId = lastType++
            pluginResources.get(it.resType).each {
                it.setNewResourceId(virtualApk.packageId, typeId, entryId++)
            }
        }

        List<ResourceEntry> attrEntries = allResources.get('attr')

        pluginStyleables.findAll { it.valueType == 'int[]' }.each { StyleableEntry styleableEntry ->
            List<String> values = styleableEntry.valueAsList
            values.eachWithIndex { hexResId, idx ->
                ResourceEntry resEntry = attrEntries.find { it.hexResourceId == hexResId }
                if (resEntry != null) {
                    values[idx] = resEntry.hexNewResourceId
                }
            }
            styleableEntry.value = values
        }
    }

    /**
     * Parse the type part of a android resource id
     */
    def parseTypeIdFromResId(int resourceId) {
        resourceId >> 16 & 0xFF
    }

    /**
     * Collect all resources the aar project can access
     * @param aarDependenceInfo aar dependence info
     */
    def gatherReservedAarResources(AarDependenceInfo aarDependenceInfo) {
        def aarResKeys = aarDependenceInfo.resourceKeys
        if (aarResKeys.empty) return

        allResources.keySet().each { resType ->
            allResources.get(resType).each { resEntry ->
                if (aarResKeys.contains("${resType}:${resEntry.resourceName}")) {
                    aarDependenceInfo.aarResources.put(resType, resEntry)
                }
            }
        }

        aarDependenceInfo.aarStyleables = allStyleables.findAll { styleableEntry ->
            aarResKeys.contains("styleable:${styleableEntry.name}")
        }
    }

    /**
     * Returns the mapping from the original ID to the redistributed ID
     */
    def getResIdMap() {
        def idMap = [:] as Map<Integer, Integer>
        allResources.values().each { resEntry ->
            idMap.put(resEntry.resourceId, resEntry.newResourceId)
        }
        return idMap
    }


    private void dump() {
        final def resSplitDir = new File(project.buildDir, 'generated')

        final def retainTypeFile = new File(resSplitDir, 'retainType.txt')
        if (!retainTypeFile.exists()) {
            retainTypeFile.createNewFile()
        }
        retainTypeFile.withPrintWriter { pw ->
            pluginResources.values().each {
                pw.println "${it.resourceType} ${it.resourceName} 0x${Integer.toHexString(it.resourceId)} 0x${Integer.toHexString(it.newResourceId)}"
            }
            pw.println "****************Styleables*****************"
            pluginStyleables.each {
                pw.println "${it.name} ${it.valueType} ${it.value}"
            }
        }


        final def allTypeFile = new File(resSplitDir, "allType.txt")
        if (!allTypeFile.exists()) {
            allTypeFile.createNewFile()
        }
        allTypeFile.withPrintWriter { pw ->
            allResources.values().each {
                pw.println "${it.resourceType} ${it.resourceName} 0x${Integer.toHexString(it.resourceId)} 0x${Integer.toHexString(it.newResourceId)}"
            }
            pw.println "****************Styleables*****************"
            allStyleables.each {
                pw.println "${it.name} ${it.valueType} ${it.value}"
            }
        }

        final def vendorTypeFile = new File(resSplitDir, 'vendorType.txt')
        if (!vendorTypeFile.exists()) {
            vendorTypeFile.createNewFile()
        }

        vendorTypeFile.withPrintWriter { pw ->
            vaContext.retainedAarLibs.each { aarLib ->
                pw.println "${aarLib.toString()}"

                aarLib.aarResources.values().each {
                    pw.println "${it.resourceType} ${it.resourceName} 0x${Integer.toHexString(it.resourceId)} 0x${Integer.toHexString(it.newResourceId)}"
                }
            }
        }
    }

}