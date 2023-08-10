package com.didi.virtualapk.collector.dependence


import com.android.builder.model.AndroidLibrary
import com.android.utils.FileUtils
import com.didi.virtualapk.collector.res.ResourceEntry
import com.didi.virtualapk.collector.res.StyleableEntry
import com.didi.virtualapk.utils.Log
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists

/**
 * Represents a AAR dependence from Maven repository or Android library module
 *
 * @author zhengtao
 */
class AarDependenceInfo extends DependenceInfo {

    /**
     * Android library dependence in android build system, delegate of AarDependenceInfo
     */
    AndroidLibrary library
    File jarFile

    String intermediatesFile
    String AndroidManifest
    File assetsFolder
    File jniFolder
    Collection<File> localJars

    /**
     * All resources(e.g. drawable, layout...) this library can access
     * include resources of self-project and dependence(direct&transitive) project
     */
    ListMultimap<String, ResourceEntry> aarResources = ArrayListMultimap.create()
    /**
     * All styleables this library can access, like "aarResources"
     */
    List<StyleableEntry> aarStyleables = Lists.newArrayList()

    AarDependenceInfo(String group, String artifact, String version, AndroidLibrary library) {
        super(group, artifact, version)
        this.library = library
        //origin
        ///Users/sansecy/Downloads/shanpao/shanpao_tv/component/contract/build/intermediates/full_jar/debug/createFullJarDebug/full.jar/jars/classes.jar
        //target
        //build/intermediates/runtime_library_classes/debug/classes.jar
//        println "AarDependenceInfo.AarDependenceInfo library.jarFile = ${library.jarFile}"
        println "[AarDependenceInfo.AarDependenceInfo] ${library} -- manifest = ${library.manifest.path}"
        this.AndroidManifest = library.manifest.path.replace("AndroidManifest.xml" + File.separator + "AndroidManifest.xml", "AndroidManifest.xml")
//        this.AndroidManifest = library.manifest.path.replace("full_jar", "aapt_friendly_merged_manifests")
//                .replace("createFullJarDebug/full.jar", "aapt")
        //        merged_native_libs/jxSpOnlineBaseNoaiDebug/out/lib/armeabi-v7a/libnewtvsdk.so
        //intermediates/runtime_library_classes_jar/debug/classes.jar
        if (library.project != null && library.project.startsWith(":")) {
            def bundlePath = library.getBundle().toString()
            intermediatesFile = bundlePath.substring(0, bundlePath.indexOf("intermediates")) + "intermediates"
            def variant = library.projectVariant
            if (variant == null) {
                variant = new File(library.bundle.path).parentFile.name
            }
            jarFile = new File(intermediatesFile + "/runtime_library_classes_jar/" + variant + "/classes.jar")
            if (!jarFile.exists()) {
                jarFile = new File(intermediatesFile + "/compile_library_classes_jar/" + variant + "/classes.jar")
            }
            assetsFolder = new File(intermediatesFile + "/library_assets/" + variant + "/out")
            //intermediates/compressed_assets/jxSpOnlineBaseNoaiDebug/out/assets
            jniFolder = new File(intermediatesFile + "/merged_native_libs/" + variant + "/out/lib")
        } else {
            jarFile = library.jarFile
            assetsFolder = library.assetsFolder
            jniFolder = library.jniFolder
            localJars = library.localJars
        }
    }

    @Override
    File getJarFile() {
//        Log.i 'AarDependenceInfo', "[${library.resolvedCoordinates}] Found jar file: ${jarFile}"
        return jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.AAR
    }

    File getAssetsFolder() {
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s assets folder: ${assetsFolder}"
        return assetsFolder
    }

    File getJniFolder() {
//        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s jni folder: ${jniFolder}"
        return jniFolder
    }

    Collection<File> getLocalJars() {
//        Log.i 'AarDependenceInfo', "[${library.resolvedCoordinates}] Found local jars: ${localJars}"
        return localJars
    }

    /**
     * Return collection of "resourceType:resourceName", parse from R symbol file
     * @return set of a combination of resource type and name
     */
    public Set<String> getResourceKeys() {

        def resKeys = [] as Set<String>
//        println "AarDependenceInfo.getResourceKeys ${library.name}.symbolFile = " + library.symbolFile
//        def rSymbol = getFile(library.symbolFile, TaskManager.DIR_BUNDLES, library.projectVariant, SdkConstants.FN_RESOURCE_TEXT)
        def rSymbol = library.symbolFile
        if (rSymbol.exists()) {
//            Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s symbol file: ${rSymbol}"
            rSymbol.eachLine { line ->
                if (!line.empty) {
                    def tokenizer = new StringTokenizer(line)
                    def valueType = tokenizer.nextToken()
                    def resType = tokenizer.nextToken()
                    // resource type (attr/string/color etc.)
                    def resName = tokenizer.nextToken()       // resource name

                    resKeys.add("${resType}:${resName}")
                }
            }
        }

        return resKeys
    }

    /**
     * Return the package name of this library, parse from manifest file
     * manifest file are obtained by delegating to "library"
     * @return package name of this library
     */
    public String getPackage() {
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s library.project: ${library.project.toString()}"
        //C:\Users\sansecy\shanpao_tv2\business\base\build\intermediates\full_jar\debug\createFullJarDebug\full.jar\jars\classes.jar
//        library.project + "/build/intermediates/aapt_friendly_merged_manifests" + "/debug/aapt/AndroidManifest.xml"
        File manifest = new File(AndroidManifest)
        if (!manifest.exists()) {
            throw new FileNotFoundException(manifest.path + " doesn't exist ")
        }
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s manifest file: ${manifest}"
        def xmlManifest = new XmlParser().parse(manifest)
        return xmlManifest.@package
    }

    String getIntermediatesDir() {
        if (intermediatesFile == null) {
            String path = library.folder.path
            try {
                intermediatesFile = new File(path.substring(0, path.indexOf("${File.separator}intermediates${File.separator}")), 'intermediates')

            } catch (Exception e) {
                Log.e('AarDependenceInfo', "Can not find [${library.resolvedCoordinates}]'s intermediates dir from the path: ${path}")
                intermediatesFile = library.folder
            }
        }
        return intermediatesFile
    }

    File getFile(File defaultFile, String... paths) {
        if (library.projectVariant == null) {
            return defaultFile
        }

        if (defaultFile.exists()) {
            return defaultFile
        }

        // module library
        return FileUtils.join(intermediatesDir, paths)
    }

    @Override
    String toString() {
        return "${super.toString()}"
//        return "${super.toString()} -> ${library}"
    }
}