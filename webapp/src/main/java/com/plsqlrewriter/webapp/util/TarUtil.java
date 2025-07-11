package com.plsqlrewriter.webapp.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.nio.file.*;

public class TarUtil {
    
    /**
     * 解压tar文件（支持.tar, .tar.gz, .tar.bz2, .gz, .bz2等格式）
     * @param archiveFile 压缩文件
     * @param destDir 目标目录
     * @throws IOException 解压异常
     */
    public static void extract(File archiveFile, File destDir) throws IOException {
        String fileName = archiveFile.getName().toLowerCase();
        
        try (FileInputStream fis = new FileInputStream(archiveFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             InputStream decompressedStream = createDecompressedStream(bis, fileName);
             TarArchiveInputStream tis = new TarArchiveInputStream(decompressedStream)) {
            
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                File outputFile = new File(destDir, entry.getName());
                
                // 安全检查：防止路径穿越攻击
                if (!outputFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                    throw new IOException("Entry is trying to leave the target directory: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    // 确保父目录存在
                    outputFile.getParentFile().mkdirs();
                    
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        IOUtils.copy(tis, fos);
                    }
                    
                    // 设置文件权限（如果需要）
                    if (entry.getMode() != 0) {
                        try {
                            Files.setPosixFilePermissions(outputFile.toPath(), 
                                getPosixFilePermissions(entry.getMode()));
                        } catch (UnsupportedOperationException e) {
                            // Windows系统不支持POSIX权限，忽略
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 根据文件名创建相应的解压流
     */
    private static InputStream createDecompressedStream(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
            return new GzipCompressorInputStream(inputStream);
        } else if (fileName.endsWith(".tar.bz2") || fileName.endsWith(".tbz2")) {
            return new BZip2CompressorInputStream(inputStream);
        } else if (fileName.endsWith(".gz")) {
            return new GzipCompressorInputStream(inputStream);
        } else if (fileName.endsWith(".bz2")) {
            return new BZip2CompressorInputStream(inputStream);
        } else {
            // 普通tar文件
            return inputStream;
        }
    }
    
    /**
     * 将Unix权限模式转换为Java NIO的PosixFilePermissions
     */
    private static java.util.Set<java.nio.file.attribute.PosixFilePermission> getPosixFilePermissions(int mode) {
        java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions = java.util.EnumSet.noneOf(java.nio.file.attribute.PosixFilePermission.class);
        
        if ((mode & 0400) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_READ);
        if ((mode & 0200) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE);
        if ((mode & 0100) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE);
        if ((mode & 0040) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.GROUP_READ);
        if ((mode & 0020) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.GROUP_WRITE);
        if ((mode & 0010) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE);
        if ((mode & 0004) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.OTHERS_READ);
        if ((mode & 0002) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE);
        if ((mode & 0001) != 0) permissions.add(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE);
        
        return permissions;
    }
    
    /**
     * 检查文件是否为支持的压缩格式
     */
    public static boolean isSupportedArchive(String fileName) {
        if (fileName == null) return false;
        String name = fileName.toLowerCase();
        return name.endsWith(".tar") || 
               name.endsWith(".tar.gz") || 
               name.endsWith(".tgz") || 
               name.endsWith(".tar.bz2") || 
               name.endsWith(".tbz2") || 
               name.endsWith(".gz") || 
               name.endsWith(".bz2");
    }
    
    /**
     * 将目录打包成tar.gz文件
     * @param sourceDir 源目录
     * @param tarGzFile 目标tar.gz文件
     * @throws IOException 打包异常
     */
    public static void createTarGz(File sourceDir, File tarGzFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(tarGzFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos);
             org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(gzos)) {
            
            taos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX);
            addFilesToTar(taos, sourceDir, sourceDir.toPath());
            taos.finish();
        }
    }
    
    /**
     * 递归添加文件到tar归档
     */
    private static void addFilesToTar(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos, File dir, Path basePath) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String entryName = basePath.relativize(file.toPath()).toString().replace('\\', '/');
                
                if (file.isDirectory()) {
                    // 添加目录条目
                    TarArchiveEntry entry = new TarArchiveEntry(file, entryName + "/");
                    taos.putArchiveEntry(entry);
                    taos.closeArchiveEntry();
                    
                    // 递归添加子目录
                    addFilesToTar(taos, file, basePath);
                } else {
                    // 添加文件条目
                    TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
                    taos.putArchiveEntry(entry);
                    
                    try (FileInputStream fis = new FileInputStream(file)) {
                        IOUtils.copy(fis, taos);
                    }
                    
                    taos.closeArchiveEntry();
                }
            }
        }
    }
} 