package com.didi.virtualapk.collector.dependence

class ArtifactDependenceInfo extends DependenceInfo {
    File file

    ArtifactDependenceInfo(String group, String artifact, String version, File file) {
        super(group, artifact, version)
        this.file = file
    }

    @Override
    File getJarFile() {
        return file
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.JAR
    }
}