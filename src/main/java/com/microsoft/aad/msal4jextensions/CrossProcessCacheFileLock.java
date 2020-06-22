// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross process lock based on OS level file lock.
 */
class CrossProcessCacheFileLock {
    enum LockMode {
        SHARED,
        EXCLUSIVE
    }

    private final static Logger LOG = LoggerFactory.getLogger(CrossProcessCacheFileLock.class);

    private int retryDelayMilliseconds;
    private int retryNumber;

    private File lockFile;

    private LockMode lockMode;

    private FileLock lock;

    private FileChannel fileChannel;

    /**
     * Constructor
     *
     * @param lockfileName           Path of the lock file
     * @param retryDelayMilliseconds Delay between lock acquisition attempts in ms
     * @param retryNumber            Number of attempts to acquire lock
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
     * @throws CacheFileLockAcquisitionException if failed to acquire lock
     */
    void readLock() throws CacheFileLockAcquisitionException {
        lock(LockMode.SHARED);
    }

    /**
     * Acquire write lock - exclusive access
     *
     * @throws CacheFileLockAcquisitionException if failed to acquire lock
     */
    void writeLock() throws CacheFileLockAcquisitionException {
        lock(LockMode.EXCLUSIVE);
    }

    private String getProcessId() {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();

        return vmName.substring(0, vmName.indexOf("@"));
    }

    private String getLockProcessThreadId() {
        return "pid:" + getProcessId() + " thread:" + Thread.currentThread().getId();
    }

    /**
     * Tries to acquire OS lock for lock file
     * Retries {@link #retryNumber} times with {@link #retryDelayMilliseconds} delay
     *
     * @throws CacheFileLockAcquisitionException if the lock was not obtained.
     */
    private void lock(LockMode mode) throws CacheFileLockAcquisitionException {

        for (int tryCount = 0; tryCount < retryNumber; tryCount++) {
            try {
                LOG.debug(getLockProcessThreadId() + " acquiring " + mode + " file lock");

                fileChannel = FileChannel.open(lockFile.toPath(),
                        StandardOpenOption.READ,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.DELETE_ON_CLOSE,
                        StandardOpenOption.SYNC,
                        StandardOpenOption.WRITE);

                lock = fileChannel.tryLock(0L, Long.MAX_VALUE, LockMode.SHARED == mode);
                if (lock == null) {
                    throw new IllegalStateException("Lock is not available");
                }

                lockMode = lock.isShared() ? LockMode.SHARED : LockMode.EXCLUSIVE;

                String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();

                // for debugging purpose write jvm name to lock file
                ByteBuffer buff = ByteBuffer.wrap(jvmName.replace("@", " ").
                        getBytes(StandardCharsets.UTF_8));
                fileChannel.write(buff);

                LOG.debug(getLockProcessThreadId() + " acquired file lock, isShared - " + lock.isShared());
                return;
            } catch (Exception ex) {
                LOG.debug(getLockProcessThreadId() + " failed to acquire " + mode + " lock," +
                        " exception msg - " + ex.getMessage());
                try {
                    releaseResources();
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }

                try {
                    Thread.sleep(retryDelayMilliseconds);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
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
        LOG.debug(getLockProcessThreadId() + " releasing " + lockMode + " lock");

        releaseResources();
    }

    private void releaseResources() throws IOException {
        if (lock != null) {
            lock.release();
        }
        if (fileChannel != null) {
            fileChannel.close();
        }
    }
}