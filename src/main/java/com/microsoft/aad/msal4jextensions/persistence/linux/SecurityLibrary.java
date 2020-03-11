// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface SecurityLibrary extends Library {

    int SECRET_SCHEMA_ATTRIBUTE_STRING = 0;

    int SECRET_SCHEMA_DONT_MATCH_NAME = 2;

    int SECRET_SCHEMA_NONE = 0;

    SecurityLibrary library = Native.load("libsecret-1", SecurityLibrary.class);

    Pointer secret_schema_new(String name,
                              int flags,
                              String attribute1Key, int attribute1Type,
                              String attribute2Key, int attribute2Type,
                              Pointer end);

    int secret_password_store_sync(Pointer scheme,
                                   String collection,
                                   String label,
                                   String password,
                                   Pointer cancellable,
                                   Pointer[] error,
                                   String attribute1Key, String attribute1Value,
                                   String attribute2Key, String attribute2Value,
                                   Pointer end);

    String secret_password_lookup_sync(Pointer scheme,
                                       Pointer cancellable,
                                       Pointer[] error,
                                       String attribute1Key, String attribute1Value,
                                       String attribute2Key, String attribute2Value,
                                       Pointer end);

    int secret_password_clear_sync(Pointer scheme,
                                   Pointer cancellable,
                                   Pointer[] error,
                                   String attribute1Key, String attribute1Value,
                                   String attribute2Key, String attribute2Value,
                                   Pointer end);
}
