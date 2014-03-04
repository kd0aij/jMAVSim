package me.drton.jmavsim;

import java.io.File;
import java.io.FileFilter;

class FileUtils {

    public static String getLogFileName(final String directory, final String prefix) {
        // autosequence logfile names: format target_nnnn.log
        File dir = new File(directory);
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                boolean status = false;
                if (file.isFile() && !file.isDirectory()) {
                    String filename = file.getName();
                    if (filename.endsWith(".log") && filename.startsWith(prefix)) {
                        status = true;
                    }
                }
                return status;
            }
        };
        File[] logFiles = dir.listFiles(filter);
        int logSeq = 0;
        for (File file : logFiles) {
            String fName = file.getName();
            String[] parts = fName.split("_|\\.");
            if (parts.length == 3) {
                int seq = Integer.decode(parts[1]);
                if (seq >= logSeq)
                    logSeq = seq + 1;
            }
        }
        return new String(directory + File.separatorChar + prefix + "_" + logSeq + ".log");
    }
}