package me.drton.jmavsim;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

class FileUtils {

    public static String getLogFileName(final String directory, final String prefix, boolean append) throws IOException {
        File dir = new File(directory);
        // make sure directory exists
        if (!dir.exists()) {
               dir.mkdir();
        }
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
        // autosequence logfile names: format target_nnnn.log
        File[] logFiles = dir.listFiles(filter);
        int logSeq = 0;
        String highestFile = null;
        for (File file : logFiles) {
            String fName = file.getName();
            String[] parts = fName.split("_|\\.");
            if (parts.length == 3) {
                int seq = Integer.decode(parts[1]);
                if (seq >= logSeq) {
                    logSeq = seq + 1;
                    highestFile = fName;
                }
            }
        }
        if (append && (highestFile != null)) {
            return directory + File.separatorChar + highestFile;
        } else {
            return directory + File.separatorChar + prefix + "_" + logSeq + ".log";
        }
    }

}
