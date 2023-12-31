package com.didi.virtualapk.transform

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didi.virtualapk.VAExtension
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

public class DxTransform extends Transform {

    VAExtension virtualApk;
    Project project;
    VAExtension.VAContext vaContext

    public DxTransform(Project project, VAExtension virtualApk) {
        this.project = project
        this.virtualApk = virtualApk;
        vaContext = virtualApk.getVaContext()
    }

    @Override
    public String getName() {
        return "DxTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
//        return TransformManager.CONTENT_DEX_WITH_RESOURCES;
//        return TransformManager.CONTENT_RESOURCES;
//        return TransformManager.CONTENT_CLASS;
        return TransformManager.CONTENT_JARS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        if (!isIncremental()) {
            transformInvocation.outputProvider.deleteAll()
        }
//        _transform(transformInvocation.context, transformInvocation.inputs, transformInvocation.outputProvider)
        handle(transformInvocation)
    }

    private Collection<TransformInput> handle(TransformInvocation transformInvocation) {
        transformInvocation.inputs.each {
            it.directoryInputs.each { directoryInput ->
                def destDir = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
//                if (virtualApk.stripResource) {
//                    handleFile(it.file)
//                } else {
//                }
//                transform(transformInvocation.context,)
                directoryInput.file.traverse(type: FileType.FILES) {
                    def entryName = it.path.substring(directoryInput.file.path.length() + 1)
                    def dest = new File(destDir, entryName)
                    FileUtils.copyFile(it, dest)
                }
                println "DxTransform.transform directoryInputs = " + it + " destDir=" + destDir
            }
            it.jarInputs.each { jarInput ->
                def destDir = transformInvocation.outputProvider.getContentLocation(
                        jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                println "DxTransform.transform jarInputs = " + jarInput.file + " destDir=" + destDir
//                handleFile(it.file)
                def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }


    /**
     *
     * @param context
     * @param inputs 有两种类型，一种是目录，一种是 jar 包，要分开遍历
     * @param outputProvider 输出路径
     */
    void _transform(Context context, Collection<TransformInput> inputs, TransformOutputProvider outputProvider) throws IOException, TransformException, InterruptedException {
        if (!incremental) {
            //不是增量更新删除所有的outputProvider
            outputProvider.deleteAll()
        }
        inputs.each { TransformInput input ->
            //遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectoryInput(directoryInput, outputProvider)
            }
            // 遍历jar 第三方引入的 class
            input.jarInputs.each { JarInput jarInput ->
                handleJarInput(jarInput, outputProvider)
            }
        }
    }

    void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        println "handleDirectoryInput directoryInput = $directoryInput, outputProvider = $outputProvider"
        if (directoryInput.file.isDirectory()) {
            directoryInput.file.eachFileRecurse { File file ->
                String name = file.name
                if (filterClass(name)) {
                    println "DxTransform.handleDirectoryInput 应该处理 " + entryName + ""
                    // 用来读 class 信息
//                    ClassReader classReader = new ClassReader(file.bytes)
//                    // 用来写
//                    ClassWriter classWriter = new ClassWriter(0 /* flags */)
//                    //todo 改这里就可以了x
//                    ClassVisitor classVisitor = new BuryPointVisitor(classWriter)
//                    // 下面还可以包多层
//                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
//                    // 重新覆盖写入文件
//                    byte[] code = classWriter.toByteArray()
//                    FileOutputStream fos = new FileOutputStream(
//                            file.parentFile.absolutePath + File.separator + name)
//                    fos.write(code)
//                    fos.close()
                }
            }
        }
        // 把修改好的数据，写入到 output
        def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes,
                directoryInput.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    void handleJarInput(JarInput jarInput, TransformOutputProvider outputProvider) {
        println "handleJarInput jarInput = $jarInput, outputProvider = $outputProvider"
        if (jarInput.file.absolutePath.endsWith(".jar")) {
            // 重名名输出文件,因为可能同名,会覆盖
            def jarName = jarInput.name
            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
            //用于保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                //插桩class
                if (filterClass(entryName)) {
                    println "DxTransform.handleJarInput 应该处理 " + entryName + ""
                    //class文件处理
//                    jarOutputStream.putNextEntry(zipEntry)
//                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
//                    ClassWriter classWriter = new ClassWriter(0)
//                    //todo 改这里就可以了
//                    ClassVisitor classVisitor = new BuryPointVisitor(classWriter)
//                    // 下面还可以包多层
//                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
//                    byte[] code = classWriter.toByteArray()
//                    jarOutputStream.write(code)
                } else {
//                    jarOutputStream.putNextEntry(zipEntry)
//                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }
            //结束
            jarOutputStream.close()
            jarFile.close()
            def dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }

    boolean filterClass(String className) {
//        return (className.endsWith(".class") && !className.startsWith("R\$") && "R.class" != className && "BuildConfig.class" != className)
        return className.endsWith(".class") && (className.contains("R\$") || className.contains("R.class"))
    }

}
