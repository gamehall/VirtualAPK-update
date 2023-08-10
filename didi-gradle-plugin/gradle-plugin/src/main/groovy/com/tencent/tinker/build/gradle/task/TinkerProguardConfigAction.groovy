/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tinker.build.gradle.task

import com.didi.virtualapk.VAExtension
import com.tencent.tinker.build.gradle.Compatibilities
import com.tencent.tinker.build.gradle.common.TinkerBuildPath
import com.tencent.tinker.build.util.FileOperation
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * The configuration properties.
 *
 * @author zhangshaowen
 */
class TinkerProguardConfigAction implements Action<Task> {
    static final String PROGUARD_CONFIG_SETTINGS =
            "-keepattributes *Annotation* \n" +
                    "-dontwarn com.tencent.tinker.anno.AnnotationProcessor \n" +
                    "-keep @com.tencent.tinker.anno.DefaultLifeCycle public class *\n" +
                    "-keep public class * extends android.app.Application {\n" +
                    "    *;\n" +
                    "}\n" +
                    "-keep public class com.tencent.tinker.entry.ApplicationLifeCycle {\n" +
                    "    *;\n" +
                    "}\n" +
                    "-keep public class * implements com.tencent.tinker.entry.ApplicationLifeCycle {\n" +
                    "    *;\n" +
                    "}\n" +
                    "-keep public class com.tencent.tinker.loader.TinkerLoader {\n" +
                    "    *;\n" +
                    "}\n" +
                    "-keep public class * extends com.tencent.tinker.loader.TinkerLoader {\n" +
                    "    *;\n" +
                    "}\n" +
                    "-keep public class com.tencent.tinker.loader.TinkerTestDexLoad {\n" +
                    "    *;\n" +
                    "}\n" +
                    "-keep public class com.tencent.tinker.entry.TinkerApplicationInlineFence {\n" +
                    "    *;\n" +
                    "}\n"

    def applicationVariant
    def project

    TinkerProguardConfigAction(project, variant) {
        this.project = project
        applicationVariant = variant
    }

    @Override
    void execute(Task task) {
        updateTinkerProguardConfig(project)
    }

    def updateTinkerProguardConfig(Project project) {
        def file = project.file(TinkerBuildPath.getProguardConfigPath(project))
        println("try update tinker proguard file with ${file}")

        // Create the directory if it doesnt exist already
        file.getParentFile().mkdirs()

        // Write our recommended proguard settings to this file
        FileWriter fr = new FileWriter(file.path)

        String applyMappingFile = VAExtension.vaContext.VAExtension.mapping

        //write applymapping
        if (FileOperation.isLegalFile(applyMappingFile)) {
            println("try add applymapping ${applyMappingFile} to build the package")
            fr.write("-applymapping " + applyMappingFile)
            fr.write("\n")
        } else {
            println("applymapping file ${applyMappingFile} is illegal, just ignore")
        }

        fr.write(PROGUARD_CONFIG_SETTINGS)
        //they will removed when apply
        fr.close()

        // Add this proguard settings file to the list
        injectTinkerProguardRuleFile(project, file)
    }

    private void injectTinkerProguardRuleFile(project, file) {
        def agpObfuscateTask = Compatibilities.getObfuscateTask(project, applicationVariant)
        def configurationFilesOwner = null
        def configurationFilesField = null
        try {
            configurationFilesOwner = agpObfuscateTask
            configurationFilesField = Compatibilities.getFieldRecursively(configurationFilesOwner.getClass(), '__configurationFiles__')
        } catch (Throwable ignored) {
            configurationFilesOwner = null
            configurationFilesField = null
        }
        if (configurationFilesField == null) {
            try {
                configurationFilesOwner = agpObfuscateTask
                configurationFilesField = Compatibilities.getFieldRecursively(configurationFilesOwner.getClass(), 'configurationFiles')
            } catch (Throwable ignored) {
                configurationFilesOwner = null
                configurationFilesField = null
            }
        }
        if (configurationFilesField == null) {
            try {
                configurationFilesOwner = agpObfuscateTask.transform
                configurationFilesField = Compatibilities.getFieldRecursively(configurationFilesOwner.getClass(), 'configurationFiles')
            } catch (Throwable ignored) {
                configurationFilesOwner = null
                configurationFilesField = null
            }
        }
        def agpConfigurationFiles = null
        boolean isOK = false
        if (configurationFilesOwner != null && configurationFilesField != null) {
            try {
                agpConfigurationFiles = configurationFilesField.get(configurationFilesOwner)
                isOK = true
            } catch (Throwable ignored) {
                isOK = false
            }
        }
        if (isOK) {
            def mergedConfigurationFiles = project.files(agpConfigurationFiles, project.files(file))
            try {
                configurationFilesField.set(configurationFilesOwner, mergedConfigurationFiles)
                def mergedConfigurationFilesForConfirm = configurationFilesField.get(configurationFilesOwner)
                println "Now proguard rule files are: ${mergedConfigurationFilesForConfirm.files}"
            } catch (Throwable ignored) {
                isOK = false
            }
        }
        if (!isOK) {
            throw new GradleException('Fail to inject tinker proguard rules file. Some compatibility works need to be done.')
        }
    }
}