package com.didi.virtualapk.aapt

import com.didi.virtualapk.collector.res.ResourceEntry
import com.didi.virtualapk.collector.res.StyleableEntry
import com.google.common.collect.ListMultimap
import com.google.common.io.Files
import groovy.io.FileType
import org.gradle.api.Project

/**
 * Class to expand aapt function
 */
public class Aapt {

    public static final int ID_DELETED = -1
    public static final int ID_NO_ATTR = -2

    public static final String RESOURCES_ARSC = 'resources.arsc'

    private static final String ENTRY_SEPARATOR = '/'

    private final File assetDir
    private final File symbolFile
    private final def toolsRevision

    Aapt(final File assetDir, File symbolFile, toolsRevision) {
        this.assetDir = assetDir
        this.symbolFile = symbolFile
        this.toolsRevision = toolsRevision
    }

    /**
     * Filter package assets by specific types
     *
     * @param retainedTypes
     *            the resource types to retain
     * @param pp
     *            new package id
     * @param idMaps
     */
    void filterPackage(final List<?> retainedTypes, final List<?> retainedStyleables, final int pp,
                       final Map<?, ?> idMaps, final Map<?, ?> libRefTable, final Set<String> outUpdatedResources) {
        final File arscFile = new File(assetDir, RESOURCES_ARSC)
        final def arscEditor = new ArscEditor(arscFile, toolsRevision)

        // Filter R.txt
        if (symbolFile != null) {
            filterRTxt(symbolFile, retainedTypes, retainedStyleables)
        }
        // Filter resource.arsc
        arscEditor.slice(pp, idMaps, libRefTable, retainedTypes)
        outUpdatedResources.add(RESOURCES_ARSC)
        resetAllXmlPackageId(this.assetDir, pp, idMaps, outUpdatedResources)
    }

    /**
     * Reset resource package id for assets
     * @param pp new package id
     * @param ppStr the hex string of package id
     * @param idMaps
     */
    void resetPackage(int pp, String ppStr, Map idMaps) {
        File arscFile = new File(assetDir, RESOURCES_ARSC)
        def arscEditor = new ArscEditor(arscFile, null)

        // Modify R.java
        //resetRjava(mJavaFile, ppStr)
        // Modify resources.arsc
        arscEditor.reset(pp, idMaps)

        resetAllXmlPackageId(arscEditor, pp, idMaps, null)
    }

    boolean deletePackage(Set outFilteredResources) {
        File arscFile = new File(assetDir, RESOURCES_ARSC)
        if (arscFile.exists()) {
            outFilteredResources.add(RESOURCES_ARSC)
            return arscFile.delete()
        }
        return false
    }

    void manifest(Project project, Map options) {
        // Create source file
        File tempManifest = new File(mAssetDir, FILE_MANIFEST)
        tempManifest.write("""<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${options.packageName}"
    android:versionName="${options.versionName}"
    android:versionCode="${options.versionCode}"/>""")
        // Compile to hex with aapt
        File tempZip = new File(assetDir, 'manifest.zip')
        project.exec {
            executable options.aaptExe
            args 'package', '-f', '-M', tempManifest.path,
                    '-I', options.baseAsset, '-F', tempZip.path
        }
        // Unzip the compiled apk
        tempManifest.delete()
        project.copy {
            from project.zipTree(tempZip)
            into assetDir
        }
        tempZip.delete()
    }

    /**
     * Filter resources with specific types
     *
     * @param retainedTypes
     */
    void filterResources(final List<?> retainedTypes, final Set<String> outFilteredResources) {
        def resDir = new File(assetDir, 'res')
        resDir.listFiles().each { typeDir ->
            def type = retainedTypes.find {
                if (typeDir.name.startsWith("animator")) {
                    it.name == "animator"
                } else {
                    typeDir.name.startsWith(it.name)
                }
            }
            if (type == null) {
                typeDir.listFiles().each {
                    outFilteredResources.add("res/$typeDir.name/$it.name")
                }

                typeDir.deleteDir()
                return
            }

            def entryFiles = typeDir.listFiles()
            def retainedEntryCount = entryFiles.size()

            entryFiles.each { entryFile ->
                def entry = type.entries.find { entryFile.name.startsWith("${it.name}.") }
                if (entry == null) {
                    outFilteredResources.add("res/$typeDir.name/$entryFile.name")
                    entryFile.delete()
                    retainedEntryCount--
                }
            }

            if (retainedEntryCount == 0) {
                typeDir.deleteDir()
            }
        }
    }

    boolean deleteResourcesDir(Set outFilters) {
        def resDir = new File(assetDir, 'res')
        if (resDir.exists()) {
            resDir.listFiles().each { dir ->
                dir.listFiles().each { file ->
                    outFilters.add("res/$dir.name/$file.name" as String)
                }
            }
            return resDir.deleteDir()
        }
        return false
    }

    /**
     * Reset package id for *.xml
     */
    private static void resetAllXmlPackageId(final File dir, final int pp, final Map<?, ?> idMaps, final Set<String> outUpdatedResources) {
        int len = dir.canonicalPath.length() + 1 // bypass '/'
        def isWindows = (File.separator != ENTRY_SEPARATOR)

        dir.eachFileRecurse(FileType.FILES) { file ->
            if ('xml'.equalsIgnoreCase(Files.getFileExtension(file.name))) {
                new AXmlEditor(file).setPackageId(pp, idMaps)

                if (outUpdatedResources != null) {
                    def path = file.canonicalPath.substring(len)
                    if (isWindows) { // compat for windows
                        path = path.replaceAll('\\\\', ENTRY_SEPARATOR)
                    }

                    outUpdatedResources.add(path)
                }
            }
        }
    }

    public static void generateRJava(File dest, String pkg, ListMultimap<String, ResourceEntry> resources,
                                     List<StyleableEntry> styleables) {
        println "generateRJava. dest = $dest, pkg = $pkg"
        if (!dest.parentFile.exists()) {
            dest.parentFile.mkdirs()
        }

        if (!dest.exists()) {
            dest.createNewFile()
        }

        dest.withPrintWriter { pw ->
            pw.println "/* AUTO-GENERATED FILE.  DO NOT MODIFY."
            pw.println " * "
            pw.println " * This class was automatically generated by the"
            pw.println " * aapt tool from the resource data it found.  It"
            pw.println " * should not be modified by hand."
            pw.println " */"
            pw.println "package ${pkg};"
            pw.println "public final class R {"

            resources.keySet().each { type ->
                pw.println "    public static final class ${type} {"
                resources.get(type).each { entry ->
                    pw.println "        public static final int ${entry.resourceName} = ${entry.hexNewResourceId};"
                }
                pw.println "    }"
            }

            pw.println "    public static final class styleable {"
            styleables.each { styleable ->
                    pw.println "        public static final ${styleable.valueType} ${styleable.name} = ${styleable.value};"
            }
            pw.println "    }"
            pw.println "}"
        }
    }


    /**
     * Filter specify types for R.java
     * @param rJavaFile
     * @param retainedTypes types that to keep
     * @param retainedStyleables styleables that to keep
     * @return
     */
    public static def filterRjava(File rJavaFile, List retainedTypes, List retainedStyleables) {
        def final clazzStart = '    public static final class '
        def final clazzEnd = '    }'
        def final varStart = '        public static final '
        def final varEnd = ';'
        def pkgRPath = rJavaFile.parentFile
        def tempRFile = new File(pkgRPath, "${rJavaFile.name}~")
        def pw = tempRFile.newPrintWriter()
        def types = []
        def currType
        def currValue
        def currIdMap
        def skip = false
        def entriesMaps = [:]
        retainedTypes.each {
            entriesMaps.put(it.name, it.entries)
        }
        rJavaFile.eachLine { str, no ->
            if (skip) {
                // ignored
                if (str == clazzEnd) skip = false
            } else if (str.startsWith(clazzStart)) {
                def name = str.substring(clazzStart.length())
                def idx = name.indexOf(' ')
                name = name.substring(0, idx)
                def entries = entriesMaps.get(name)
                if (entries == null) {
                    skip = true
                    return
                }
                currType = [declare:str, name:name, values:[]]
                currIdMap = [:]
                entries.each {
                    currIdMap.put(it.name, it._vs)
                }
                types.add(currType)
            } else if (str.startsWith(varStart)) {
                str = str.substring(varStart.length())
                def vtIdx = str.indexOf(' ')
                def vtype = str.substring(0, vtIdx)
                str = str.substring(vtIdx + 1)
                def eqIdx = str.indexOf('=')
                String var = str.substring(0, eqIdx)
                def id = currIdMap.get(var)
                if (id == null) return
                str = "$varStart$vtype $var=$id${str.substring(eqIdx + 11)}" // 0x$packageId}
                if (!str.endsWith(varEnd)) {
                    currValue = str
                    return
                }
                currType.values.add(str)
            } else if (currValue != null) {
                if (!str.endsWith(varEnd)) {
                    currValue += '\n' + str
                    return
                }
                currType.values.add(str)
            } else if (currType == null) {
                // Copy text before any types
                pw.println(str)
            }
        }
        types.each {
            if (it.values.size() == 0) return
            pw.println(it.declare)
            it.values.each {
                pw.println(it)
            }
            pw.println(clazzEnd)
        }

        if (retainedStyleables.size() > 0) {
            pw.println '    public static final class styleable {'
            retainedStyleables.each {
                pw.println "        public static final ${it.vtype} ${it.key} = ${it.idStr};"
            }
            pw.println clazzEnd
        }

        pw.println('}')

        pw.flush()
        pw.close()

        rJavaFile.delete()
        tempRFile.renameTo(rJavaFile)
    }


    /**
     * Filter specify types for R.txt
     *
     * @param rTxt
     *            The R.txt
     * @param retainedTypes
     */
    private static def filterRTxt(final File rTxt, final List<?> retainedTypes, final List<?> retainedStyleables) {
        rTxt.write('')
        rTxt.withPrintWriter { pw ->
            retainedTypes.each { t ->
                t.entries.each { e ->
                    pw.println("${t.type} ${t.name} ${e.name} ${e._vs}")
                }
            }
            retainedStyleables.each {
                pw.println("${it.vtype} ${it.type} ${it.key} ${it.idStr}")
            }
        }
    }


    /**
     * Reset package id for R.java
     * @param rJavaFile
     * @param packageId
     * @return
     */
    private static def resetRjava(File rJavaFile, String packageId) {
        def pkgRPath = rJavaFile.parentFile
        def tempRFile = new File(pkgRPath, 'tempR.java')
        def tempRWriter = tempRFile.newPrintWriter()
        rJavaFile.eachLine { str, no ->
            def str2 = str.replaceAll('0x7f([0-9a-f]{6})', '0x' + packageId + '$1');
            tempRWriter.write(str2 + '\n')
        }
        tempRWriter.flush()
        tempRWriter.close()

        rJavaFile.delete()
        tempRFile.renameTo(rJavaFile)
    }
}
