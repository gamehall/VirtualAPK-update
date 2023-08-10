package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.android.build.gradle.tasks.ProcessApplicationManifest
import com.android.build.gradle.tasks.ProcessMultiApkApplicationManifest
import com.didi.virtualapk.Constants
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.utils.Log
import com.didi.virtualapk.utils.Reflect
import com.tencent.tinker.build.gradle.Compatibilities
import groovy.xml.QName
import groovy.xml.XmlUtil
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.tasks.TaskDependency

import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Filter the stripped ManifestDependency in the ManifestDependency list of MergeManifests task
 *
 * @author zhengtao
 */
class MergeManifestsHooker extends GradleTaskHooker<ManifestProcessorTask> {

    public static final String ANDROID_NAMESPACE = 'http://schemas.android.com/apk/res/android'

    public MergeManifestsHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTaskName() {
        return scope.getTaskName('process', 'Manifest')
    }

    @Override
    void beforeTaskExecute(ManifestProcessorTask task) {


        def stripAarNames = vaContext.stripDependencies.
                findAll {
                    it.dependenceType == DependenceInfo.DependenceType.AAR
                }.
                collect { DependenceInfo dep ->
                    "${dep.group}:${dep.artifact}:${dep.version}"
                } as Set<String>
        if(task instanceof ProcessApplicationManifest){
            Reflect reflect = Reflect.on(task)
            ArtifactCollection manifests = new FixedArtifactCollection(this, reflect.get('manifests'), stripAarNames)
            reflect.set('manifests', manifests)
        }else {
           throw new GradleException("task is not ProcessApplicationManifest can not stripped components")
        }

    }

    /**
     * Filter specific attributes from <application /> element after MergeManifests task executed
     */
    @Override
    void afterTaskExecute(ManifestProcessorTask task) {

   /*     if (project.extensions.extraProperties.get(Constants.GRADLE_3_1_0)) {
            File outputFile = Reflect.on('com.android.build.gradle.internal.scope.ExistingBuildElements')
                    .call('from', TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS, scope.getOutput(TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS))
                    .call('element', variantData.outputScope.mainSplit)
                    .call('getOutputFile')
                    .get()
            rewrite(outputFile)
        } else {
            variantData.outputScope.getOutputs(TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS).each {
                rewrite(it.outputFile)
            }
        }*/
        if(task instanceof ProcessMultiApkApplicationManifest){
            File manifestFile = Compatibilities.getOutputManifestPath(project,task,apkVariant)
            rewrite(manifestFile)
        }else {
            throw new GradleException("task is not ProcessMultiApkApplicationManifest can not rewrite AndroidManifest")
        }

//        val manifestFile = task.mainMergedManifest.get().asFile
    }

    void rewrite(File xml) {
        if (xml?.exists()) {
            final Node manifest = new XmlParser().parse(xml)


            manifest.application.each { application ->
                [ 'icon', 'label', 'allowBackup', 'supportsRtl' ].each {
                    application.attributes().remove(new QName(MergeManifestsHooker.ANDROID_NAMESPACE, it))
                }
            }

            xml.withPrintWriter('utf-8', { pw ->
                XmlUtil.serialize(manifest, pw)
            })
        }
    }

    private static class FixedArtifactCollection implements ArtifactCollection {

        private MergeManifestsHooker hooker
        private ArtifactCollection origin
        def stripAarNames

        FixedArtifactCollection(MergeManifestsHooker hooker, ArtifactCollection origin, stripAarNames) {
            this.hooker = hooker
            this.origin = origin
            this.stripAarNames = stripAarNames
        }

        @Override
        FileCollection getArtifactFiles() {
            Set<File> set = getArtifacts().collect { ResolvedArtifactResult result ->
                result.file
            } as Set<File>
            FileCollection fileCollection = origin.getArtifactFiles()

            return new AbstractFileCollection() {
                @Override
                String getDisplayName() {
                    return fileCollection.getDisplayName()
                }

/*
                @Override
                TaskDependency getBuildDependencies() {
                    return fileCollection.getBuildDependencies()
                }
*/

                @Override
                Set<File> getFiles() {
                    Set<File> files = new LinkedHashSet(fileCollection.getFiles())
                    files.retainAll(set)
                    return files
                }
            }
        }

        @Override
        Set<ResolvedArtifactResult> getArtifacts() {
            Set<ResolvedArtifactResult> set = origin.getArtifacts()
            set.removeIf(new Predicate<ResolvedArtifactResult>() {
                @Override
                boolean test(ResolvedArtifactResult result) {
                    def filterName =  "${result.id.componentIdentifier.displayName}";
                    Log.i 'MergeManifestsHooker', "will  begin filterName:"+filterName
                    int filterSize = result.id.componentIdentifier.displayName.split(":").size()
                    if(filterSize == 4){
                        filterName = "${result.id.componentIdentifier.displayName.substring(0,result.id.componentIdentifier.displayName.lastIndexOf(":"))}"
                    }
                    Log.i 'MergeManifestsHooker', "will  check of filterName:"+filterName
                    boolean ret = stripAarNames.contains(filterName)
                    if (ret) {
                        Log.i 'MergeManifestsHooker', "Stripped manifest of artifact: ${result} -> ${result.file}"
                    }
                    return ret
                }
            })

            hooker.mark()
            return set
        }

        @Override
        Collection<Throwable> getFailures() {
            return origin.getFailures()
        }

        @Override
        Iterator<ResolvedArtifactResult> iterator() {
            return getArtifacts().iterator()
        }

        @Override
        void forEach(Consumer<? super ResolvedArtifactResult> action) {
            getArtifacts().forEach(action)
        }

        @Override
        Spliterator<ResolvedArtifactResult> spliterator() {
            return getArtifacts().spliterator()
        }
    }
}