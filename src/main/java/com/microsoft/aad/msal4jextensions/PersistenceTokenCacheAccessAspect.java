// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4j.ITokenCacheAccessAspect;
import com.microsoft.aad.msal4j.ITokenCacheAccessContext;
import com.microsoft.aad.msal4jextensions.persistence.CacheFileIO;
import com.microsoft.aad.msal4jextensions.persistence.CacheIO;
import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingAccessException;
import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingIO;
import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainIO;
import com.nimbusds.jose.util.StandardCharset;
import com.sun.jna.Platform;

import java.io.File;
import java.io.IOException;

public class PersistenceTokenCacheAccessAspect implements ITokenCacheAccessAspect {

    private CacheFileLock lock;
    private Long lastSeenCacheFileModifiedTimestamp;
    private CacheIO cacheIO;

    private PersistenceParameters parameters;

    public PersistenceTokenCacheAccessAspect(PersistenceParameters persistenceParameters) {
        this.parameters = persistenceParameters;

        lock = new CacheFileLock(parameters.getCacheLockFilePath());

        if (Platform.isMac()) {
            cacheIO = new KeyChainIO(
                    parameters.getCacheFilePath(), parameters.getKeychainService(), parameters.getKeychainAccount());
        } else if (Platform.isWindows()) {
            cacheIO = new CacheFileIO(parameters.getCacheFilePath());
        } else if (Platform.isLinux()) {
            try {
                cacheIO = new KeyRingIO(parameters.getCacheFilePath(),
                        parameters.getKeyringCollection(),
                        parameters.getKeyringSchemaName(),
                        parameters.getKeyringSecretLabel(),
                        parameters.getKeyringAttributeKey1(),
                        parameters.getKeyringAttributeValue1(),
                        parameters.getKeyringAttributeKey2(),
                        parameters.getKeyringAttributeValue2());

                ((KeyRingIO) cacheIO).verify();
            } catch (KeyRingAccessException ex) {
                cacheIO = new CacheFileIO(persistenceParameters.getCacheFilePath());
            }
        }
    }

    private boolean isWriteAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        return iTokenCacheAccessContext.hasCacheChanged();
    }

    private boolean isReadAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        return !isWriteAccess(iTokenCacheAccessContext);
    }

    private void updateLastSeenCacheFileModifiedTimestamp() throws IOException {
        lastSeenCacheFileModifiedTimestamp = getCurrentCacheFileModifiedTimestamp();
    }

    public Long getCurrentCacheFileModifiedTimestamp() {
        return new File(parameters.getCacheFilePath()).lastModified();
    }

    @Override
    public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        try {
            if (isWriteAccess(iTokenCacheAccessContext)) {
                lock.writeLock();
            } else {
                Long currentCacheFileModifiedTimestamp = getCurrentCacheFileModifiedTimestamp();
                if (currentCacheFileModifiedTimestamp != null &&
                        currentCacheFileModifiedTimestamp == lastSeenCacheFileModifiedTimestamp) {
                    return;
                } else {
                    lock.readLock();
                }
            }
            byte[] data = cacheIO.read();
            iTokenCacheAccessContext.tokenCache().deserialize(new String(data, StandardCharset.UTF_8));

            updateLastSeenCacheFileModifiedTimestamp();

            if (isReadAccess(iTokenCacheAccessContext)) {
                lock.unlock();
            }
        } catch (IOException ex) {
        }
    }

    @Override
    public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        if (isWriteAccess(iTokenCacheAccessContext)) {
            try {
                cacheIO.write(iTokenCacheAccessContext.tokenCache().serialize().getBytes());

                updateLastSeenCacheFileModifiedTimestamp();
                lock.unlock();
            } catch (IOException ex) {
            }
        }
    }
}
