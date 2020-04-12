// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainAccessor;
import com.sun.jna.Platform;
import org.junit.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CacheLockTest {

    public static final String TEST_MSAL_SERVICE = "testMsalService";
    public static final String TEST_MSAL_ACCOUNT = "testMsalAccount";
    private static String folder;
    private static String testFilePath;
    private static String lockFilePath;

    private static String testClassesPath;

    private static String lockHoldingIntervalsFilePath;

    @BeforeClass
    public static void setup() {
        // get proper file paths
        String currDir = System.getProperty("user.dir");
        String home = System.getProperty("user.home");

        java.nio.file.Path classes = java.nio.file.Paths.get(currDir, "target", "classes");
        testClassesPath = java.nio.file.Paths.get(currDir, "target", "test-classes").toString();

        testFilePath = java.nio.file.Paths.get(home, "test.txt").toString();
        lockFilePath = java.nio.file.Paths.get(home, "testLock.lockfile").toString();

        lockHoldingIntervalsFilePath = java.nio.file.Paths.get(home, "lockHoldingIntervals.txt").toString();

        String delimiter = ":";
        if (Platform.isWindows()) {
            delimiter = ";";
        }
        folder = classes.toString() + delimiter + testClassesPath;
    }

/*    @Test
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

        validateResultInFile(NUM_OF_THREADS);
    }*/


    @Test
    public void multipleThreadsWritingToFile() throws IOException, InterruptedException {
        int NUM_OF_THREADS = 200;

        new File(testFilePath).delete();
        new File(lockHoldingIntervalsFilePath).delete();

        List<Thread> writersThreads = new ArrayList<>();

        for (int i = 0; i < NUM_OF_THREADS; i++) {
            CacheFileWriterRunnable cacheFileWriterRunnable =
                    new CacheFileWriterRunnable("Thread_" + i,
                            lockFilePath, testFilePath, lockHoldingIntervalsFilePath);

            Thread t = new Thread(cacheFileWriterRunnable);
            t.start();
            writersThreads.add(t);
        }

        for (Thread t : writersThreads) {
            t.join();
        }
        validateLockUsageIntervals(NUM_OF_THREADS);
        validateResultInFile(NUM_OF_THREADS);
    }

    @Test
    public void multipleProcessesWritingToFile() throws IOException, InterruptedException {
        int NUM_OF_PROCESSES = 20;
        // make sure tester.json file doesn't already exist
        new File(testFilePath).delete();
        new File(lockHoldingIntervalsFilePath).delete();

        String mainWriterClass = com.microsoft.aad.msal4jextensions.CacheFileWriter.class.getName();

        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < NUM_OF_PROCESSES; i++) {

            String[] command =
                    new String[]{"java", "-cp", folder, mainWriterClass, "Process # " + i,
                            lockFilePath, testFilePath, lockHoldingIntervalsFilePath};

/*            String mvnArgs = ("Process_#_" + i) + " " +
                    lockFilePath + " " +
                    testFilePath + " " +
                    TEST_MSAL_SERVICE + " " +
                    TEST_MSAL_ACCOUNT;

            String[] mvnCommand =
                    new String[]{"mvn", "exec:java",
                            "-Dexec.mainClass=" + mainWriterClass,
                            "-Dexec.classpathScope=test",
                            "-Dexec.args=" + mvnArgs};*/

            Process process = new ProcessBuilder(command).inheritIO().start();
            processes.add(process);
        }

        for (Process process : processes) {
            waitForProcess(process);
        }

        validateLockUsageIntervals(NUM_OF_PROCESSES);
        validateResultInFile(NUM_OF_PROCESSES);
    }

    private String readFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return  new String(bytes, StandardCharsets.UTF_8);
    }

    private void validateLockUsageIntervals(int expected_size) throws IOException {
        List<Long[]> list = new ArrayList<>();
        String data = readFile(lockHoldingIntervalsFilePath);

        for (String line : data.split("\\r?\\n")) {
            String[] split = line.split("-");
            list.add(new Long[]{Long.parseLong(split[0]), Long.parseLong(split[1])});
        }

        Assert.assertEquals(expected_size, list.size());

        Collections.sort(list, (a, b) -> Long.compare(a[0], b[0]));

        Long[] prev = null;
        for(Long[] interval : list){
            Assert.assertTrue(interval[0] <= interval[1]);
            if(prev != null){
                if(interval[0] < prev[1]){
                    System.out.println("lock acquisition intersection detected");
                    //Assert.fail();
                }
            }
            prev = interval;
        }
    }

    @Test
    public void multipleThreadsWritingToKeyChain() throws IOException, InterruptedException {
        int NUM_OF_THREADS = 100;

        KeyChainAccessor keyChainAccessor =
                new KeyChainAccessor(testFilePath, TEST_MSAL_SERVICE, TEST_MSAL_ACCOUNT);
        keyChainAccessor.delete();

        List<Thread> writersThreads = new ArrayList<>();

        for (int i = 0; i < NUM_OF_THREADS; i++) {
            KeyChainWriterRunnable cacheFileWriterRunnable =
                    new KeyChainWriterRunnable
                            ("Thread_" + i, lockFilePath, testFilePath,
                                    TEST_MSAL_SERVICE, TEST_MSAL_ACCOUNT);

            Thread t = new Thread(cacheFileWriterRunnable);
            t.start();
            writersThreads.add(t);
        }

        for (Thread t : writersThreads) {
            t.join();
        }
        validateResultInKeyChain(keyChainAccessor, NUM_OF_THREADS);
    }


    @Test
    public void multipleProcessesWritingToKeyChain() throws IOException, InterruptedException {
        int NUM_OF_PROCESSES = 15;

        KeyChainAccessor keyChainAccessor =
                new KeyChainAccessor(testFilePath, TEST_MSAL_SERVICE, TEST_MSAL_ACCOUNT);
        keyChainAccessor.delete();

        String mainWriterClass = com.microsoft.aad.msal4jextensions.KeyChainWriter.class.getName();

        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < NUM_OF_PROCESSES; i++) {

            String mvnArgs = ("Process_#_" + i) + " " +
                    lockFilePath + " " +
                    testFilePath + " " +
                    TEST_MSAL_SERVICE + " " +
                    TEST_MSAL_ACCOUNT;

            String[] mvnCommand =
                    new String[]{"mvn", "exec:java",
                            "-Dexec.mainClass=" + mainWriterClass,
                            "-Dexec.classpathScope=test",
                            "-Dexec.args=" + mvnArgs};

            try {
                Process process = new ProcessBuilder(mvnCommand).inheritIO().start();
                processes.add(process);
            }
            catch (Throwable e){
                System.out.println(e.getMessage());
            }
        }

        for (Process process : processes) {
            try {
                waitForProcess(process);
            }
            catch (Throwable e){
                System.out.println(e.getMessage());
            }
        }

        validateResultInKeyChain(keyChainAccessor, NUM_OF_PROCESSES);
    }

    private void validateResultInFile(int expectedNum) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(testFilePath));
        validateResult(new String(bytes, StandardCharsets.UTF_8), expectedNum);
    }

    private void validateResultInKeyChain(KeyChainAccessor keyChainAccessor, int expectedNum) throws IOException {
        validateResult(new String(keyChainAccessor.read(), StandardCharsets.UTF_8), expectedNum);
    }

    private void validateResult(String data, int expectedNum) throws IOException {
        System.out.println("DATA TO VALIDATE: ");
        System.out.println(data);

        String prevTag = null;
        String prevProcId = null;
        int count = 0;

        for (String line : data.split("\\r?\\n")) {

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
                    count++;
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
        if (!">".equals(prevTag)) {
            Assert.fail("Unexpected Token");
        }
        Assert.assertEquals(expectedNum, count);
    }

    private void waitForProcess(Process process) throws InterruptedException {
        if (process.waitFor() != 0) {
            throw new RuntimeException(new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n")));
        }
    }
}
