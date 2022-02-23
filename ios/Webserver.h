// Webserver.h

#import <React/RCTBridgeModule.h>
#import <GCDWebServer/GCDWebServer.h>
#import <SSZipArchive.h>

@interface Webserver : NSObject <RCTBridgeModule,SSZipArchiveDelegate>
@property (nonatomic,strong) GCDWebServer* webServer;
@end
