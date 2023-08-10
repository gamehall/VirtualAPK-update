/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.didi.virtualapk.collector;

import com.android.annotations.NonNull;
import com.android.annotations.concurrency.Immutable;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.Dependencies;
import com.android.builder.model.JavaLibrary;
import com.google.common.base.MoreObjects;

import org.gradle.api.artifacts.result.ResolvedArtifactResult;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Implementation of {@link Dependencies} interface
 */
@Immutable
public class DependenciesImpl2 implements Serializable {
    private static final long serialVersionUID = 2L;

    @Immutable
    public static class ProjectIdentifierImpl implements Dependencies.ProjectIdentifier, Serializable {
        private static final long serialVersionUID = 1L;

        @NonNull
        private final String buildId;
        @NonNull
        private final String projectPath;

        public ProjectIdentifierImpl(@NonNull String buildId, @NonNull String projectPath) {
            this.buildId = buildId;
            this.projectPath = projectPath;
        }

        @NonNull
        
        public String getBuildId() {
            return buildId;
        }

        @NonNull
        
        public String getProjectPath() {
            return projectPath;
        }

        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProjectIdentifierImpl that = (ProjectIdentifierImpl) o;
            return Objects.equals(buildId, that.buildId)
                    && Objects.equals(projectPath, that.projectPath);
        }

        
        public int hashCode() {
            return Objects.hash(buildId, projectPath);
        }
    }

    @NonNull
    private final List<AndroidLibrary> libraries;
    @NonNull
    private final List<JavaLibrary> javaLibraries;
    @NonNull
    private final Set<String> projects;
    @NonNull
    private final List<Dependencies.ProjectIdentifier> javaModules;
    @NonNull
    private final List<ResolvedArtifactResult> runtimeOnlyClasses;

    public DependenciesImpl2(
            @NonNull List<AndroidLibrary> libraries,
            @NonNull List<JavaLibrary> javaLibraries,
            @NonNull List<Dependencies.ProjectIdentifier> javaModules,
            @NonNull List<ResolvedArtifactResult> runtimeOnlyClasses) {
        this.libraries = libraries;
        this.javaLibraries = javaLibraries;
        this.javaModules = javaModules;
        this.runtimeOnlyClasses = runtimeOnlyClasses;
        projects = new HashSet<>();
        for (Dependencies.ProjectIdentifier javaModule : javaModules) {
            projects.add(javaModule.getProjectPath());
        }

    }

    @NonNull
    
    public Collection<AndroidLibrary> getLibraries() {
        return libraries;
    }

    @NonNull
    
    public Collection<JavaLibrary> getJavaLibraries() {
        return javaLibraries;
    }

    @NonNull
    
    public Collection<String> getProjects() {
        return projects;
    }

    @NonNull
    
    public List<Dependencies.ProjectIdentifier> getJavaModules() {
        return javaModules;
    }

    @NonNull
    public Collection<ResolvedArtifactResult> getRuntimeOnlyClasses() {
        return runtimeOnlyClasses;
    }

    
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("libraries", libraries)
                .add("javaLibraries", javaLibraries)
                .add("javaModules", javaModules)
                .add("projects", projects)
                .add("runtimeOnlyClasses", runtimeOnlyClasses)
                .toString();
    }

    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DependenciesImpl2 that = (DependenciesImpl2) o;
        return Objects.equals(libraries, that.libraries)
                && Objects.equals(javaLibraries, that.javaLibraries)
                && Objects.equals(projects, that.projects)
                && Objects.equals(javaModules, that.javaModules)
                && Objects.equals(runtimeOnlyClasses, that.runtimeOnlyClasses);
    }

    
    public int hashCode() {
        return Objects.hash(libraries, javaLibraries, projects, javaModules, runtimeOnlyClasses);
    }
}
