#include "BroadCastDiscovery.h"
#include <chrono>
#include <thread>
#include <cstdio>

void FoundDevice(const char* tid, const char* mid, const char *ip){
        printf("%s %s :%s",tid,mid,ip);
     
}

int main()
{
    
    int mode;
    scanf("%d",&mode);
    
    
    if(mode==1)
        StartPOSProcessCCallBack(FoundDevice);
    else if(mode==2) 
        StartEDCProcess("12345678","123456789012345");
    printf("setup done\n");
    std::this_thread::sleep_for(std::chrono::seconds(100));
    return 0;
}
