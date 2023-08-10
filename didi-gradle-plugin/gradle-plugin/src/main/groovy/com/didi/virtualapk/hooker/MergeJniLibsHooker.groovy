package com.didi.virtualapk.hooker


import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.collector.HostJniLibsCollector
import org.gradle.api.Project

/**
 * Remove the Native libs(.so) in stripped dependencies before mergeJniLibs task
 *
 * @author zhengtao
 */
class MergeJniLibsHooker {

    HostJniLibsCollector jniLibsCollector
    AppExtension androidConfig
    ApplicationVariantImpl appVariant
    Project project

    public MergeJniLibsHooker(Project project, ApplicationVariantImpl appVariant) {
        jniLibsCollector = new HostJniLibsCollector()
        androidConfig = project.extensions.findByType(AppExtension)
        this.project = project
        this.appVariant = appVariant
    }

    /**
     * Prevent .so files from packaging into apk via the PackagingOptions exclude configuration
     */
    void beforeTaskExecute() {
        def excludeJniFiles = jniLibsCollector.collect(VAExtension.getVaContext().stripDependencies)
        println "MergeJniLibsHooker.beforeTaskExecute excludeJniFiles.size = " + excludeJniFiles.size()
        excludeJniFiles.each {
            println "MergeJniLibsHooker.beforeTaskExecute Stripped jni file " + it
//            FileUtil.deleteFile("${project.buildDir.path}/intermediates/merged_native_libs/${appVariant.name}/out/${it}".toString())
//            FileUtil.deleteFile("${project.buildDir.path}/intermediates/stripped_native_libs/${appVariant.name}/out/${it}".toString())
        }
//        excludeJniFiles.each {
//            androidConfig.packagingOptions.exclude("/${it}")
//            Log.i 'MergeJniLibsHooker', "Stripped jni file: ${it}"
//        }

//        Reflect.on(task.transform)
//                .set('packagingOptions', new ParsedPackagingOptions(androidConfig.packagingOptions))
    }

    Set<String> getExcludeJniFiles() {
        return jniLibsCollector.collect(VAExtension.getVaContext().stripDependencies)
    }

}