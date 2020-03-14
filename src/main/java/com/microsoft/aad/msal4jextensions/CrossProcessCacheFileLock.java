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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross process lock based on OS level file lock.
 */
class CrossProcessCacheFileLock {

    private final static Logger LOG = LoggerFactory.getLogger(CrossProcessCacheFileLock.class);
    public static final String READ_MODE = "r";
    public static final String WRITE_MODE = "rw";

    private int retryDelayMilliseconds;
    private int retryNumber;

    private File lockFile;
    private RandomAccessFile randomAccessFile;

    private String mode;

    private FileLock lock;

    /**
     * Constructor
     *
     * @param lockfileName          Path of the lock file
     * @param retryDelayMilliseconds Delay between lock acquisition attempts in ms
     * @param retryNumber           Number of attempts to acquire lock
     */
    CrossProcessCacheFileLock(String lockfileName, int retryDelayMilliseconds, int retryNumber) {
        lockFile = new File(lockfileName);
        lockFile.deleteOnExit();

        this.retryDelayMilliseconds = retryDelayMilliseconds;
        this.retryNumber = retryNumber;
    }

    /**
     * Acquire read lock - can be shared by multiple readers
     *
     * @throws CacheFileLockAcquisitionException
     */
    void readLock() throws CacheFileLockAcquisitionException {
        lock(READ_MODE);
    }

    /**
     * Acquire write lock - exclusive access
     *
     * @throws CacheFileLockAcquisitionException
     */
    void writeLock() throws CacheFileLockAcquisitionException {
        lock(WRITE_MODE);
    }

    private String getLockProcessThreadId(){
        return "pid:" + ProcessHandle.current().pid() + " thread:" + Thread.currentThread().getId();
    }

    /**
     * Tries to acquire OS lock for lock file
     * Retries {@link #retryNumber} times with {@link #retryDelayMilliseconds} delay
     *
     * @throws CacheFileLockAcquisitionException if the lock was not obtained.
     */
    private void lock(String mode) throws CacheFileLockAcquisitionException {
        for (int tryCount = 0; tryCount < retryNumber; tryCount++) {
            try {
                lockFile.createNewFile();

                LOG.debug(getLockProcessThreadId() + " acquiring " + mode + " file lock");

                randomAccessFile = new RandomAccessFile(lockFile, mode);
                FileChannel channel = randomAccessFile.getChannel();

                boolean isShared = READ_MODE.equals(mode);
                lock = channel.lock(0L, Long.MAX_VALUE, isShared);

                this.mode = mode;

                if (!lock.isShared()) {
                    String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();

                    // for debugging purpose
                    // if exclusive lock acquired, write jvm name to lock file
                    ByteBuffer buff = ByteBuffer.wrap(jvmName.replace("@", " ").
                            getBytes(StandardCharsets.UTF_8));
                    channel.write(buff);
                }
                LOG.debug(getLockProcessThreadId() + " acquired file lock, isShared - " + lock.isShared());
                return;
            } catch (Exception ex) {
                LOG.debug(getLockProcessThreadId() + " failed to acquire " + mode + " lock," +
                        " exception msg - " + ex.getMessage());

                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(retryDelayMilliseconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        LOG.error(getLockProcessThreadId() + " failed to acquire " + mode + " lock");

        throw new CacheFileLockAcquisitionException(
                getLockProcessThreadId() + " failed to acquire " + mode + " lock");
    }

    /**
     * Release OS lock for lockFile
     *
     * @throws IOException
     */
    void unlock() throws IOException {
        if (randomAccessFile != null) {
            LOG.debug(getLockProcessThreadId() + " releasing " + mode + " lock");

            if(lock != null){
                lock.release();
            }
            randomAccessFile.close();
        }
    }
}