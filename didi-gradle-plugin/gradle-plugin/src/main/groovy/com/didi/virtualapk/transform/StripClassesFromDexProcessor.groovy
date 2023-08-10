package com.didi.virtualapk.transform

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.collector.HostClassAndResCollector
import com.didi.virtualapk.utils.FileUtil
import com.didi.virtualapk.utils.ZipUtil
import org.gradle.api.Project

class StripClassesFromDexProcessor {
    private HostClassAndResCollector classAndResCollector
    private Project project
    private ApplicationVariantImpl appVariant
    private VAExtension virtualApk

    StripClassesFromDexProcessor(Project project, ApplicationVariantImpl appVariant) {
        this.project = project
        classAndResCollector = new HostClassAndResCollector(project)
        this.appVariant = appVariant
        this.virtualApk = project.virtualApk
    }

    void doAction() {
        println "StripClassesFromDexProcessor.doAction start"
//        LinkedHashSet stripEntries = classAndResCollector.collect(vaContext.stripDependencies)
//        def hostClasses = classAndResCollector.unzipJar(getHostShrunkJar())
//        hostClasses.each {
//            println "StripClassesFromDexProcessor.doAction ${it}"
//        }
//        FileUtil.saveFile(project.buildDir, "host_classes", stripEntries)

        File file = new File(project.rootProject.buildDir.path + "/host/minifyReleaseWithR8")
        //        def decodeDexFolder = new File(file.parent, "decodeDexFolder")
        //        decodeDexFolder.mkdirs()
        File[] files = file.listFiles()
        def dexJar = new ArrayList<File>()
        files.each {
            if (it.name.endsWith("dex2jar.jar")) {
                it.delete()
            }
//            /Users/sansecy/didi-gradle-plugin-422/tools/dex-tools-2.2-SNAPSHOT/d2j-dex2jar.sh -f -o classes2-dex2jar.jar /Users/sansecy/didi-gradle-plugin-422/host/build/intermediates/dex/release/minifyReleaseWithR8/classes2.dex
//            output .jar file, default is $current_dir/[file-name]-dex2jar.jar
            if (it.name.endsWith(".dex")) {
//                def decodeDexItemFolder = new File(decodeDexFolder.path, it.name.replace(".dex", ""))
//                decodeDexItemFolder.mkdir()

                def jarFile = new File(it.parent, it.name.replace(".dex", "") + "-dex2jar.jar")
                dexJar.add(jarFile)
                String output = jarFile.path
                println "decode host dex : ${it.name} to ${jarFile.name}"
                execCommand(String.format("${getDex2jar()} -f -o %s %s", output, it.path))
                println "decode host dex : ${it.name} to ${jarFile.name} finish"
            }
        }
        def hostClasses = new HashSet<String>()
        println "collecting host classes"
        dexJar.each {
            def jarClasses = HostClassAndResCollector.unzipJar(it)
            hostClasses.addAll(jarClasses)
            FileUtil.saveFile(project.buildDir, "host_classes", hostClasses)
        }
        println "collecting host ${hostClasses.size()} classes"
        def pluginDexDir
        if (appVariant.buildType.name.endsWith("release")) {
            pluginDexDir = new File("${project.buildDir}/intermediates/dex/${appVariant.name}/minify${appVariant.name.capitalize()}WithR8/")
        } else {
            pluginDexDir = new File("${project.buildDir}/intermediates/dex/${appVariant.name}/mergeDex${appVariant.name.capitalize()}")
        }
        println pluginDexDir
        def pluginDexList = pluginDexDir.listFiles()
        def pluginClasses = new HashSet<String>()

        int retainJarClassesSize = 0
        int originJarClassesSize = 0

        pluginDexList.each {
            if (it.name.endsWithIgnoreCase(".dex")) {
                def jarFile = new File(it.parent, it.name.replace(".dex", ".jar"))
                boolean firstJar = false
                if (it.name == "classes.jar") {
                    firstJar = true
                }
//            def jarFile = new File(it.parent, it.name.replace(".dex", "") + "-dex2jar.jar")
                String output = jarFile.path
                println "decode plugin dex : ${it.name} to ${jarFile.name}"
                execCommand(String.format("${getDex2jar()} -f -o %s %s", output, it.path))
                println "deleteing host classes from plugin dexJar : ${jarFile.name}"
                originJarClassesSize += HostClassAndResCollector.unzipJar(jarFile).size()

                ZipUtil.with(jarFile).deleteAll(hostClasses)
                def jarClasses = HostClassAndResCollector.unzipJar(jarFile)
                retainJarClassesSize += jarClasses.size()
                pluginClasses.addAll(jarClasses)
//                    if (!firstJar) {
//                        ZipUtil.with(jarFile).mergeInto(new File(it.parent, "classes.jar"))
//                    }
                it.delete()
                def newDexFile = new File(it.parent, it.name.replace(".jar", ".dex"))
//            def newDexFile = new File(it.parent, it.name.replace(".jar", "") + "-jar2dex.dex")
                println "tranform jar [${jarFile.name}] to dex [${newDexFile.name}]"
                execCommand(String.format("${getJar2Dex()} -f -o %s %s", newDexFile.path, output))
//                jarFile.delete()
            }
        }
        FileUtil.saveFile(project.buildDir, "plugin_classes", pluginClasses)
        println "StripClassesFromDexProcessor.doAction , retainJarClassesSize = ${retainJarClassesSize} ,originJarClassesSize =  ${originJarClassesSize}"

//        println "StripClassesFromDexProcessor.doAction after delete class , ${minified.name} file size = ${minified.length()}"
        println "StripClassesFromDexProcessor.doAction finish"
//        throw new RuntimeException("stop")
    }

    private File getHostShrunkJar() {
        VAExtension.VAContext vaContext = virtualApk.getVaContext()
        return new File(project.rootProject.buildDir.path + "/host/minified.jar")
    }


    String getDex2jar() {
        def osVersion = System.getProperty("os.name")
        println osVersion
        if (osVersion.containsIgnoreCase("windows")) {
            return "${project.rootProject.rootDir.path}/tools/dex-tools-2.2-SNAPSHOT/d2j-dex2jar.bat"
        } else {
            return "${project.rootProject.rootDir.path}/tools/dex-tools-2.2-SNAPSHOT/d2j-dex2jar.sh"
        }
    }

    String getJar2Dex() {
        def osVersion = System.getProperty("os.name")
        println osVersion
        if (osVersion.containsIgnoreCase("windows")) {
            return "${project.rootProject.rootDir.path}/tools/dex-tools-2.2-SNAPSHOT/d2j-jar2dex.bat"
        } else {
            return "${project.rootProject.rootDir.path}/tools/dex-tools-2.2-SNAPSHOT/d2j-jar2dex.sh"
        }
    }

    static String execCommand(command) {
        println "execCommand = [$command]"
        Runtime runtime = Runtime.getRuntime()
        Process p = runtime.exec(command)
        InputStream fis = p.getInputStream()
        InputStreamReader isr = new InputStreamReader(fis)
        BufferedReader br = new BufferedReader(isr)
        String line = null
        StringBuilder sb = new StringBuilder()
        while ((line = br.readLine()) != null) {
            sb.append(line)
        }
        br.close()
        isr.close()
        fis.close()
        def result = sb.toString()
        println "result = [$result]"
        return result
    }
}