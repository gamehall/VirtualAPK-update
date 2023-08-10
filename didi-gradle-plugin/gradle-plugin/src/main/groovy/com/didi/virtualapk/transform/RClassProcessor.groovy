package com.didi.virtualapk.transform

import com.didi.virtualapk.VAExtension
import com.didi.virtualapk.utils.Log
import groovy.io.FileType
import org.apache.commons.io.FilenameUtils
import org.gradle.api.JavaVersion
import org.gradle.api.Project

class RClassProcessor {

    Project project;
    VAExtension.VAContext vaContext;

    RClassProcessor(Project project) {
        this.project = project
        vaContext = VAExtension.getVaContext()
    }

    void handleDir(File dir) {
        dir.list().each {
            println "handleDir subdir = $it"
        }
        dir.eachFileRecurse(FileType.FILES) { file ->
            compileR(dir, dir)
            if (file.name.endsWith(".java")) {
                file.delete()
            }
        }
        project.ant.zip(baseDir: dir, destFile: new File(dir, "R.jar"))
    }

    void handleFile(File file) {
        println "RClassProcessor.handleFile " + file
        if (file.directory && file.path.endsWith(vaContext.packagePath)) {
            if (recompileSplitR(file)) {
                Log.i 'DxTaskHooker', "Recompiled R.java in dir: ${file.absoluteFile}"
            }
        } else if (file.file && file.name.endsWith('.jar')) {
            // Decompress jar file
            File unzipJarDir = new File(file.parentFile, FilenameUtils.getBaseName(file.name))
            project.copy {
                from project.zipTree(file)
                into unzipJarDir
            }
            // VirtualApk Package Dir
            File pkgDir = new File(unzipJarDir, vaContext.packagePath)
            if (pkgDir.exists()) {
                if (recompileSplitR(pkgDir)) {
                    Log.i 'DxTaskHooker', "Recompiled R.java in jar: ${file.absoluteFile}"
                    File backupDir = new File(vaContext.getBuildDir(project), 'origin/classes')
                    backupDir.deleteDir()
                    project.copy {
                        from file
                        into backupDir
                    }
                    project.ant.zip(baseDir: unzipJarDir, destFile: file)
                }
            }
        }
    }

    /**
     * Delete the large R class file under the applicationId namespace, then
     * compile the splitRJavaFile to generate the R class file only records
     * plugin resources
     *
     * @param pkgDir The path to storing the R class file
     * @return true if the search&delete&compile actions succeed
     */
    boolean recompileSplitR(File pkgDir) {

        File[] RClassFiles = pkgDir.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name.startsWith('R$') && name.endsWith('.class')
            }
        })

        if (RClassFiles?.length) {
            RClassFiles.each {
                it.delete()
            }

            String baseDir = pkgDir.path - "${File.separator}${vaContext.packagePath}"
            project.ant.javac(
                    srcdir: vaContext.splitRJavaFile.parentFile,
                    source: JavaVersion.VERSION_1_8,
                    target: JavaVersion.VERSION_1_8,
                    destdir: new File(baseDir))

            return true
        }

        return false
    }

    void compileR(File srcDir, File destDir) {
        println "compileR srcDir = $srcDir, destDir = $destDir"
        project.ant.javac(
                srcdir: srcDir,
                source: JavaVersion.VERSION_1_8,
                target: JavaVersion.VERSION_1_8,
                destdir: destDir)
    }
}
