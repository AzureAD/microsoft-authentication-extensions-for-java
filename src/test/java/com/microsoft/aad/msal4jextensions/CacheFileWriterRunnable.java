// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class CacheFileWriterRunnable implements Runnable {
    String id;
    String lockFilePath;
    File file;

    CacheFileWriterRunnable(String id, String lockFilePath, String filePath) {
        this.id = id;
        this.lockFilePath = lockFilePath;
        this.file = new File(filePath);
    }

    public void run() {
        CacheFileLock lock = new CacheFileLock(lockFilePath);
        try {
            lock.writeLock();
            file.createNewFile();
            try (FileOutputStream os = new FileOutputStream(file, true)) {
                os.write(("< " + id + "\n").getBytes());
                Thread.sleep(1000);
                os.write(("> " + id + "\n").getBytes());
            }
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