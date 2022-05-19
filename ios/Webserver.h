// Webserver.h

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <GCDWebServer/GCDWebServer.h>
#import <SSZipArchive.h>

@interface Webserver : RCTEventEmitter <RCTBridgeModule,SSZipArchiveDelegate>
@property (nonatomic,strong) GCDWebServer* webServer;
@end
