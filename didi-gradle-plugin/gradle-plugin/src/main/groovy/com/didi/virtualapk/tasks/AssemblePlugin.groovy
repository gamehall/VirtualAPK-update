package com.didi.virtualapk.tasks

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.utils.Log
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task for assemble plugin apk
 * @author zhengtao
 */
class AssemblePlugin {

    @OutputDirectory
    File pluginApkDir

    @Input
    String appPackageName

    @Input
    String apkTimestamp

    @InputFile
    File originApkFile
    @Input
    String variantName
    @Input
    String buildDir

    Project project

    ApplicationVariantImpl variant
    VAExtension virtualApk

    AssemblePlugin(Project project, ApkVariant variant) {
        println "AssemblePlugin . NEW  "
        this.project = project
        this.variant = variant
        this.virtualApk = project.virtualApk
    }

    void configure(Task task) {
        println "AssemblePlugin .configure "
        def assemblePluginTask = this
        assemblePluginTask.appPackageName = variant.applicationId
        assemblePluginTask.apkTimestamp = new Date().format("yyyyMMddHHmmss")
        assemblePluginTask.originApkFile = variant.outputs[0].outputFile
        assemblePluginTask.pluginApkDir = new File(project.buildDir, "/outputs/plugin/${variant.name}")
        assemblePluginTask.variantName = variant.name
        assemblePluginTask.buildDir = virtualApk.vaContext.getBuildDir(project).canonicalPath
        println "BasePlugin.apply variant.assemble.name=" + variant.assemble.name
        task.setGroup("build")
        task.setDescription("Build ${variant.name.capitalize()} plugin apk")
        task.dependsOn(variant.assemble.name)
    }
//com.cmgame.gamehall.mainpage_202305301936.apk
    void execute() {
        println "AssemblePlugin .outputPluginApk " + "${appPackageName}_${apkTimestamp}.apk"
        getProject().copy({
            from originApkFile
            into pluginApkDir
            rename { "${appPackageName}_${apkTimestamp}.apk" }
        })
    }
}
