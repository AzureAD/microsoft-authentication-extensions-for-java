// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

import java.io.IOException;

public interface CacheIO {

    byte[] read() throws IOException;

    void write(byte[] data) throws IOException;

    void delete() throws IOException;
}
