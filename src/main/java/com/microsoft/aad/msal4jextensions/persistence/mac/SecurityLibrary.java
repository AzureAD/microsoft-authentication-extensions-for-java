// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface SecurityLibrary extends Library {

    int ERR_SEC_SUCCESS = 0;
    int ERR_SEC_ITEM_NOT_FOUND = -25300;

    SecurityLibrary library = Native.load("Security", SecurityLibrary.class);

    int SecKeychainAddGenericPassword(
            Pointer keychain,
            int serviceNameLength,
            byte[] serviceName,
            int accountNameLength,
            byte[] accountName,
            int passwordLength,
            byte[] passwordData,
            Pointer itemRef
    );

    int SecKeychainItemModifyContent(
            Pointer itemRef,
            Pointer attrList,
            int length,
            byte[] data
    );

    int SecKeychainFindGenericPassword(
            Pointer keychainOrArray,
            int serviceNameLength,
            byte[] serviceName,
            int accountNameLength,
            byte[] accountName,
            int[] passwordLength,
            Pointer[] passwordData,
            Pointer[] itemRef
    );

    int SecKeychainItemDelete(
            Pointer itemRef
    );

    Pointer SecCopyErrorMessageString(
            int status,
            Pointer reserved);

    int CFStringGetLength(
            Pointer theString
    );

    char CFStringGetCharacterAtIndex(
            Pointer theString,
            long idx
    );

    void CFRelease(
            Pointer cf
    );

    int SecKeychainItemFreeContent(
            Pointer[] attrList,
            Pointer data);
}
