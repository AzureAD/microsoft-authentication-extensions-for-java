// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Crypt32Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Implementation of CacheAccessor based on File persistence
 */
public class CacheFileAccessor implements ICacheAccessor {
    private String cacheFilePath;
    private File cacheFile;

    public CacheFileAccessor(String cacheFilePath) throws IOException {
        this.cacheFilePath = cacheFilePath;

        cacheFile = new File(cacheFilePath);
    }

    @Override
    public byte[] read() throws IOException {
        byte[] data = null;
        
        if(cacheFile.exists()) {
            data = Files.readAllBytes(cacheFile.toPath());

            if (data != null && data.length > 0 && Platform.isWindows()) {
                data = Crypt32Util.cryptUnprotectData(data);
            }
        }

        return data;
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (Platform.isWindows()) {
            data = Crypt32Util.cryptProtectData(data);
        }

        try (FileOutputStream stream = new FileOutputStream(cacheFilePath)) {
            stream.write(data);
        }
    }

    @Override
    public void delete() throws IOException {
        Files.deleteIfExists(new File(cacheFilePath).toPath());
    }

    public void updateCacheFileLastModifiedTimeByWritingDummyData() throws IOException {
        write(new byte[1]);
    }
}
