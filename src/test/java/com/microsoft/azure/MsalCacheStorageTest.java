// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.azure;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;


import static org.testng.AssertJUnit.assertTrue;

public class MsalCacheStorageTest {

    MsalCacheStorage storage;
    String cacheLocation;

    @BeforeTest
    public void setup() throws Exception {
        cacheLocation = java.nio.file.Paths.get(System.getProperty("user.home"), "test.cache").toString();
        storage = new MsalCacheStorage.Builder()
                .cacheLocation(cacheLocation)
                .lockfileLocation(cacheLocation + ".lockfile")
                .build();
    }

    @AfterTest
    public void cleanup() {
        storage.deleteCache();
    }

    @Test
    public void createsNewCacheTest() throws IOException {
        storage.createCache();
        File f = new File(cacheLocation);

        assertTrue(f.exists());

        storage.deleteCache();
    }

    @Test
    public void writesReadsCacheData() throws IOException {
        storage.createCache();
        File f = new File(cacheLocation);

        String testString = "hello world";

        storage.writeCache(testString.getBytes());
        String receivedString = new String(storage.readCache());

        Assert.assertEquals(receivedString, testString);

        storage.deleteCache();
    }

}
