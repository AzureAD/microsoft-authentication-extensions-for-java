// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.sun.jna.Platform;
import org.junit.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CacheLockTest {

    private static String folder;
    private static String testFilePath;
    private static String lockFilePath;

    @BeforeClass
    public static void setup() {
        // get proper file paths
        String currDir = System.getProperty("user.dir");
        String home = System.getProperty("user.home");

        java.nio.file.Path classes = java.nio.file.Paths.get(currDir, "target", "classes");
        java.nio.file.Path tests = java.nio.file.Paths.get(currDir, "target", "test-classes");

        testFilePath = java.nio.file.Paths.get(home, "test.txt").toString();
        lockFilePath = java.nio.file.Paths.get(home, "testLock.lockfile").toString();

        String delimiter = ":";
        if (Platform.isWindows()) {
            delimiter = ";";
        }
        folder = classes.toString() + delimiter + tests;
    }

    @Test
    public void tenThreadsWritingToFile_notSharedLock() throws IOException, InterruptedException {
        int NUM_OF_THREADS = 10;

        File tester = new File(testFilePath);
        tester.delete();

        List<Thread> writersThreads = new ArrayList<>();
        for (int i = 0; i < NUM_OF_THREADS; i++) {
            CacheFileWriterRunnable cacheFileWriterRunnable =
                    new CacheFileWriterRunnable("Thread # " + i, lockFilePath, testFilePath);

            writersThreads.add(new Thread(cacheFileWriterRunnable));
        }

        for (Thread t : writersThreads) {
            t.start();
            t.join();
        }

        validateResult();
    }


    @Test
    public void tenThreadsWritingToFile_sharedLock() throws IOException, InterruptedException {
        int NUM_OF_THREADS = 10;

        File tester = new File(testFilePath);
        tester.delete();

        List<Thread> writersThreads = new ArrayList<>();
        CacheFileWriterRunnable cacheFileWriterRunnable =
                new CacheFileWriterRunnable("Thread # ", lockFilePath, testFilePath);

        for (int i = 0; i < NUM_OF_THREADS; i++) {
            writersThreads.add(new Thread(cacheFileWriterRunnable));
        }

        for (Thread t : writersThreads) {
            t.start();
            t.join();
        }

        validateResult();
    }

    // implementation of org/slf4j/LoggerFactory should be available in Path
    //@Test
    public void tenProcessesWritingToFile() throws IOException, InterruptedException {
        int NUM_OF_PROCESSES = 10;
        // make sure tester.json file doesn't already exist
        File tester = new File(testFilePath);
        tester.delete();

        String mainWriterClass = com.microsoft.aad.msal4jextensions.CacheFileWriter.class.getName();

        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < NUM_OF_PROCESSES; i++) {
            String[] command =
                    new String[]{"java", "-cp", folder, mainWriterClass, "Process # " + i, lockFilePath, testFilePath};

            Process process = new ProcessBuilder(command).inheritIO().start();
            processes.add(process);
        }

        for (Process process : processes) {
            waitForProcess(process);
        }

        validateResult();
    }

    private void validateResult() throws IOException {
        String prevTag = null;
        String prevProcId = null;

        try (BufferedReader br = new BufferedReader(new FileReader(new File(testFilePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(" ");
                String tag = tokens[0];
                String procId = tokens[1];
                switch (tag) {
                    case ("<"):
                        if ("<".equals(prevTag)) {
                            Assert.fail("Unexpected Token");
                        }
                        break;
                    case (">"):
                        if (!"<".equals(prevTag) || !prevProcId.equals(procId)) {
                            Assert.fail("Unexpected Token");
                        }
                        break;
                    default:
                        Assert.fail("Unexpected Token");
                }
                prevTag = tag;
                prevProcId = procId;
            }
        }
        if (!">".equals(prevTag)) {
            Assert.fail("Unexpected Token");
        }
    }

    private void waitForProcess(Process process) throws InterruptedException {
        if (process.waitFor() != 0) {
            throw new RuntimeException(new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n")));
        }
    }
}
