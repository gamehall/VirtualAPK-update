package com.didi.virtualapk.hooker
//package com.didi.virtualapk.hooker
//
//import com.android.build.gradle.api.ApkVariant
//import com.android.build.gradle.tasks.ProcessApplicationManifest
//import com.didi.virtualapk.Constants
//import com.didi.virtualapk.collector.dependence.DependenceInfo
//import com.didi.virtualapk.utils.Log
//import com.didi.virtualapk.utils.Reflect
//import groovy.xml.QName
//import groovy.xml.XmlUtil
//import org.gradle.api.Project
//import org.gradle.api.artifacts.ArtifactCollection
//import org.gradle.api.artifacts.result.ResolvedArtifactResult
//import org.gradle.api.file.FileCollection
//import org.gradle.api.internal.file.AbstractFileCollection
//import org.gradle.api.tasks.TaskDependency
//
//import java.util.function.Consumer
//import java.util.function.Predicate
//
///**
// * Filter the stripped ManifestDependency in the ManifestDependency list of ProcessApplicationManifest task
// *
// * @author zhengtao
// */
//class ProcessApplicationManifestHooker extends GradleTaskHooker<ProcessApplicationManifest> {
//
//    public static final String ANDROID_NAMESPACE = 'http://schemas.android.com/apk/res/android'
//
//    public ProcessApplicationManifestHooker(Project project, ApkVariant apkVariant) {
//        super(project, apkVariant)
//    }
//
//    @Override
//    String getTaskName() {
//        return "process" + apkVariant.name + "Manifest"
//    }
//
//    @Override
//    void beforeTaskExecute(ProcessApplicationManifest task) {
//
//        def stripAarNames = vaContext.stripDependencies.
//                findAll {
//                    it.dependenceType == DependenceInfo.DependenceType.AAR
//                }.
//                collect { DependenceInfo dep ->
//                    "${dep.group}:${dep.artifact}:${dep.version}"
//                } as Set<String>
//
//        Reflect reflect = Reflect.on(task)
//        ArtifactCollection manifests = new FixedArtifactCollection(this, reflect.get('manifests'), stripAarNames)
//        reflect.set('manifests', manifests)
//    }
//
//    /**
//     * Filter specific attributes from <application /> element after ProcessApplicationManifest task executed
//     */
//    @Override
//    void afterTaskExecute(ProcessApplicationManifest task) {
//        //fixme
//        if (project.extensions.extraProperties.get(Constants.GRADLE_3_1_0)) {
//            File outputFile = Reflect.on('com.android.build.gradle.internal.scope.ExistingBuildElements')
//                    .call('from', TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS, scope.getOutput(TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS))
//                    .call('element', variantData.outputScope.mainSplit)
//                    .call('getOutputFile')
//                    .get()
//            rewrite(outputFile)
//        } else {
//            variantData.outputScope.getOutputs(TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS).each {
//                rewrite(it.outputFile)
//            }
//        }
//    }
//
//    void rewrite(File xml) {
//        if (xml?.exists()) {
//            final Node manifest = new XmlParser().parse(xml)
//
//
//            manifest.application.each { application ->
//                [ 'icon', 'label', 'allowBackup', 'supportsRtl' ].each {
//                    application.attributes().remove(new QName(ProcessApplicationManifestHooker.ANDROID_NAMESPACE, it))
//                }
//            }
//
//            xml.withPrintWriter('utf-8', { pw ->
//                XmlUtil.serialize(manifest, pw)
//            })
//        }
//    }
//
//    private fun getPackage(manifest: Path): String =
//    BufferedInputStream(Files.newInputStream(manifest)).use {
//        AndroidManifestParser.parse(it).`package`
//    }
//    private static class FixedArtifactCollection implements ArtifactCollection {
//
//        private ProcessApplicationManifestHooker hooker
//        private ArtifactCollection origin
//        def stripAarNames
//
//        FixedArtifactCollection(ProcessApplicationManifestHooker hooker, ArtifactCollection origin, stripAarNames) {
//            this.hooker = hooker
//            this.origin = origin
//            this.stripAarNames = stripAarNames
//        }
//
//        @Override
//        FileCollection getArtifactFiles() {
//            Set<File> set = getArtifacts().collect { ResolvedArtifactResult result ->
//                result.file
//            } as Set<File>
//            FileCollection fileCollection = origin.getArtifactFiles()
//
//            return new AbstractFileCollection() {
//                @Override
//                String getDisplayName() {
//                    return fileCollection.getDisplayName()
//                }
//
////                @Override
////                TaskDependency getBuildDependencies() {
////                    return fileCollection.getBuildDependencies()
////                }
//
//                @Override
//                Set<File> getFiles() {
//                    Set<File> files = new LinkedHashSet(fileCollection.getFiles())
//                    files.retainAll(set)
//                    return files
//                }
//            }
//        }
//
//        @Override
//        Set<ResolvedArtifactResult> getArtifacts() {
//            Set<ResolvedArtifactResult> set = origin.getArtifacts()
//            set.removeIf(new Predicate<ResolvedArtifactResult>() {
//                @Override
//                boolean test(ResolvedArtifactResult result) {
//                    boolean ret = stripAarNames.contains("${result.id.componentIdentifier.displayName}")
//                    if (ret) {
//                        Log.i 'ProcessApplicationManifestHooker', "Stripped manifest of artifact: ${result} -> ${result.file}"
//                    }
//                    return ret
//                }
//            })
//
//            hooker.mark()
//            return set
//        }
//
//        @Override
//        Collection<Throwable> getFailures() {
//            return origin.getFailures()
//        }
//
//        @Override
//        Iterator<ResolvedArtifactResult> iterator() {
//            return getArtifacts().iterator()
//        }
//
//        @Override
//        void forEach(Consumer<? super ResolvedArtifactResult> action) {
//            getArtifacts().forEach(action)
//        }
//
//        @Override
//        Spliterator<ResolvedArtifactResult> spliterator() {
//            return getArtifacts().spliterator()
//        }
//    }
//}