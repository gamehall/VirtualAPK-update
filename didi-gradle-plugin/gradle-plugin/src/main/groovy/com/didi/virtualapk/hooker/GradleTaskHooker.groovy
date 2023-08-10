package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.variant.BaseVariantData
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.VAExtension.VAContext
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Base class of gradle task hooker， provides some common field used by hookers
 * @param <T> Type of hooked task
 *
 * @author zhengtao
 */
public abstract class GradleTaskHooker<T extends Task> {

    private Project project

    /**
     * A Build variant when build a apk and all its public data.
     */
    private ApplicationVariantImpl apkVariant

    private VAExtension virtualApk

    public GradleTaskHooker(Project project, ApplicationVariantImpl apkVariant) {
        this.project = project
        this.apkVariant = apkVariant
        this.virtualApk = project.virtualApk
//        vaContext.checkList.addCheckPoint(taskName + " " + apkVariant.name)
    }

    public Project getProject() {
        return this.project
    }

    public ApkVariant getApkVariant() {
        return this.apkVariant
    }

    public BaseVariantData getVariantData() {
        return ((ApplicationVariantImpl) this.apkVariant).variantData
    }

    public VAExtension getVirtualApk() {
        return this.virtualApk
    }

    public VAContext getVaContext() {
        return this.virtualApk.getVaContext()
    }

    public void mark() {

    }

    public T getTask() {

    }

    /**
     * Return the transform name of the hooked task(transform task)
     */
    public String getTransformName() {
        return ""
    }

    /**
     * Return the task name(exclude transform task)
     */
    public String getTaskName() {
        return "${transformName}For${apkVariant.name.capitalize()}"
    }

    /**
     * Callback function before the hooked task executes
     * @param task Hooked task
     */
    public abstract void beforeTaskExecute(T task)
    /**
     * Callback function after the hooked task executes
     * @param task Hooked task
     */
    public abstract void afterTaskExecute(T task)
}