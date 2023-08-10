package com.didi.virtualapk

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.ide.DependenciesImpl
import com.android.build.gradle.internal.ide.dependencies.ArtifactCollectionsInputs
import com.android.build.gradle.internal.ide.dependencies.ArtifactDependencyGraph
import com.android.build.gradle.internal.ide.dependencies.BuildMappingUtils
import com.android.build.gradle.internal.ide.dependencies.MavenCoordinatesCacheBuildService
import com.android.build.gradle.internal.services.StringCachingBuildService
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.builder.errors.EvalIssueException
import com.android.builder.errors.IssueReporter
import com.android.utils.StringHelper
import com.didi.virtualapk.utils.FileUtil
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * VirtualAPK gradle plugin for host project,
 * The primary role of this class is to save the
 * information needed to build the plugin apk.
 *
 * @author zhengtao
 */
class VAHostPlugin implements Plugin<Project> {

    public static final String TAG = 'VAHostPlugin'
    Project project
    File vaHostDir
    BaseAppModuleExtension android

    @Override
    public void apply(Project project) {
        this.project = project
        android = project.extensions.getByType(BaseAppModuleExtension.class)
        //The target project must be a android application module
        if (!project.plugins.hasPlugin('com.android.application')) {
            Log.e(TAG, "application required!")
            return;
        }
//        AppPlugin appPlugin = project.plugins.getPlugin(AppPlugin.class)
        vaHostDir = new File(project.getBuildDir(), "VAHost")

        project.afterEvaluate {
            println "VAHostPlugin.apply project.afterEvaluate "
            android.applicationVariants.all { ApplicationVariantImpl variant ->
                def assembleTaskName = StringHelper.appendCapitalized("assemble", variant.name)
                def installTaskName = StringHelper.appendCapitalized("install", variant.name)
                def variantName = variant.name
                project.tasks.findByName(assembleTaskName).doLast {
                    println "VAHostPlugin.apply variant = " + variant
                    println "VAHostPlugin.apply variantTaskName = " + assembleTaskName
                    backupFiles(variant)
                }
                project.tasks.findByName(installTaskName).doLast {
                    println "VAHostPlugin.apply variant = " + variant
                    println "VAHostPlugin.apply variantTaskName = " + assembleTaskName
                    backupFiles(variant)
                }
                def variantTaskName = StringHelper.appendCapitalized("generate", variant.name)
                project.task("${variantTaskName}Dependencies".toString()).doLast {
                    println "VAHostPlugin.apply variantName = " + variantName
                    android.applicationVariants.each {
                        if (it.name == variantName) {
                            println "VAHostPlugin.apply it.name = " + it.name
                            backupFiles(variant)
                        }
                    }
                }
            }
        }
    }

    private void backupFiles(ApplicationVariantImpl variant) {
        generateVersion(variant)
        saveNativeLibs(variant)
        saveHostAllVersion()
        backupHostR(variant)
        backupHostClass(variant)
        backupHostDex(variant)
        backupProguardMapping(variant)
        copyHostsFileToBuildDir()
    }

    private void copyHostsFileToBuildDir() {
        def targetDir = new File(project.rootProject.buildDir, "host")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
//        new File(project.rootProject.rootDir.path + "/host/minifyReleaseWithR8").mkdirs()
        project.copy {
            from vaHostDir
            into targetDir
        }
//        vaHostDir.listFiles().each {
//            println "VAHostPlugin.copyHostsFileToBuildDir " + it.path
//            def dst = new File(targetDir, it.name)
//            Files.copy(it, dst)
//        }
    }

    private void saveNativeLibs(ApplicationVariantImpl variant) {
        FileUtil.saveFile(vaHostDir, "nativeLibs", {
            def list = []
            def parentPath = "${project.buildDir.path}${File.separator}intermediates${File.separator}merged_native_libs${File.separator}" + variant.name + "${File.separator}out"
            def dir = new File(parentPath)
            if (dir.exists()) {
                dir.eachFileRecurse(FileType.FILES) { file ->
                    list << file.path.replace(parentPath, "").replace("\\", "/")
                }
            } else {
                println "VAHostPlugin.saveNativeLibs ${dir.path} does't exist"
            }
            list
        })
    }

    /**
     * Save proguard mapping
     */
    def backupProguardMapping(ApplicationVariant applicationVariant) {
        if (applicationVariant.buildType.minifyEnabled) {
            def capitalizedVariantName = applicationVariant.name.capitalize()

            // For WeChat internal build tools.
            def customProguardTransformTask = project.tasks.findByName("transformClassesWithCustomProguardFor${capitalizedVariantName}")
            if (customProguardTransformTask != null && customProguardTransformTask.enabled) {
                println "VAHostPlugin.backupProguardMapping customProguardTransformTask = " + customProguardTransformTask + " , " + customProguardTransformTask.class
                return customProguardTransformTask
            }

//            def proguardTransformTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${capitalizedVariantName}")
//            if (proguardTransformTask != null && proguardTransformTask.enabled) {
//                println "VAHostPlugin.backupProguardMapping proguardTransformTask = " + proguardTransformTask + " , " + proguardTransformTask.class
//                project.copy {
//                    from proguardTransformTask.mappingFile
//                    into vaHostDir
//                }
//            }

            def r8TransformTask = project.tasks.findByName("transformClassesAndResourcesWithR8For${capitalizedVariantName}")
            if (r8TransformTask != null && r8TransformTask.enabled) {
                println "VAHostPlugin.backupProguardMapping r8TransformTask = " + r8TransformTask + " , " + r8TransformTask.class
                return r8TransformTask
            }

            def r8Task = project.tasks.findByName("minify${capitalizedVariantName}WithR8")
            if (r8Task != null && r8Task.enabled) {
                println "VAHostPlugin.backupProguardMapping r8Task = " + r8Task + " , " + r8Task.class
                project.copy {
                    from r8Task.mappingFile.get().asFile
                    into vaHostDir
                }
            }

            def proguardTask = project.tasks.findByName("minify${capitalizedVariantName}WithProguard")
            if (proguardTask != null && proguardTask.enabled) {
                println "VAHostPlugin.backupProguardMapping proguardTask = " + proguardTask + " , " + proguardTask.class
                project.copy {
                    from proguardTask.mappingFile
                    into vaHostDir
                }
            }
        }
//        if (applicationVariant.buildType.minifyEnabled) {
//            R8Task proguardTask = project.tasks["minify${applicationVariant.name.capitalize()}WithR8"]
////            StringHelper.appendCapitalized("minify", applicationVariant.name.capitalize()) + "WithR8"
////            StringHelper.appendCapitalized("minify", applicationVariant.name.capitalize()) + "WithR8"
//
////            ProGuardTransform proguardTransform = proguardTask.transform
//            File mappingFile = proguardTask.mappingFile.get().asFile
//            project.copy {
//                from mappingFile
//                into vaHostDir
//            }
//        }
    }

    def generateVersion(ApplicationVariantImpl variant) {
        FileUtil.saveFile(vaHostDir, "versions", {
            getDependList(project, variant)
        })
    }

    static List<String> getDependList(Project project, ApplicationVariantImpl variant) {
        println "VAHostPlugin.apply variant.name = " + variant.name
        DependenciesImpl dependenciesImpl
        def dependenciesList = new ArrayList<String>()
        dependenciesImpl = getDependenciesImpl(project, variant)
        dependenciesImpl.getLibraries().each {
            dependenciesList.add(it.name.replace("@aar", "").replace("@jar", "").replace("::", ":"))
//            println "VAHostPlugin.getDependList AndroidLibrary = " + it.name + " -> " + it.getBundle()
        }
        dependenciesImpl.getJavaLibraries().each {
            dependenciesList.add(it.name.replace("@aar", "").replace("@jar", "").replace("::", ":"))
//            println "VAHostPlugin.getDependList JavaLibrary = " + it.name + " -> " + it.jarFile
        }
//        dependenciesImpl.getJavaModules().each {
//            println "VAHostPlugin.getDependList getJavaModules = " + it
//        }
//        dependenciesImpl.getProjects().each {
//            println "VAHostPlugin.getDependList getProjects = " + it
//        }


//        graphBuilder.createDependencies(
//                modelBuilder,
//                inputs,
//                true,
//                new SyncIssueReporterImpl(
//                        SyncOptions.EvaluationMode.STANDARD,
//                        SyncOptions.ErrorFormatMode.MACHINE_PARSABLE,
//                        project.logger)
//        )
//        def model = modelBuilder.createModel()

//        model.runtimeDependencies.each {
////                dependenciesList.add(it.artifactAddress.toString())
////                println "VAHostPlugin.generateVersion ${it.artifactAddress}"
//            dependenciesList.add(it.artifactAddress.toString().replace("@aar", "").replace("@jar", ""))
//        }
//        model.compileDependencies.each {
////                dependenciesList.add(it.artifactAddress.toString())
//        }
        Collections.sort(dependenciesList)
        return dependenciesList
    }

    static DependenciesImpl getDependenciesImpl(Project project, ApplicationVariantImpl variant) {
        DependenciesImpl dependenciesImpl
        def modelBuilder = new Level1DependencyModelBuilder2(variant.component.getServices().getBuildServiceRegistry())
//        def buildMapping = project.gradle.computeBuildMapping()
//        def inputs = new ArtifactCollectionsInputs(
//                variant.variantData.variantDependencies,
//                variant.variantData.globalScope.project.path,
//                variant.name,
//                ArtifactCollectionsInputs.RuntimeType.FULL,
//                BuildMappingUtils.computeBuildMapping(project.gradle)
//        )
        Provider<StringCachingBuildService> stringCachingService =
                new StringCachingBuildService.RegistrationAction(project).execute();
        Provider<MavenCoordinatesCacheBuildService> mavenCoordinatesCacheBuildService =
                new MavenCoordinatesCacheBuildService.RegistrationAction(project, stringCachingService).execute();
        def buildMapping = BuildMappingUtils.computeBuildMapping(project.gradle)
        def inputs = new ArtifactCollectionsInputs(
                variant.variantData.variantDependencies,
                variant.variantData.globalScope.project.path,
                variant.name,
                ArtifactCollectionsInputs.RuntimeType.FULL,
                mavenCoordinatesCacheBuildService,
                buildMapping
        )
        new ArtifactDependencyGraph().createDependencies(modelBuilder, inputs, true, new IssueReporter() {
            @Override
            protected void reportIssue( IssueReporter.Type type,  IssueReporter.Severity severity,  EvalIssueException e) {

            }

            @Override
            boolean hasIssue( IssueReporter.Type type) {
                return false
            }
        })

        dependenciesImpl = modelBuilder.createModel()
        dependenciesImpl
    }


    def saveHostAllVersion() {
        FileUtil.saveFile(vaHostDir, "allVersions", {
            List<String> deps = new ArrayList<String>()
            project.configurations.each {
                String configName = it.name

                if (!it.canBeResolved) {
                    deps.add("${configName} -> NOT READY")
                    return
                }

                try {
                    it.resolvedConfiguration.resolvedArtifacts.each {
                        deps.add("${configName} -> id: ${it.moduleVersion.id}, type: ${it.type}, ext: ${it.extension}")
                    }

                } catch (Exception e) {
                    deps.add("${configName} -> ${e}")
                }
            }
            Collections.sort(deps)
            return deps
        })
    }
    /**
     * Generate ${project.buildDir}/VAHost/versions.txt
     */
    def generateDependencies(ApplicationVariantImpl applicationVariant) {

        FileUtil.saveFile(vaHostDir, "versions", {
            List<String> deps = new ArrayList<String>()
            Log.i TAG, "Used compileClasspath: ${applicationVariant.name}"
//                def buildMapping = BuildMappingUtils.computeBuildMapping(project.getGradle())
//                def dependencies = new DependencyCollector().createDependencies(
//                        applicationVariant.variantData.globalScope, false, buildMapping, new Consumer<SyncIssue>() {
//                    @Override
//                    void accept(SyncIssue syncIssue) {
//
//                    }
//                })
//                applicationVariant.variantData.variantDependencies.compileClasspath.each {
//                    println "VAHostPlugin.generateDependencies file = " + it
//                }
            println "VAHostPlugin.generateDependencies "
//                dependencies.javaLibraries.each {
//                    println "VAHostPlugin.generateDependencies javaLibraries = " + it.name
//                    deps.add(it.name.replace("@jar", "").replace("@aar", "").toString())
//                }
//                dependencies.libraries.each {
//                    println "VAHostPlugin.generateDependencies libraries = " + it.name
//                    deps.add(it.name.replace("@jar", "").replace("@aar", "").toString())
//                }
//                dependencies.javaModules.each {
//                    println "VAHostPlugin.generateDependencies javaModules = " + it.projectPath
//                }
//                dependencies.projects.each {
//                    println "VAHostPlugin.generateDependencies projects = " + it
//                }
//                dependencies.runtimeOnlyClasses.each {
//                    println "---------------"
//                    println "VAHostPlugin.generateDependencies runtimeOnlyClasses id= ${it.id.toString()}"
//                    println "VAHostPlugin.generateDependencies runtimeOnlyClasses it.id.class= ${it.id.class}"
//                    println "VAHostPlugin.generateDependencies runtimeOnlyClasses it.id.componentIdentifier= ${it.id.componentIdentifier.class}"
//                    //library-tray:debug:unspecified 0
//                    def id = it.id
//                    if (id instanceof PublishArtifactLocalArtifactMetadata) {
//                        //{
//                        // artifactType=jar,
//                        // com.android.build.api.attributes.BuildTypeAttr=debug,
//                        // com.android.build.api.attributes.VariantAttr=debug,
//                        // com.android.build.gradle.internal.dependency.AndroidTypeAttr=Aar,
//                        // org.gradle.libraryelements=jar,
//                        // org.gradle.usage=java-runtime
//                        // }
//                        def attributes = it.variant.attributes
//                        def variantAttr = attributes.getAttribute(Attribute.of("com.android.build.api.attributes.VariantAttr", VariantAttr))
//                        def metaData = ((PublishArtifactLocalArtifactMetadata) id)
//                        def component = ((DefaultProjectComponentIdentifier) metaData.componentIdentifier)
//                        deps.add("${component.projectName}:${variantAttr}:unspecified 0".toString())
//                        println "[VAHostPlugin.generateDependencies] ProjectComponentIdentifier.${id.name}.metaData=" + metaData
//                    } else if (id instanceof ComponentFileArtifactIdentifier) {
//                        def artifactName = ((ComponentFileArtifactIdentifier) id).componentIdentifier.toString()
////                        def artifactName = "${identifier.group}:${id.module}:${id.version} ${it.artifactFile.length()}"
//                        deps.add(artifactName)
//                        println "[VAHostPlugin.generateDependencies] ComponentFileArtifactIdentifier.${id.displayName}.artifactName=" + artifactName
//                    } else if (id instanceof DefaultModuleComponentArtifactIdentifier) {
////                        def identifier = id as DefaultModuleComponentArtifactIdentifier
//                        def artifactName = id.componentIdentifier.toString()
//                        deps.add(artifactName)
//                        println "[VAHostPlugin.generateDependencies] DefaultModuleComponentArtifactIdentifier.${id.displayName}.artifactName=" + artifactName
//                    } else if (id instanceof ModuleComponentIdentifier) {
//                        def artifactName = "${id.group}:${id.module}:${id.version} ${it.artifactFile.length()}".toString()
//                        deps.add(artifactName)
//                        println "[VAHostPlugin.generateDependencies] ModuleComponentIdentifier.${id.displayName}.artifactName=" + artifactName
//                    } else if (id instanceof OpaqueComponentArtifactIdentifier) {
//                        //DefaultResolvedArtifactResult
//                        println "VAHostPlugin.generateDependencies ${id.componentIdentifier}"
//                        def artifactName = "${it.variant.displayName}:${it.file.path}:unspecified ${it.file.length()}".toString()
//                        deps.add(artifactName)
//                        println "[VAHostPlugin.generateDependencies] other.${id.displayName}.artifactName=" + artifactName
//                    } else {
//                        throw new RuntimeException("other type,config it")
//                    }
//                }

            Collections.sort(deps)
            return deps
        })
    }

    /**
     * Save R symbol file
     */
    def backupHostR(ApplicationVariant applicationVariant) {
        final ProcessAndroidResources aaptTask = this.project.tasks["process${applicationVariant.name.capitalize()}Resources"]
        project.copy {
            from aaptTask.textSymbolOutputFile
            into vaHostDir
            rename { "Host_R.txt" }
        }
    }
    /**
     */
    def backupHostClass(ApplicationVariant applicationVariant) {
        def minified = new File(project.buildDir.path + "/intermediates/shrunk_jar/" + applicationVariant.name + "/minified.jar")
        println "VAHostPlugin.backupHostClass from ${minified.path}"
        project.copy {
            from minified
            into vaHostDir
            rename { "minified.jar" }
        }
    }
    /**
     */
    def backupHostDex(ApplicationVariant applicationVariant) {
//        "/Users/sansecy/didi-gradle-plugin-422/host/build/intermediates/dex/release/minifyReleaseWithR8"
        if (applicationVariant.buildType.minifyEnabled) {
            def minified = new File("${project.buildDir.path}/intermediates/dex/${applicationVariant.name}/minify${applicationVariant.name.capitalize()}WithR8")
            if (minified.exists()) {
                println "VAHostPlugin.backupHostDex from ${minified.path}"
                def backupDexDir = vaHostDir.path + "/minifyReleaseWithR8"
                new File(backupDexDir).mkdirs()
                project.copy {
                    from minified
                    into backupDexDir
                }
            }
        } else {
            def minified = new File("${project.buildDir.path}/intermediates/dex/${applicationVariant.name}/mergeDex${applicationVariant.name.capitalize()}")
            if (minified.exists()) {
                println "VAHostPlugin.backupHostDex from ${minified.path}"
                def backupDexDir = vaHostDir.path + "/minifyReleaseWithR8"
                new File(backupDexDir).mkdirs()
                project.copy {
                    from minified
                    into backupDexDir
                }
            }

        }
    }

/**
 * Keep the host app resource id same with last publish, in order to compatible with the published plugin
 */
    def keepResourceIds(variant) {


        def VIRTUAL_APK_DIR = new File([project.rootDir, 'virtualapk'].join(File.separator))
        System.println("keepResource start")
        def mergeResourceTask = project.tasks["merge${variant.name.capitalize()}Resources"]
        def vaDir = new File(VIRTUAL_APK_DIR, "${variant.dirName}")

        def rSymbole = new File(vaDir, 'Host-R.txt')
        if (!rSymbole.exists()) {
            return
        }

        File resDir = new File(project.projectDir, ['src', 'main', 'res'].join(File.separator))
        File mergedValuesDir = new File(mergeResourceTask.outputDir, 'values')

        mergeResourceTask.doFirst {
            generateIdsXml(rSymbole, resDir)
        }

        mergeResourceTask.doLast {

            def mergeXml = new File(variant.mergeResources.incrementalFolder, 'merger.xml')
            def typeEntries = [:] as Map<String, Set>

            collectResourceEntries(mergeXml, resDir.path, typeEntries)

            generatePublicXml(rSymbole, mergedValuesDir, typeEntries)

            new File(resDir, 'values/ids.xml').delete()
        }
    }


    def collectResourceEntries(final File mergeXml, final String projectResDir, final Map typeEntries) {

        collectAarResourceEntries(null, projectResDir, mergeXml, typeEntries)

        File aarDir = new File(project.buildDir, "intermediates/exploded-aar")

        project.configurations.compile.resolvedConfiguration.resolvedArtifacts.each {
            if (it.extension == 'aar') {
                def moduleVersion = it.moduleVersion.id
                def resPath = new File(aarDir, "${moduleVersion.group}/${moduleVersion.name}/${moduleVersion.version}/res")
                collectAarResourceEntries(moduleVersion.version, resPath.path, mergeXml, typeEntries)
            }
        }
    }


    def collectAarResourceEntries(String aarVersion, String resPath, File mergeXml, final Map typeEntries) {
        final def merger = new XmlParser().parse(mergeXml)
        def filter = aarVersion == null ? {
            it.@config == 'main' || it.@config == 'release'
        } : {
            it.@config = aarVersion
        }
        def dataSets = merger.dataSet.findAll filter
        dataSets.each {
            it.source.each {
                if (it.@path != resPath) {
                    return
                }
                it.file.each {
                    def String type = it.@type
                    if (type != null) {
                        def entrySet = getEntriesSet(type, typeEntries)
                        if (!entrySet.contains(it.@name)) {
                            entrySet.add(it.@name)
                        }
                    } else {
                        it.children().each {
                            type = it.name()
                            def name = it.@name
                            if (type.endsWith('-array')) {
                                type = 'array'
                            } else if (type == 'item') {
                                type = it.@type
                            } else if (type == 'declare-styleable') {
                                return
                            }
                            def entrySet = getEntriesSet(type, typeEntries)
                            if (!entrySet.contains(name)) {
                                entrySet.add(name)
                            }
                        }
                    }
                }
            }
        }
    }

    def generatePublicXml(rSymboleFile, destDir, hostResourceEntries) {
        def styleNameMap = [:] as Map
        def styleEntries = hostResourceEntries['style']
        styleEntries.each {
            def _styleName = it.replaceAll('\\.', '_')
            styleNameMap.put(_styleName, it)
        }

        def lastSplitType
        new File(destDir, "public.xml").withPrintWriter { pw ->
            pw.println "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            pw.println "<resources>"
            rSymboleFile.eachLine { line ->
                def values = line.split(' ')
                def type = values[1]
                if (type == 'styleable') {
                    return
                }
                if (type == 'style') {
                    if (styleNameMap.containsKey(values[2])) {
                        pw.println "\t<public type=\"${type}\" name=\"${styleNameMap.get(values[2])}\" id=\"${values[3]}\" />"
                    }
                    return
                }
                //ID does not filter and remains redundant
                if (type == 'id') {
                    pw.println "\t<public type=\"${type}\" name=\"${values[2]}\" id=\"${values[3]}\" />"
                    return
                }

                //Only keep resources' Id that are present in the current project
                Set entries = hostResourceEntries[type]
                if (entries != null && entries.contains(values[2])) {
                    pw.println "\t<public type=\"${type}\" name=\"${values[2]}\" id=\"${values[3]}\" />"
                } else {
                    if (entries == null) {
                        if (type != lastSplitType) {
                            lastSplitType = type
                            println ">>>> ${type} is splited"
                        }

                    } else {
                        if (type != 'attr') {
                            println ">>>> ${type} : ${values[2]} is deleted"
                        }

                    }
                }

            }
            pw.print "</resources>"
        }
    }

    def generateIdsXml(rSymboleFile, resDir) {
        new File(resDir, "values/ids.xml").withPrintWriter { pw ->
            pw.println "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            pw.println "<resources>"
            rSymboleFile.eachLine { line ->
                def values = line.split(' ')
                if (values[1] == 'id')
                    pw.println "\t<item type=\"id\" name=\"${values[2]}\"/>"
            }
            pw.print "</resources>"
        }
    }


    def Set<String> getEntriesSet(final String type, final Map typeEntries) {
        def entries = typeEntries[type]
        if (entries == null) {
            entries = [] as Set<String>
            typeEntries[type] = entries
        }
        return entries
    }
}