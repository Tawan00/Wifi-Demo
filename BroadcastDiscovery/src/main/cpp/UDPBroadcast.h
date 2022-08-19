//
// Created by nant on 8/20/21.
//
//#define ANDROID_JNI
#define IOS

#ifndef TESTBROADCAST2_UDPBROADCAST_H
#define TESTBROADCAST2_UDPBROADCAST_H
#include <string>
#include <thread>
#include <functional>
#include <atomic>
class UDPBroadcast {
public:
    typedef std::function<void(const std::string&)> ReceiveCallback;

    // no copy
    UDPBroadcast(UDPBroadcast&) = delete;
    UDPBroadcast& operator=(UDPBroadcast const&) = delete;


    UDPBroadcast(){}
    virtual ~UDPBroadcast(){
        StopListening();
    }

    virtual int GetIp(std::string &ip) const;
    virtual std::string BroadcastUdp(const std::string& msg,int port) const;

    virtual void StartListening(ReceiveCallback receive_data_callback,
            int port);
    virtual void StopListening();



private:
    std::thread listening_thread_;
    std::atomic<bool> stop_listening_{false};
};


#endif //TESTBROADCAST2_UDPBROADCAST_H
