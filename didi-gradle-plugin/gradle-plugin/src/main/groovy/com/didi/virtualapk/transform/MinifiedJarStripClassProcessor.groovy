package com.didi.virtualapk.transform

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.collector.HostClassAndResCollector
import com.didi.virtualapk.utils.FileUtil
import com.didi.virtualapk.utils.ZipUtil
import org.gradle.api.Project

class MinifiedJarStripClassProcessor {
    private HostClassAndResCollector classAndResCollector
    private Project project
    private ApplicationVariantImpl appVariant
    private VAExtension virtualApk

    MinifiedJarStripClassProcessor(Project project, ApplicationVariantImpl appVariant) {
        this.project = project
        classAndResCollector = new HostClassAndResCollector(project)
        this.appVariant = appVariant
        this.virtualApk = project.virtualApk
    }

    void doAction() {
        println "MinifiedJarStripClassProcessor.doAction start"
        VAExtension.VAContext vaContext = virtualApk.getVaContext()
        if (vaContext.VAExtension.stripClass) {
            LinkedHashSet stripEntries = classAndResCollector.collect(vaContext.stripDependencies)
            def hostClasses = classAndResCollector.unzipJar(getHostShrunkJar())
//            hostClasses.each {
//                println "MinifiedJarStripClassProcessor.doAction ${it}"
//            }
            FileUtil.saveFile(project.buildDir, "host_classes", hostClasses)
            def minified = new File(project.buildDir.path + "/intermediates/shrunk_jar/" + appVariant.name + "/minified.jar")
            println "MinifiedJarStripClassProcessor.doAction delete class in ${minified.path} "
            def preSize = minified.length()
            println "MinifiedJarStripClassProcessor.doAction befor delete class , ${minified.name} file size = ${preSize}"
            ZipUtil.with(minified).deleteAll(hostClasses)
            def pluginJar = classAndResCollector.unzipJar(minified)
            FileUtil.saveFile(project.buildDir, "plugin_classes", pluginJar)
            println "MinifiedJarStripClassProcessor.doAction after delete class , preSize = ${preSize}, file size = ${minified.length()}"
        }
        println "MinifiedJarStripClassProcessor.doAction end"
//        throw new RuntimeException("stop")
    }

    private File getHostShrunkJar() {
        VAExtension.VAContext vaContext = virtualApk.getVaContext()
        return new File(project.rootProject.buildDir.path + "/host/minified.jar")
    }

}