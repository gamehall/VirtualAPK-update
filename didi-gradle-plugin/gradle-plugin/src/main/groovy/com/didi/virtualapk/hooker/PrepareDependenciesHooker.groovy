package com.didi.virtualapk.hooker

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.ide.DependenciesImpl
import com.android.build.gradle.internal.variant.ApplicationVariantData
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.VAHostPlugin
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.collector.dependence.JarDependenceInfo
import com.didi.virtualapk.utils.FileUtil
import com.didi.virtualapk.utils.Log
import org.apache.http.util.TextUtils
import org.gradle.api.Project

/**
 *
 * 为了收集插件的依赖项
 * Gather list of dependencies(aar&jar) need to be stripped&retained after the PrepareDependenciesTask finished.
 * The entire stripped operation throughout the build lifecycle is based on the result of this hooker。
 *
 * @author zhengtao
 */
class PrepareDependenciesHooker {

    //group:artifact:version
    def hostDependencies = [] as Set

    def retainedAarLibs = [] as Set<AarDependenceInfo>
    def retainedJarLib = [] as Set<JarDependenceInfo>
    def stripDependencies = [] as Collection<DependenceInfo>
    Project project

    ApplicationVariantImpl apkVariant
    VAExtension virtualApk
    VAExtension.VAContext vaContext
    ApplicationVariantData variantData
    def taskName = "collectDependencies"

    public PrepareDependenciesHooker(Project project, ApplicationVariantImpl apkVariant) {
        this.project = project
        this.virtualApk = project.virtualApk
        this.apkVariant = apkVariant
        variantData = apkVariant.variantData
        vaContext = VAExtension.getVaContext()
    }

    /**
     * Collect host dependencies via hostDependenceFile or exclude configuration before PrepareDependenciesTask execute,
     * @param task Gradle Task fo PrepareDependenciesTask
     */
    void beforeTaskExecute() {
        println "[PrepareDependenciesHooker.beforeTaskExecute] "
        hostDependencies.addAll(virtualApk.hostDependencies.keySet())
        virtualApk.excludes.each { String artifact ->
            final def module = artifact.split(':')
            hostDependencies.add("${module[0]}:${module[1]}")
        }
    }

    /**
     * Classify all dependencies into retainedAarLibs & retainedJarLib & stripDependencies
     *
     */
    void afterTaskExecute() {
        println "[PrepareDependenciesHooker.afterTaskExecute] "
        DependenciesImpl dependenciesImpl = VAHostPlugin.getDependenciesImpl(project, apkVariant)
        dependenciesImpl.libraries.each {
            def name = it.name.replace("@aar", "").replace("@jar", "").replace("::", ":")
            final def module = name.split(':')
//            println "PrepareDependenciesHooker.afterTaskExecute module = " + module
            def groupId="unspecified"
            def artifactId
            def version
            if (module.length == 1) {
                //:monkey-amber105-release:
                groupId = "unspecified"
                artifactId = module[0]
                version = "unspecified"
            } else if (module.length == 2) {
                groupId = module[0]
                artifactId = module[1]
                version = "unspecified"
            } else {
                groupId = module[0]
                artifactId = module[1]
                version = module[2]
            }
            if (TextUtils.isEmpty(groupId)) {
                groupId = "unspecified"
            }
            if (TextUtils.isEmpty(artifactId)) {
                artifactId = "unspecified"
            }
            if (TextUtils.isEmpty(version)) {
                version = "unspecified"
            }

            //GString和Java的String类不同
//            def flag = "${module[0]}:${module[1]}"
            if (hostDependencies.contains("${groupId}:${artifactId}")) {
//            if (hostDependencies.contains("${module[0]}:${module[1]}")) {
                stripDependencies.add(
                        new AarDependenceInfo(
                                groupId,
                                artifactId,
                                version,
                                it
                        ))
            } else {
                retainedAarLibs.add(
                        new AarDependenceInfo(
                                groupId,
                                artifactId,
                                version,
                                it
                        ))
            }
        }
        dependenciesImpl.javaLibraries.each {
            def name = it.name.replace("@aar", "").replace("@jar", "").replace("::", ":")
            final def module = name.split(':')
            def groupId = module[0]
            def artifactId = module[1]
            def version = module[2]

            if (hostDependencies.contains("${groupId}:${artifactId}")) {
                stripDependencies.add(
                        new JarDependenceInfo(
                                groupId,
                                artifactId,
                                version,
                                it
                        )
                )
            } else {
                retainedJarLib.add(new JarDependenceInfo(
                        groupId,
                        artifactId,
                        version,
                        it
                ))
            }
        }

//        def list = VAHostPlugin.getDependList(project, apkVariant)
//        list.each {
//            final def module = it.split(':')
//            def groupId = module[0]
//            def artifactId = module[1]
//            def version = module[2]
//
//            if (hostDependencies.contains("${module[0]}:${module[1]}")) {
//                stripDependencies.add(
//                        new ArtifactDependenceInfo(
//                                groupId,
//                                artifactId,
//                                version,
//                                file
//                        ))
//            } else {
//                retainedJarLib.add(
//                        new ArtifactDependenceInfo(
//                                groupId,
//                                artifactId,
//                                version,
//                                file
//                        ))
//            }
//        }
//        scope.getIncrementalDir("aapt_friendly_merged_manifests").toString()+"/"+scope.
        File hostDir = vaContext.getBuildDir(project)
        FileUtil.saveFile(hostDir, "${taskName}-stripDependencies", stripDependencies)
        FileUtil.saveFile(hostDir, "${taskName}-retainedAarLibs", retainedAarLibs)
        FileUtil.saveFile(hostDir, "${taskName}-retainedJarLib", retainedJarLib)

//        checkDependencies()

        Log.i 'PrepareDependenciesHooker', "Analyzed all dependencis. Get more infomation in dir: ${hostDir.absoluteFile}"

        vaContext.stripDependencies = stripDependencies
        println "PrepareDependenciesHooker.afterTaskExecute setStripDependencies to " + vaContext
        vaContext.retainedAarLibs = retainedAarLibs
    }

    void checkDependencies() {
        ArrayList<DependenceInfo> allRetainedDependencies = new ArrayList<>()
        allRetainedDependencies.addAll(retainedAarLibs)
        allRetainedDependencies.addAll(retainedJarLib)

        ArrayList<String> checked = new ArrayList<>()

        allRetainedDependencies.each {
            String group = it.group
            String artifact = it.artifact
            String version = it.version

            // com.didi.virtualapk:core
            if (group == 'com.didi.virtualapk' && artifact == 'core') {
                checked.add("${group}:${artifact}:${version}")
            }

            // com.android.support:all
            if (group == 'com.android.support' || group.startsWith('com.android.support.')) {
                checked.add("${group}:${artifact}:${version}")
            }

            // com.android.databinding:all
            if (group == 'com.android.databinding' || group.startsWith('com.android.databinding.')) {
                checked.add("${group}:${artifact}:${version}")
            }
        }

        if (!checked.empty) {
            throw new Exception("The dependencies [${String.join(', ', checked)}] that will be used in the current plugin must be included in the host app first. Please add it in the host app as well.")
        }
    }
}