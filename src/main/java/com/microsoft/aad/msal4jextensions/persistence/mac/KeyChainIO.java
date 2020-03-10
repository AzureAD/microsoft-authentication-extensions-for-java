// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.mac;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileIO;
import com.microsoft.aad.msal4jextensions.persistence.CacheIO;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KeyChainIO implements CacheIO {
    private String cacheFilePath;
    private byte[] serviceNameBytes;
    private byte[] accountNameBytes;

    public KeyChainIO(String cacheFilePath, String serviceName, String accountName) {
        this.cacheFilePath = cacheFilePath;
        serviceNameBytes = serviceName.getBytes(StandardCharsets.UTF_8);
        accountNameBytes = accountName.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] read() {

        int[] dataLength = new int[1];
        Pointer[] data = new Pointer[1];

        int status = SecurityLibrary.library.SecKeychainFindGenericPassword
                (null,
                        serviceNameBytes.length, serviceNameBytes,
                        accountNameBytes.length, accountNameBytes,
                        dataLength, data,
                        null);

        if (status != SecurityLibrary.ERR_SEC_SUCCESS) {
            throw new KeyChainAccessException(convertErrorCodeToMessage(status));
        }

        byte[] result = data[0].getByteArray(0, dataLength[0]);

        SecurityLibrary.library.SecKeychainItemFreeContent(null, data[0]);

        return result;
    }

    @Override
    public void write(byte[] data) {

        Pointer[] itemRef = new Pointer[1];

        int status = SecurityLibrary.library.SecKeychainFindGenericPassword(
                null,
                serviceNameBytes.length, serviceNameBytes,
                accountNameBytes.length, accountNameBytes,
                null, null, itemRef);

        if (status != SecurityLibrary.ERR_SEC_SUCCESS
                && status != SecurityLibrary.ERR_SEC_ITEM_NOT_FOUND) {
            throw new KeyChainAccessException(convertErrorCodeToMessage(status));
        }

        if (itemRef[0] != null) {
            status = SecurityLibrary.library.SecKeychainItemModifyContent(
                    itemRef[0], null, data.length, data);

            SecurityLibrary.library.CFRelease(itemRef[0]);
        } else {
            status = SecurityLibrary.library.SecKeychainAddGenericPassword(
                    Pointer.NULL,
                    serviceNameBytes.length, serviceNameBytes,
                    accountNameBytes.length, accountNameBytes,
                    data.length, data, null);
        }

        if (status != SecurityLibrary.ERR_SEC_SUCCESS) {
            throw new KeyChainAccessException(convertErrorCodeToMessage(status));
        }

        new CacheFileIO(cacheFilePath).modifyCacheFile();
    }

    @Override
    public void delete() {
        Pointer[] itemRef = new Pointer[1];

        SecurityLibrary.library.SecKeychainFindGenericPassword(
                null,
                serviceNameBytes.length, serviceNameBytes,
                accountNameBytes.length, accountNameBytes,
                null, null,
                itemRef);

        if (itemRef[0] != null) {
            int status = SecurityLibrary.library.SecKeychainItemDelete(itemRef[0]);

            if (status != SecurityLibrary.ERR_SEC_SUCCESS) {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

            SecurityLibrary.library.CFRelease(itemRef[0]);
        }

        new CacheFileIO(cacheFilePath).modifyCacheFile();
    }

    private String convertErrorCodeToMessage(int errorCode) {
        Pointer msgPtr = SecurityLibrary.library.SecCopyErrorMessageString(errorCode, null);
        if (msgPtr == null) {
            return null;
        }

        int bufSize = SecurityLibrary.library.CFStringGetLength(msgPtr);
        char[] buf = new char[bufSize];

        for (int i = 0; i < buf.length; i++) {
            buf[i] = SecurityLibrary.library.CFStringGetCharacterAtIndex(msgPtr, i);
        }

        SecurityLibrary.library.CFRelease(msgPtr);

        return new String(buf);
    }
}
