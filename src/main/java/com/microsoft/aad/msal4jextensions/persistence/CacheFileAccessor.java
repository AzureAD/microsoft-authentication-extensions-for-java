// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Crypt32Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

/**
 * Implementation of CacheAccessor based on File persistence
 */
public class CacheFileAccessor implements CacheAccessor {
    String cacheFilePath;

    public CacheFileAccessor(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
    }

    @Override
    public byte[] read() throws IOException {
        File cacheFile = new File(cacheFilePath);
        cacheFile.createNewFile();

        byte[] data = Files.readAllBytes(cacheFile.toPath());

        if (Platform.isWindows()) {
            data = Crypt32Util.cryptUnprotectData(data);
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

    public void updateCacheFileLastModifiedTime() throws IOException {
        FileTime fileTime = FileTime.fromMillis(System.currentTimeMillis());

        Files.setLastModifiedTime(Paths.get(cacheFilePath), fileTime);
    }
}
