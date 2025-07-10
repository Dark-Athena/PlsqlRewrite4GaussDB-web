package com.plsqlrewriter.webapp.util;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.nio.file.*;

public class ZipUtil {
    public static void unzip(File zipFile, File destDir) throws IOException {
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new FileInputStream(zipFile))) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(outFile)) {
                        IOUtils.copy(zis, os);
                    }
                }
            }
        }
    }

    public static void zipDir(File dir, File zipFile) throws IOException {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(new FileOutputStream(zipFile))) {
            Path basePath = dir.toPath();
            Files.walk(basePath).filter(Files::isRegularFile).forEach(path -> {
                try {
                    String entryName = basePath.relativize(path).toString().replace("\\", "/");
                    zos.putArchiveEntry(new ZipArchiveEntry(entryName));
                    Files.copy(path, zos);
                    zos.closeArchiveEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            zos.finish();
        }
    }
} 