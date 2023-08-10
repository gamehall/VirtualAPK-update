package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import org.gradle.api.Project

/**
 * Minify R class file under the applicationId namespace before dx task
 *
 * @author zhengtao
 */
class DxTaskHooker extends GradleTaskHooker<TransformTask> {


    public DxTaskHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTransformName() {
        return "dex"
    }

    /**
     * Replace the R class files record all resources with the stripped R only record plugin resources.
     * Input file may be a directory or jar file
     *
     * @param task Gradle transform task for dex
     */
    @Override
    void beforeTaskExecute(TransformTask task) {

    }

    @Override
    void afterTaskExecute(TransformTask task) { }
}