package com.didi.virtualapk.transform;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider;

abstract class Unzip implements TransformAction<TransformParameters.None> {
    @InputArtifact
    abstract Provider<FileSystemLocation> getInputArtifact()

    @Override
    void transform(TransformOutputs outputs) {
        println "Unzip.transform " + outputs
        def input = inputArtifact.get().asFile
        def unzipDir = outputs.dir(input.name)
        unzipTo(input, unzipDir)
    }

    private static void unzipTo(File zipFile, File unzipDir) {
        // implementation...
    }
}
