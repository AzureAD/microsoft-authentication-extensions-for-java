// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.mac;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileAccessor;
import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of CacheAccessor based on KeyChain for Mac
 */
public class KeyChainAccessor implements ICacheAccessor {
    private String cacheFilePath;
    private byte[] serviceNameBytes;
    private byte[] accountNameBytes;

    public KeyChainAccessor(String cacheFilePath, String serviceName, String accountName) {
        this.cacheFilePath = cacheFilePath;
        serviceNameBytes = serviceName.getBytes(StandardCharsets.UTF_8);
        accountNameBytes = accountName.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] read() {
        int[] dataLength = new int[1];
        Pointer[] data = new Pointer[1];

        try {
            int status = ISecurityLibrary.library.SecKeychainFindGenericPassword
                    (null,
                            serviceNameBytes.length, serviceNameBytes,
                            accountNameBytes.length, accountNameBytes,
                            dataLength, data,
                            null);

            if (status == ISecurityLibrary.ERR_SEC_ITEM_NOT_FOUND) {
                return null;
            }

            if (status != ISecurityLibrary.ERR_SEC_SUCCESS) {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

            return data[0].getByteArray(0, dataLength[0]);
        } finally {
            ISecurityLibrary.library.SecKeychainItemFreeContent(null, data[0]);
        }
    }

    @Override
    public void write(byte[] data) throws IOException {
        Pointer[] itemRef = new Pointer[1];
        int status;

        try {
            status = ISecurityLibrary.library.SecKeychainFindGenericPassword(
                    null,
                    serviceNameBytes.length, serviceNameBytes,
                    accountNameBytes.length, accountNameBytes,
                    null, null, itemRef);

            if (status != ISecurityLibrary.ERR_SEC_SUCCESS
                    && status != ISecurityLibrary.ERR_SEC_ITEM_NOT_FOUND) {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

            if (itemRef[0] != null) {
                status = ISecurityLibrary.library.SecKeychainItemModifyContent(
                        itemRef[0], null, data.length, data);
            } else {
                status = ISecurityLibrary.library.SecKeychainAddGenericPassword(
                        Pointer.NULL,
                        serviceNameBytes.length, serviceNameBytes,
                        accountNameBytes.length, accountNameBytes,
                        data.length, data, null);
            }

            if (status != ISecurityLibrary.ERR_SEC_SUCCESS) {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

            new CacheFileAccessor(cacheFilePath).updateCacheFileLastModifiedTimeByWritingDummyData();

        } finally {
            if (itemRef[0] != null) {
                ISecurityLibrary.library.CFRelease(itemRef[0]);
            }
        }
    }

    @Override
    public void delete() throws IOException {
        Pointer[] itemRef = new Pointer[1];
        try {
            int status = ISecurityLibrary.library.SecKeychainFindGenericPassword(
                    null,
                    serviceNameBytes.length, serviceNameBytes,
                    accountNameBytes.length, accountNameBytes,
                    null, null,
                    itemRef);

            if (status == ISecurityLibrary.ERR_SEC_ITEM_NOT_FOUND) {
                return;
            }

            if (status != ISecurityLibrary.ERR_SEC_SUCCESS) {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

            if (itemRef[0] != null) {
                status = ISecurityLibrary.library.SecKeychainItemDelete(itemRef[0]);

                if (status != ISecurityLibrary.ERR_SEC_SUCCESS) {
                    throw new KeyChainAccessException(convertErrorCodeToMessage(status));
                }
            }
            new CacheFileAccessor(cacheFilePath).updateCacheFileLastModifiedTimeByWritingDummyData();
        } finally {
            if (itemRef[0] != null) {
                ISecurityLibrary.library.CFRelease(itemRef[0]);
            }
        }
    }

    private String convertErrorCodeToMessage(int errorCode) {
        Pointer msgPtr = null;
        try {
            msgPtr = ISecurityLibrary.library.SecCopyErrorMessageString(errorCode, null);
            if (msgPtr == null) {
                return null;
            }

            int bufSize = ISecurityLibrary.library.CFStringGetLength(msgPtr);
            char[] buf = new char[bufSize];

            for (int i = 0; i < buf.length; i++) {
                buf[i] = ISecurityLibrary.library.CFStringGetCharacterAtIndex(msgPtr, i);
            }
            return new String(buf);
        } finally {
            if (msgPtr != null) {
                ISecurityLibrary.library.CFRelease(msgPtr);
            }
        }
    }
}
