package com.didi.virtualapk.transform

import com.android.build.api.transform.*
import com.android.build.api.variant.VariantInfo
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.collector.HostClassAndResCollector
import com.didi.virtualapk.utils.FileUtil
import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.transform.TransformException

/**
 * Strip Host classes and java resources from project, it's an equivalent of provided compile
 * @author zhengtao
 */
class StripClassAndResTransform extends Transform {

    private Project project
    private VAExtension virtualApk
    private HostClassAndResCollector classAndResCollector

    StripClassAndResTransform(Project project) {
        this.project = project
        this.virtualApk = project.virtualApk
        classAndResCollector = new HostClassAndResCollector(project)
    }

    @Override
    String getName() {
        return 'stripClassAndRes'
    }

    @Override
    boolean applyToVariant(VariantInfo variant) {
        println "stripClassAndRes.applyToVariant#variant = ${variant.buildTypeName}"
        return variant.buildTypeName.containsIgnoreCase("debug")
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
//        return TransformManager.CONTENT_CLASS
        return TransformManager.CONTENT_JARS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * Only copy the jars or classes and java resources of retained aar into output directory
     */
    @Override
    void transform(final TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        VAExtension.VAContext vaContext = virtualApk.getVaContext()
//        vaContext.stripDependencies.each {
//            println "[StripClassAndResTransform.transform] stripDependencie = ${it.group} :${it.artifact}: ${it.version}"
//        }
        LinkedHashSet stripEntries = classAndResCollector.collect(vaContext.stripDependencies)
        FileUtil.saveFile(project.buildDir, "host_classes", stripEntries)
//        stripEntries.each {
//            println "StripClassAndResTransform.transform stripEntry = " + it
//        }
        if (!isIncremental()) {
            transformInvocation.outputProvider.deleteAll()
        }

        def strip = new ArrayList<String>()
        def pluginAll = new ArrayList<String>()
        def jars = new ArrayList<String>()
        transformInvocation.inputs.each {
            it.directoryInputs.each { directoryInput ->
//                Log.i 'StripClassAndResTransform', "input dir: ${directoryInput.file.absoluteFile}"
                def destDir = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
//                Log.i 'StripClassAndResTransform', "output dir: ${destDir.absoluteFile}"
                directoryInput.file.traverse(type: FileType.FILES) {
                    def entryName = it.path.substring(directoryInput.file.path.length() + 1)
//                    Log.i 'StripClassAndResTransform', "found file: ${it.absoluteFile}"
//                    Log.i 'StripClassAndResTransform', "entryName: ${entryName}"
                    pluginAll.add(entryName)
                    if (virtualApk.stripClass) {
                        if (!stripEntries.contains(entryName)) {
                            def dest = new File(destDir, entryName)
                            FileUtils.copyFile(it, dest)
//                        Log.i 'StripClassAndResTransform', "Copied to file: ${dest.absoluteFile}"
                        } else {
                            strip.add(entryName)
//                            Log.i 'StripClassAndResTransform', "Stripped file: ${it.absoluteFile}"
                        }
                    } else {
                        def dest = new File(destDir, entryName)
                        FileUtils.copyFile(it, dest)
                    }
                }
            }

            it.jarInputs.each { jarInput ->
                if (virtualApk.stripClass) {
//                    Log.i 'StripClassAndResTransform', "input jar: ${jarInput.file.absoluteFile}"
                    Set<String> jarEntries = HostClassAndResCollector.unzipJar(jarInput.file)
                    pluginAll.addAll(jarEntries)
                    if (!stripEntries.containsAll(jarEntries)) {
                        jars.add("inputJar=" + jarInput.name + " ${jarEntries.size()} " + jarInput.file)
                        jars.addAll(jarEntries)
//                    println "StripClassAndResTransform.transform stripEntries not contain " + jarEntries[0]
                        def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name,
                                jarInput.contentTypes, jarInput.scopes, Format.JAR)
//                    Log.i 'StripClassAndResTransform', "output jar: ${dest.absoluteFile}"
                        FileUtils.copyFile(jarInput.file, dest)
//                    Log.i 'StripClassAndResTransform', "Copied to jar: ${dest.absoluteFile}"
                    } else {
                        strip.addAll(jarEntries)
//                        Log.i 'StripClassAndResTransform', "Stripped jar: ${jarInput.file.absoluteFile}"
                    }
                } else {
                    def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name,
                            jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
        }
        FileUtil.saveFile(project.buildDir, "striped_classes", strip)
        FileUtil.saveFile(project.buildDir, "plugin_all_classes", pluginAll)
        FileUtil.saveFile(project.buildDir, "plugin_jars_classes", jars)
        vaContext.checkList.mark(name)
    }

}