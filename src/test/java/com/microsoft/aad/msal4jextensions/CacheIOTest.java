// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileIO;
import com.microsoft.aad.msal4jextensions.persistence.CacheIO;
import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingIO;
import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainIO;
import com.sun.jna.Platform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CacheIOTest {

    String cacheFilePath;

    @Before
    public void init(){
        cacheFilePath = java.nio.file.Paths.get(System.getProperty("user.home"), "test.cache").toString();
    }

    @Test
    public void keyChainIOTest() throws IOException {
        if(!Platform.isMac()){
            return;
        }
        String keyChainAccount = "MSAL_Test_Account";
        String keyChainService = "MSAL_Test_Service";

        CacheIO cacheIO = new KeyChainIO(cacheFilePath, keyChainService, keyChainAccount);

        readWriteTest(cacheIO);
    }

    @Test
    public void cacheFileIOTest() throws IOException {

        CacheIO cacheIO  = new CacheFileIO(cacheFilePath);

        readWriteTest(cacheIO);
    }

    @Test
    public void keyRingIOTest() throws IOException {
        if(!Platform.isLinux()){
            return;
        }
        // default collection
        String keyringCollection = null;
        String keyringSchemaName = "TestSchemaName";
        String keyringSecretLabel = "TestSecretLabel";
        String attributeKey1 = "TestAttributeKey1";
        String attributeValue1 = "TestAttributeValue1";
        String attributeKey2 = "TestAttributeKey2";
        String attributeValue2 = "TestAttributeValue2";

        CacheIO cacheIO = new KeyRingIO(cacheFilePath,
                keyringCollection,
                keyringSchemaName,
                keyringSecretLabel,
                attributeKey1,
                attributeValue1,
                attributeKey2,
                attributeValue2);

        readWriteTest(cacheIO);
    }

    private void readWriteAssert(CacheIO cacheIO, String data) throws IOException {
        cacheIO.write(data.getBytes());
        String receivedString = new String(cacheIO.read());

        Assert.assertEquals(receivedString, data);
    }


    private void readWriteTest(CacheIO cacheIO) throws IOException {
        try {
            cacheIO.delete();

            readWriteAssert(cacheIO, "test data 1");
            readWriteAssert(cacheIO, "test data 2");
        } finally {
            cacheIO.delete();
        }
    }
}
