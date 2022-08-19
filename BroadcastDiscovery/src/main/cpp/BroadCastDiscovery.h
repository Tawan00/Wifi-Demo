//
// Created by nant on 8/29/21.
//

#ifndef TESTBROADCAST2_BROADCASTDISCOVERY_H
#define TESTBROADCAST2_BROADCASTDISCOVERY_H


typedef void (*FoundDeviceCallback)(const char* tid, const char* mid, const char *ip);

//non android ndk
#ifdef __cplusplus
extern "C"{
#endif

void StartEDCProcess(const char *c_tid, const char *c_mid);

void StartPOSProcessCCallBack(FoundDeviceCallback callback);

void ReSendDiscoveryMessage();

void StopProcess();

#ifdef __cplusplus //extern "C"
}
#endif

#endif //TESTBROADCAST2_BROADCASTDISCOVERY_H
