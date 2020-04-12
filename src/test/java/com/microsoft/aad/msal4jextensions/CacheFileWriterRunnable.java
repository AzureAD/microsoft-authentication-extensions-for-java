// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.Random;

class CacheFileWriterRunnable implements Runnable {
    String id;
    String lockFilePath;
    File file;
    String lockHoldingIntervalsFilePath;

    CrossProcessCacheFileLock lock;

    long start;
    long end;

    CacheFileWriterRunnable(String id, String lockFilePath, String filePath, String lockHoldingIntervalsFilePath) {
        this.id = id;
        this.lockFilePath = lockFilePath;
        this.file = new File(filePath);
        this.lockHoldingIntervalsFilePath = lockHoldingIntervalsFilePath;

        lock = new CrossProcessCacheFileLock(lockFilePath, 150, 100);
    }

    public void run() {
        try {
            lock.writeLock();
            start = System.currentTimeMillis();
            file.createNewFile();

            byte[] bytes = Files.readAllBytes(file.toPath());
            String data = new String(bytes, StandardCharsets.UTF_8);

            data += "< " + id + "\n";
            data += "> " + id + "\n";

            Random rand = new Random();
            Thread.sleep(rand.nextInt(100));

            Files.write(file.toPath(), data.getBytes());

            end = System.currentTimeMillis();

            try (FileOutputStream os = new FileOutputStream(lockHoldingIntervalsFilePath, true)) {
                os.write((start + "-" + end + "\n").getBytes());
            }

          //  Thread.sleep(500);

/*            try (FileOutputStream os = new FileOutputStream(file, true)) {
                Random rand = new Random();
                Thread.sleep(rand.nextInt(100));

                os.write(("< " + id + "\n").getBytes());

                os.write(("> " + id + "\n").getBytes());
            }*/
        } catch (IOException | InterruptedException ex) {
            System.out.println("File write failure");
            ex.printStackTrace();
        } finally {
            try {
                lock.unlock();
            } catch (IOException e) {
                System.out.println("Failed to unlock");
                e.printStackTrace();
            }
        }
    }
}