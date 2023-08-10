/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.didi.virtualapk

import com.android.build.gradle.internal.ide.DependenciesImpl
import com.android.build.gradle.internal.ide.dependencies.DependencyModelBuilder
import com.android.build.gradle.internal.ide.dependencies.Level1ArtifactHandler
import com.android.build.gradle.internal.ide.dependencies.ResolvedArtifact
import com.google.common.collect.ImmutableList
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.services.BuildServiceRegistry

class Level1DependencyModelBuilder2 implements DependencyModelBuilder<DependenciesImpl> {
    BuildServiceRegistry buildServiceRegistry

    Level1DependencyModelBuilder2(BuildServiceRegistry buildServiceRegistry) {
        this.buildServiceRegistry = buildServiceRegistry
    }
    private Level1ArtifactHandler artifactHandler = new Level1ArtifactHandler(buildServiceRegistry)

    private List<File> runtimeClasspath

    @Override
    void addArtifact(ResolvedArtifact artifact,
                     boolean isProvided,
                     Map<ComponentIdentifier, ? extends File> lintJarMap,
                     ClasspathType type) {
        // there's not need to check the return value of this handler as the handler itself
        // accumulate the result.
        // This is because unlike the newer dependency model, this model accumulate the different
        // types into separate list, so it's better handler by the artifact handler.
        artifactHandler.handleArtifact(
                artifact,
                isProvided,
                lintJarMap
        )
    }

    public boolean needFullRuntimeClasspath = true
    public boolean needRuntimeOnlyClasspath = true

    boolean getNeedFullRuntimeClasspath() {
        return needFullRuntimeClasspath
    }

    void setNeedFullRuntimeClasspath(boolean needFullRuntimeClasspath) {
        this.needFullRuntimeClasspath = needFullRuntimeClasspath
    }

    boolean getNeedRuntimeOnlyClasspath() {
        return needRuntimeOnlyClasspath
    }

    void setNeedRuntimeOnlyClasspath(boolean needRuntimeOnlyClasspath) {
        this.needRuntimeOnlyClasspath = needRuntimeOnlyClasspath
    }

    void setRuntimeOnlyClasspath(ImmutableList<File> files) {
        runtimeClasspath = files
    }

    @Override
    DependenciesImpl createModel() {
        return new DependenciesImpl(
                artifactHandler.androidLibraries,
                artifactHandler.javaLibraries,
                artifactHandler.projects,
                runtimeClasspath
        )
    }
}