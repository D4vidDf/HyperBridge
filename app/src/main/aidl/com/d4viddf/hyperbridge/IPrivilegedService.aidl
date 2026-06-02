package com.d4viddf.hyperbridge;

import com.d4viddf.hyperbridge.IPrivilegedLogCallback;

interface IPrivilegedService {
    void setLogCallback(IPrivilegedLogCallback callback);
    boolean setPackageNetworkingEnabled(int uid, boolean enabled);
}
