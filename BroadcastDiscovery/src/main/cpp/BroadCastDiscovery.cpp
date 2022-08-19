//
// Created by nant on 8/20/21.
//
#include <string>
#include <memory>
#include <thread>
#include <chrono>
#include <random>
#include <jni.h>
#include "UDPBroadcast.h"
#include "BroadCastDiscovery.h"

#ifdef ANDROID_JNI
#include <jni.h>
#endif
namespace {
    std::unique_ptr<UDPBroadcast> broadcaster_ = nullptr;
    std::string broadcast_data_;

}


extern "C"
JNIEXPORT void JNICALL
Java_com_edc_kbtg_nativebroadcast_EDCBroadcaster_StartEDCProcess(
        JNIEnv* env,
        jobject  jthis,
        jstring jtid,
        jstring jmid) {

    const char* c_tid =env->GetStringUTFChars(jtid,0);
    const char* c_mid = env->GetStringUTFChars(jmid,0);;

    broadcast_data_ =std::string("DiscoveryResponse:")+
            std::string(c_tid)+
            std::string (":")+
            std::string(c_mid);
    if(broadcaster_ == nullptr)
        broadcaster_ = std::make_unique<UDPBroadcast>();

    const auto& EDCCallback = [&](const std::string& data)->void {
        if(data.find("DiscoveryHello") != std::string::npos){
            std::mt19937 random_generator( std::random_device{} ());
            std::uniform_int_distribution<> dist(200, 2000);

            std::this_thread::sleep_for(std::chrono::milliseconds(dist(random_generator)));

            broadcaster_->BroadcastUdp(broadcast_data_,6000);
        }
    };
    broadcaster_->StartListening(EDCCallback,6000);

    //stupid
    env->ReleaseStringUTFChars(jtid,c_tid);
    env->ReleaseStringUTFChars(jmid,c_mid);
}

//TBC
void POSProcessCallbackAndroid(){

}

void StartPOSProcessCCallBack(FoundDeviceCallback callback){
    if(broadcaster_ == nullptr)
        broadcaster_ = std::make_unique<UDPBroadcast>();
    const auto& POSCallback = [=](const std::string& data)->void {
        if(data.find("DiscoveryResponse") != std::string::npos){
            std::string tid;
            std::string mid;
            std::string ip;

            callback(tid.c_str(),mid.c_str(),ip.c_str());
        }
    };
    broadcaster_->StartListening(POSCallback,6000);
    broadcaster_->BroadcastUdp("DiscoveryHello",6000);

}
void ReSendDiscoveryMessage(){
    if(broadcaster_ != nullptr){
        broadcaster_->BroadcastUdp("DiscoveryHello",6000);
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_edc_kbtg_nativebroadcast_EDCBroadcaster_StopProcess(JNIEnv* env,
        jobject /* this */) {
    if (broadcaster_!= nullptr){
        broadcaster_.reset();
    }
}