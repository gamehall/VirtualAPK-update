package com.didi.virtualapk.collector.dependence

import com.android.builder.model.JavaLibrary

/**
 * Represents a Jar file. This could be the output of a Java project.
 *
 * @author zhengtao
 */
class JarDependenceInfo extends DependenceInfo {

    JavaLibrary library
    File jarFile

    JarDependenceInfo(String group, String artifact, String version, JavaLibrary library) {
        super(group, artifact, version)
        this.library = library
//        library.jarFile.path .replace("")
        jarFile = new File(library.jarFile.path.replace("full_jar", "runtime_library_classes")
                .replace("createFullJarDebug/full.jar/jars/", ""))
    }

    @Override
    File getJarFile() {
//        Log.i 'JarDependenceInfo', "Found jar file: ${library}"
        return jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.JAR
    }
}