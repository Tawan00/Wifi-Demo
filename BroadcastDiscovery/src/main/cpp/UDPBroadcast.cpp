//
// Created by nant on 8/20/21.
//
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>

#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include <ifaddrs.h>
#include <stdbool.h>
#include <errno.h>

#include <string>
#include <cstring>
#include <iostream>
#include <cstring>
#include <vector>
#include <chrono>
#include "UDPBroadcast.h"

#ifdef ANDROID_JNI
#include <android/log.h>
#endif

int UDPBroadcast::GetIp(std::string &ip) const
{
    struct ifaddrs *ifaddr, *ifa;
    int s;
    char host[NI_MAXHOST];

    //only Android 7.0 or upper have getifaddrs()
    //if you want to use with android 6 or 5 please implement getifaddrs()
    if (getifaddrs(&ifaddr) == -1)
    {
        ip= std::string("get ip failed. case of")+strerror(errno);
        return -1;
    }


    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next)
    {
        if (ifa->ifa_addr == NULL)
            continue;

        s=getnameinfo(ifa->ifa_addr,sizeof(struct sockaddr_in),host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);

        if( ifa->ifa_addr->sa_family==AF_INET ) //ipv4
        {
            if (s != 0){
                ip = gai_strerror(s);
                return -1;
            }
            if(strcmp(ifa->ifa_name,"lo") !=0) {
                ip = host;
                return 0;
            }
        }
    }

    freeifaddrs(ifaddr);

    return -1;
}


std::string UDPBroadcast::BroadcastUdp(const std::string &msg ,int port) const
{

    int sock;
    std::string ret_msg = "";
    sock = socket(AF_INET,SOCK_DGRAM,0);
    int int_true = 1;
    if(sock < 0){
#ifdef ANDROID_JNI
        __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "Error in create socket. case of %s\n", strerror(errno));
#endif
        ret_msg = "create socket_failed";
    }
    if(setsockopt(sock,SOL_SOCKET,SO_BROADCAST,&int_true,sizeof(int_true)) < 0)
    {
#ifdef ANDROID_JNI
        __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "Error in setting Broadcast option. case of %s\n", strerror(errno));
#endif
  //      std::cout<<"Error in setting Broadcast option. case of " << strerror(errno) <<".\n";
        ret_msg = "setsockopt() failed.";
        close(sock);
        return ret_msg;
    }
    if(setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &int_true, sizeof(int_true))<0){
#ifdef ANDROID_JNI
        __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "Error in set reuse socket. case of %s\n", strerror(errno));
#else
        std::cerr << "Error in set reuse socket. resons:"<< strerror(errno)<<std::endl;
#endif
    }
    struct sockaddr_in recv_addr;

    recv_addr.sin_family       = AF_INET;
    recv_addr.sin_port         = htons(port);
    recv_addr.sin_addr.s_addr = inet_addr("255.255.255.255");


    std::vector<unsigned  char> data_out;
    data_out.reserve(2+msg.length());
    short msg_len = msg.length();
    data_out.push_back((unsigned char)((msg_len >>8) &0xff));
    data_out.push_back((unsigned char)(msg_len&0xff));
    data_out.insert(data_out.end(),msg.begin(),msg.end());

    sendto(sock,(const char*)data_out.data(),data_out.size(),0,(sockaddr *)&recv_addr,sizeof(recv_addr));

    close(sock);
#ifdef ANDROID_JNI
    __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "broadcast end successfully with msg =%s" ,msg.c_str());
#endif
    ret_msg = "broadcast end successfully with msg ="+msg;

    return ret_msg;
}


void UDPBroadcast::StartListening(ReceiveCallback receive_data_callback,
        int port)
{
    stop_listening_ = false;
    auto listening_task = [=] (int port,ReceiveCallback callback) ->void {

        int sock_server = 0;
        struct sockaddr_in server_addr, client_addr;
        if((sock_server =socket(AF_INET,SOCK_DGRAM,0)) <0) {
#ifdef ANDROID_JNI
            __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "Error in create listening socket. case of %s\n", strerror(errno));
#else
            std::cerr << "create socket_failed. resons:"<< strerror(errno)<<std::endl;
#endif      
            return;
            
        }
        std::memset(&server_addr,0 , sizeof(struct  sockaddr_in));
        std::memset(&client_addr,0 , sizeof(struct  sockaddr_in));


        server_addr.sin_addr.s_addr = INADDR_BROADCAST;
        server_addr.sin_port = htons(port);
        server_addr.sin_family = AF_INET;

        if ( bind(sock_server, (const sockaddr *)&server_addr, sizeof(server_addr)) < 0 ) {
#ifdef ANDROID_JNI
            __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "Error in bind listening socket. case of %s\n", strerror(errno));
#else
            std::cerr << "Error in bind listening socket. resons:"<< strerror(errno)<<std::endl;
#endif
            return;
        }
        int reuse_flag = 1;
        if(setsockopt(sock_server, SOL_SOCKET, SO_REUSEADDR, &reuse_flag, sizeof(reuse_flag))<0){
#ifdef ANDROID_JNI
            __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "Error in set reuse socket. case of %s\n", strerror(errno));
#else
            std::cerr << "Error in set reuse socket. resons:"<< strerror(errno)<<std::endl;
#endif
        }

        std::vector<unsigned char> buffer;

        while(!stop_listening_){

            //SAFE MTU
            buffer.resize(1400);

            short n;
            socklen_t  addr_len = sizeof(client_addr);
            n = recvfrom(sock_server,
                    (char *)buffer.data(),
                    buffer.size(),
                    MSG_DONTWAIT,
                    ( struct sockaddr *) &client_addr,
                    &addr_len);

            if(n>14){
                buffer.resize(n);
                if(n >0){
                    std::string data (reinterpret_cast<char *>(buffer.data()+2),n-2);
                    data +=std::string(":") +inet_ntoa(client_addr.sin_addr);

#ifdef ANDROID_JNI
                    __android_log_print(ANDROID_LOG_INFO, "BROADCAST", "Receive data=%s",data.c_str());
#else
                    std::cout << "Receive data="<<data.c_str()<<"\n";
#endif
                    callback(data);
                }
            }
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }

        return ;
        

    };

  // listening_task(6000);
   this->listening_thread_ = std::thread(listening_task,port,std::move(receive_data_callback));
}
void  UDPBroadcast::StopListening(){

    stop_listening_ = true;
    if(this->listening_thread_.joinable()){
        listening_thread_.join();
    }
}

