package es.gavab.ideconfigurator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnZip {

    static final int BUFFER = 2048;

    public void extractFiles(String zipFile, String destDir) {
        try {
            File destDirFile = new File(destDir);
            if ((destDirFile.isDirectory()) && (!destDirFile.exists())) {
                destDirFile.mkdirs();
            }
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;

            ZipFile zipfile = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.isDirectory()) {
                    File newDir = new File(destDir, entry.getName());
                    newDir.mkdirs();
                } else {
                    is = new BufferedInputStream(zipfile.getInputStream(entry));

                    byte[] data = new byte[2048];
                    FileOutputStream fos = new FileOutputStream(new File(destDir, entry.getName()));
                    dest = new BufferedOutputStream(fos, 2048);
                    int count;
                    while ((count = is.read(data, 0, 2048)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    is.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}