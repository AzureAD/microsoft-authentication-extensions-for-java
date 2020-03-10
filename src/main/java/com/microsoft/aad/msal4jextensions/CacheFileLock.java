// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

/**
 * Cache lock for the persistent shared MSAL token cache
 * <p>
 * Needed to maintain the integrity of the cache if multiple processes are trying to access it at the same time.
 */
public class CacheFileLock {

    private int lockfileRetryWait = 100;
    private int lockfileRetryCount = 60000 / lockfileRetryWait;

    private File lockFile;
    private RandomAccessFile randomAccessFile;

    private String mode;

    /**
     * Default constructor to be used to initialize CacheLock
     *
     * @param lockfileName path of the lock file to be used
     */
    public CacheFileLock(String lockfileName) {
        lockFile = new File(lockfileName);
        lockFile.deleteOnExit();
    }

    public void readLock() throws CacheLockNotObtainedException {
        lock("r");
    }

    public void writeLock() throws CacheLockNotObtainedException {
        lock("rw");
    }

    /**
     * Tries to acquire a lock on the provided lockFile
     * If it cannot be obtained right away, it retries lockfileRetryCount = 60000 / lockfileRetryWait times
     *
     * @throws CacheLockNotObtainedException if the lock cannot be obtained after all these tries.
     */
    public void lock(String mode) throws CacheLockNotObtainedException {
        this.mode = mode;
        for (int tryCount = 0; tryCount < lockfileRetryCount; tryCount++) {
            try {
                lockFile.createNewFile();

                randomAccessFile = new RandomAccessFile(lockFile, mode);
                FileChannel channel = randomAccessFile.getChannel();
                channel.lock();

                if ("rw".equals(mode)) {
                    String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();

                    ByteBuffer buff = ByteBuffer.wrap(jvmName.replace("@", " ").
                            getBytes(StandardCharsets.UTF_8));
                    channel.write(buff);
                }
                return;
            } catch (Exception ex) {
                System.out.println(ProcessHandle.current().pid() + "failed to " + mode + " acquire lock, ex - " + ex.getMessage());

                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(lockfileRetryWait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        throw new CacheLockNotObtainedException(
                ProcessHandle.current().pid() + " failed to acquire " + mode + " lock");
    }

    /**
     * Unlock the lockFile
     *
     * @return void
     */
    public void unlock() throws IOException {
        if (randomAccessFile != null) {
            System.out.println(ProcessHandle.current().pid() + "   releasing " + mode + " lock");
            randomAccessFile.close();
        }
    }
}