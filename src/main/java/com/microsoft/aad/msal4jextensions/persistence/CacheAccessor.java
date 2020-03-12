// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

import java.io.IOException;

/**
 * Interface for cache data access operations.
 */
public interface CacheAccessor {

    /**
     * Reads cache data
     *
     * @return Cache data
     * @throws IOException
     */
    byte[] read() throws IOException;

    /**
     * Writes cache data
     *
     * @param data cache data
     * @throws IOException
     */
    void write(byte[] data) throws IOException;

    /**
     * Deletes the cache
     *
     * @throws IOException
     */
    void delete() throws IOException;

}
