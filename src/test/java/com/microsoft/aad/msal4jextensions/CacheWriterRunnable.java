package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;
import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingAccessor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

public class CacheWriterRunnable implements Runnable {
    ICacheAccessor cacheAccessor;

    CrossProcessCacheFileLock lock;

    String lockHoldingIntervalsFilePath;

    @Override
    public void run() {
        long start;
        long end;

        try {
            lock.writeLock();
            start = System.currentTimeMillis();

            String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            String id = jvmName + ":" + Thread.currentThread().getId();

            byte[] data = cacheAccessor.read();
            String strData = (data != null) ? new String(data, StandardCharsets.UTF_8) : "";
            strData += "< " + id + "\n";

            Random rand = new Random();
            Thread.sleep(rand.nextInt(100));

            strData += "> " + id + "\n";

            cacheAccessor.write(strData.getBytes(StandardCharsets.UTF_8));
            end = System.currentTimeMillis();

            try (FileOutputStream os = new FileOutputStream(lockHoldingIntervalsFilePath, true)) {
                os.write((start + "-" + end + "\n").getBytes());
            }

        } catch (Exception ex) {
            System.out.println("File write failure " + ex.getMessage());
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
