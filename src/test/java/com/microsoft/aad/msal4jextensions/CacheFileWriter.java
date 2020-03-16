// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

public class CacheFileWriter {

    public static void main(String[] args) throws Exception {
        String filePath;
        String lockFilePath;
        String executionId;

        executionId = args[0];
        lockFilePath = args[1];
        filePath = args[2];

        CacheFileWriterRunnable cacheFileWriterRunnable =
                new CacheFileWriterRunnable(executionId, lockFilePath, filePath);

        cacheFileWriterRunnable.run();
    }
}
