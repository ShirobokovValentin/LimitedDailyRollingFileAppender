package org.apache.log4j;

import org.apache.log4j.helpers.LogLog;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LimitedDailyRollingFileAppender extends DailyRollingFileAppender {

    private int maxBackupFiles = -1;
    private final Queue<File> fileQueue = new ArrayDeque<File>();
    private boolean cleanerInUse = false;

    public void setMaxBackupFiles(int maxBackupFiles) {
        this.maxBackupFiles = maxBackupFiles;
    }

    public int getMaxBackupFiles() {
        return maxBackupFiles;
    }

    @Override
    public void activateOptions() {
        super.activateOptions();
        this.cleanerInUse = fileName != null && getDatePattern() != null && maxBackupFiles > 0;
        if (this.cleanerInUse) {
            initializeFileQueue();
        }
    }

    private void initializeFileQueue() {
        File logFile = new File(fileName);
        File parentDir = logFile.getParentFile();
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            return;
        }

        // Collect and sort log files matching the appender pattern
        File[] allFiles = parentDir.listFiles();
        if (allFiles == null) {
            return;
        }

        String baseFileName = logFile.getName();
        List<File> matchingFiles = new ArrayList<File>();
        for (File file : allFiles) {
            String currentFileName = file.getName();
            if (currentFileName.startsWith(baseFileName) && currentFileName.length() > baseFileName.length()) {
                matchingFiles.add(file);
            }
        }

        Collections.sort(matchingFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long diff = f1.lastModified() - f2.lastModified();
                return (diff < 0) ? -1 : ((diff > 0) ? 1 : 0);
            }
        });

        fileQueue.addAll(matchingFiles);
        deleteOldFilesFromQueue();
    }

    @Override
    void rollOver() throws IOException {
        super.rollOver();
        if (this.cleanerInUse) {
            manageOldFiles();
        }
    }

    private void manageOldFiles() {
        String scheduledFileName = fileName + sdf.format(now);
        File scheduledFile = new File(scheduledFileName);
        fileQueue.add(scheduledFile);
        deleteOldFilesFromQueue();
    }

    private void deleteOldFilesFromQueue() {
        while (fileQueue.size() > maxBackupFiles) {
            File oldestFile = fileQueue.poll();
            if (oldestFile == null || !oldestFile.exists()) {
                return;
            }
            try {
                if (oldestFile.delete()) {
                    LogLog.debug("Deleted old log file: " + oldestFile.getAbsolutePath());
                } else {
                    LogLog.warn("Failed to delete old log file: " + oldestFile.getAbsolutePath());
                }
            } catch (Exception e) {
                LogLog.error("Error while deleting old log file: " + oldestFile.getAbsolutePath(), e);
            }
        }
    }

}
