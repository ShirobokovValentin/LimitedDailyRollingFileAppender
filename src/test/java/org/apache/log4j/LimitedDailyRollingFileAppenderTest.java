package org.apache.log4j;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

public class LimitedDailyRollingFileAppenderTest {

    private static final String LOG_DIR = "test-logs";
    private static final String LOG_FILE = LOG_DIR + "/test-log";
    private static final String DATE_PATTERN = ".yyyy-MM-dd";
    private LimitedDailyRollingFileAppender appender;

    @Before
    public void setUp() {
        // Create test directory
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            assertTrue(logDir.mkdirs());
        }
    }

    @After
    public void tearDown() {

        // Close current file
        appender.close();

        // Clean up test files
        File logDir = new File(LOG_DIR);
        if (logDir.exists()) {
            File[] files = logDir.listFiles();
            if (files != null) {
                for (File file : files) {

                    assertTrue(file.getName(), file.delete());
                }
            }
            assertTrue(logDir.delete());
        }
    }

    @Test
    public void testLogRotationAndDeletion() throws Exception {

        // Simulate log files for 5 days
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN.substring(1));
        for (int i = 0; i < 5; i++) {
            String dateSuffix = sdf.format(new Date(System.currentTimeMillis() - (i * 24L * 60L * 60L * 1000L)));
            File logFile = new File(LOG_FILE + dateSuffix);
            FileWriter writer = null;
            try {
                writer = new FileWriter(logFile);
                writer.write("Log content for day " + i);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        // Set up appender
        appender = new LimitedDailyRollingFileAppender();
        appender.setFile(LOG_FILE);
        appender.setDatePattern(DATE_PATTERN);
        appender.setMaxBackupFiles(3);
        appender.setLayout(new PatternLayout("%d{ISO8601} [%t] %-5p %c - %m%n"));
        appender.activateOptions();

        // Trigger a rollover to clean old files
        appender.rollOver();

        // Verify that only the most recent 3 files are retained
        File logDir = new File(LOG_DIR);
        File[] files = logDir.listFiles();
        assertNotNull(files);
        assertEquals(3, files.length);
    }

}
