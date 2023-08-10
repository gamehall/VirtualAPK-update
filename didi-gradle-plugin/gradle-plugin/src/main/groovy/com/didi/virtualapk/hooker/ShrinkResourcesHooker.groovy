package com.didi.virtualapk.hooker


import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import org.gradle.api.Project

class ShrinkResourcesHooker extends GradleTaskHooker<TransformTask> {
    
    ShrinkResourcesHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }
    
    @Override
    String getTransformName() {
        return "shrinkRes"
    }
    
    @Override
    void beforeTaskExecute(TransformTask task) {
//        def shrinkResourcesTransform = task.transform as ShrinkResourcesTransform
//        Reflect.on(task).set('transform', new TransformWrapper(shrinkResourcesTransform) {
//            @Override
//            void transform(TransformInvocation invocation) throws TransformException, InterruptedException, IOException {
//                Log.i 'ShrinkResourcesHooker', "sourceDir: ${Reflect.on(origin).get('sourceDir')}"
//                super.transform(invocation)
//                mark()
//            }
//        })
    }

    @Override
    void afterTaskExecute(TransformTask task) {

    }
}