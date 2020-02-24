package fr.zeamateis.usefulsaves.util;

import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private final File outputZip;
    private final File sourceFolder;

    public String outputFileName;

    private List<String> fileList;

    public ZipUtils(String outputZip, String sourceFolder) {
        this.fileList = new ArrayList<String>();
        this.outputZip = new File(outputZip);
        this.sourceFolder = new File(sourceFolder);
    }

    public void zipIt(long serverTime) {
        byte[] buffer = new byte[1024];
        String source = sourceFolder.getName();
        FileOutputStream fos;
        ZipOutputStream zos = null;
        try {
            Date date = new Date(serverTime);
            DateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
            outputFileName = String.format("%s-%s.zip", outputZip, formatter.format(date));
            fos = new FileOutputStream(outputFileName);
            zos = new ZipOutputStream(fos);
            //Compression level
            zos.setLevel(UsefulSavesConfig.Common.backupCompression.get());

            FileInputStream in = null;

            for (String file : this.fileList) {
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(sourceFolder + File.separator + file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } finally {
                    in.close();
                }
            }
            zos.closeEntry();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void generateFileList(File node) {
        // add file only
        if (node.isFile()) {
            if (node.length() > 0)
                fileList.add(generateZipEntry(node.toString()));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
                generateFileList(new File(node, filename));
            }
        }
    }

    private String generateZipEntry(String file) {
        return file.substring(sourceFolder.getName().length() + 1, file.length());
    }

    public enum CompressionLevel {

    }
}