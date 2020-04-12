// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainAccessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class KeyChainWriterRunnable implements Runnable  {

    String id;
    String lockFilePath;
    File file;
    CrossProcessCacheFileLock lock;
    String serviceName;
    String accountName;

    KeyChainWriterRunnable
            (String id, String lockFilePath, String filePath, String serviceName, String accountName) {
        this.id = id;
        this.lockFilePath = lockFilePath;
        this.file = new File(filePath);

        this.serviceName = serviceName;
        this.accountName = accountName;

        lock = new CrossProcessCacheFileLock(lockFilePath, 150, 100);
    }

    public void run() {
        KeyChainAccessor keyChainAccessor =
                new KeyChainAccessor(file.getAbsolutePath(), serviceName, accountName);
        try {
            lock.writeLock();

            String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            String procId = jvmName + "_" + Thread.currentThread().getId();

            byte[] data = keyChainAccessor.read();
            String strData = (data != null) ? new String(data, StandardCharsets.UTF_8) : "";
            strData += "< " + procId + "\n";
            Random rand = new Random();
            Thread.sleep(rand.nextInt(100));
            strData += "> " + procId + "\n";

            keyChainAccessor.write(strData.getBytes(StandardCharsets.UTF_8));

        } catch (Throwable ex) {
            System.out.println("=======================================================> KeyChainWriterRunnable FAILURE");
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
