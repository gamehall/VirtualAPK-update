package com.didi.virtualapk.utils
/**
 * Created by qiaopu on 2017/9/4.
 */
public class FileUtil {
    
    public static void saveFile(File dir, String fileName, Closure<List<?>> action) {
        List<?> list = action.call();
        saveFile(dir, fileName, list)
    }
    
    public static void saveFile(File dir, String fileName, Collection<?> collection) {
        saveFile(dir, fileName, false, collection)
    }

    public static void saveFile(File dir, String fileName, boolean sort, Collection<?> collection) {
        dir.mkdirs()
        def file = new File(dir, "${fileName}.txt")
        ArrayList<?> list = new ArrayList<>(collection)
        if (sort) {
            Collections.sort(list)
        }
        list.add('')
        file.write(list.join('\r\n'))
    }
    
    public static boolean deleteEmptySubfolders(File dir) {
        if(dir == null || !dir.exists()) {
            return true;
        }
        if(!dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();

        if (files == null || files.length == 0) {
            return dir.delete();
        }

        boolean result = true;
        int len = files.length;

        for(int i = 0; i < len; ++i) {
            File file = files[i];
            if(file.isDirectory()) {
                if(!deleteEmptySubfolders(file)) {
                    result = false;
                }
            }
        }

        File[] updatedFiles = dir.listFiles();
        if (updatedFiles == null || updatedFiles.length == 0) {
            if (!dir.delete()) {
                result = false;
            }
        }

        return result;
    }

    static void deleteFile(File file) {
        file.delete()
    }
    static void deleteFile(String path) {
        ///Users/sansecy/Downloads/shanpao/shanpao_tv/business/bicycle/build/intermediates/merged_native_libs/debug/outlib/armeabi-v7a/libbmsdl.so
        ///Users/sansecy/Downloads/shanpao/shanpao_tv/business/bicycle/build/intermediates/merged_native_libs/debug/out/lib/armeabi-v7a/libshanpao.so
        try {
            def file = new File(path)
            if (!file.exists()) {
                return
            }
            def result = new File(path).delete()
            println "FileUtil.deleteFile ${path} result = ${result}"
        } catch (Exception e) {

        }
    }

    static findFileInDie(File file, List<String> list) {
        list.add(file.listFiles(new FileFilter() {
            @Override
            boolean accept(File item) {
                if (item.isFile()) {
                    return true
                }
                return false
            }
        }))
    }
}
