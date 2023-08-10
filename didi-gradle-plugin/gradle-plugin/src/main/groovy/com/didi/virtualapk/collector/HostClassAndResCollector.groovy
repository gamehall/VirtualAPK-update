package com.didi.virtualapk.collector

import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.utils.FileUtil
import org.gradle.api.Project

import java.util.zip.ZipFile

/**
 * Collector of Class and Java Resource(no-class files in jar) in host apk
 *
 * @author zhengtao
 */

class HostClassAndResCollector {

    private LinkedHashMap<DependenceInfo, List<File>> hostJarFiles = new LinkedHashMap<>()
    private def hostClassesAndResources = [] as LinkedHashSet<String>

    Project project

    HostClassAndResCollector(Project project) {
        this.project = project
    }
/**
 * Collect jar entries that already exist in the host apk
 *
 * @param stripDependencies DependencyInfos that exists in the host apk, including AAR and JAR
 * @return set of classes and java resources
 */
    public Set<String> collect(Collection<DependenceInfo> stripDependencies) {
        flatToJarFiles(stripDependencies, hostJarFiles)
        def all = new ArrayList<String>()
        hostJarFiles.each { entry ->
            try {
                entry.value.each { file ->
                    def unzipFile = unzipJar(file)
                    hostClassesAndResources.addAll(unzipFile)
                    all.add(entry.key.name + " --- ${file.path} --- " + unzipFile.size())
                }
            } catch (Exception e) {
                throw new FileNotFoundException(entry.key.toString() + "'s file Not Found,, msg = " + e.getMessage())
            }
        }
        FileUtil.saveFile(project.buildDir, "collect_classes", all)
        hostClassesAndResources
    }

    /**
     * Collect the jar files that are held by the DependenceInfo， including local jars of the DependenceInfo
     * @param stripDependencies Collection of DependenceInfo
     * @param jarFiles Collection used to store jar files
     */
    static def flatToJarFiles(Collection<DependenceInfo> stripDependencies, HashMap<DependenceInfo, List<File>> jarFiles) {
        stripDependencies.each { info ->
            def item = jarFiles.get(info)
            if (item == null) {
                item = new ArrayList<File>()
            }
            item.add(info.jarFile)
            jarFiles.put(info, item)
            if (info instanceof AarDependenceInfo) {
                info.localJars.each { file ->
                    item.add(file)
                }
            }
        }
    }

    /**
     * Unzip the entries of Jar
     *
     * @return Set of entries in the JarFile
     */
    public static Set<String> unzipJar(File jarFile) {

        def jarEntries = [] as Set<String>

        ZipFile zipFile = new ZipFile(jarFile)
        try {
            zipFile.entries().each {
                jarEntries.add(it.name)
            }
        } finally {
            zipFile.close();
        }

        return jarEntries
    }

}