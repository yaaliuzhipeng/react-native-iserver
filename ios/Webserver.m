// Webserver.m

#import "Webserver.h"

/**
 FUNCTIONS PART
 
 part one:
 1. unzip
 
 part two:
 1. start server with sepcified dir,port ..
 
 */


@implementation Webserver

RCT_EXPORT_MODULE(WebServer)

/**
 part one
 */
RCT_EXPORT_METHOD(unzip: (NSString *)zipPath
                  destinationPath: (NSString *) destinationPath
                  successCallback: (RCTResponseSenderBlock) successCallback
                  failCallback: (RCTResponseSenderBlock) failCallback)
{
    NSFileManager *fileManager = [NSFileManager defaultManager];
    BOOL isFile = [fileManager isReadableFileAtPath:zipPath];
    if(!isFile) {
        failCallback(@[@"target zip path is not valid"]);
        return;
    }
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        BOOL done = [SSZipArchive unzipFileAtPath:zipPath toDestination:destinationPath delegate:self];
        if(done) {
            successCallback(@[@true]);
        }else{
            successCallback(@[@false]);
        }
    });
}
- (void) zipArchiveDidUnzipArchiveAtPath:(NSString *)path zipInfo:(unz_global_info)zipInfo unzippedPath:(NSString *)unzippedPath
{
    //to-do
}
- (void)zipArchiveProgressEvent:(unsigned long long)loaded total:(unsigned long long)total
{
    //to-do
}

/**
 part two
 */

RCT_EXPORT_METHOD(startWithPort:(NSString *) directoryPath
                  port: (nonnull NSNumber *) port
                  indexFileName:(NSString *)indexFileName
                  cacheAge: (nonnull NSNumber *) cacheAge
                  callback:(RCTResponseSenderBlock) callback)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_webServer == nil) {
            self->_webServer = [[GCDWebServer alloc] init];
        }
        [self->_webServer addGETHandlerForBasePath: @"/" directoryPath:directoryPath indexFilename:indexFileName cacheAge:[cacheAge unsignedIntegerValue] allowRangeRequests:YES];
        BOOL started = [self->_webServer startWithPort:[port unsignedIntegerValue] bonjourName:nil];
        if(started) {
            callback(@[@true]);
        }else{
            callback(@[@false]);
        }
    });
}

RCT_EXPORT_METHOD(stop)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(self->_webServer != nil) {
            [self->_webServer stop];
        }
    });
}

RCT_EXPORT_METHOD(isRunning: (RCTResponseSenderBlock) callback)
{
    if(_webServer == nil){
        callback(@[@false]);
    }else{
        if([_webServer isRunning]) {
            callback(@[@true]);
        }else{
            callback(@[@false]);
        }
    }
}

@end
