package es.gavab.ideconfigurator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnZip {
	
    private static final int BUFFER = 2048;

    private UnZip() {
    }
    
    public static void extractFiles(String zipFilePath, String destDirPath) {
    	ZipFile zipfile;
        try {
            File destDirFile = new File(destDirPath);
            if ((destDirFile.isDirectory()) && (!destDirFile.exists())) {
                destDirFile.mkdirs();
            }
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;

            zipfile = new ZipFile(zipFilePath);
            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.isDirectory()) {
                    File newDir = new File(destDirPath, entry.getName());
                    newDir.mkdirs();
                } else {
                    is = new BufferedInputStream(zipfile.getInputStream(entry));

                    byte[] data = new byte[2048];
                    FileOutputStream fos = new FileOutputStream(new File(destDirPath, entry.getName()));
                    dest = new BufferedOutputStream(fos, BUFFER);
                    int count;
                    while ((count = is.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    is.close();
                }
            }
            zipfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}