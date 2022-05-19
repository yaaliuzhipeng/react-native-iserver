// Webserver.m

#import "Webserver.h"

/**
 FUNCTIONS PART
 
 part one:
 1. unzip
 
 part two:
 1. start server with sepcified dir,port ..
 
 */

NSString* zipevent = @"ZIPEVENT";
NSString* serverevent = @"SERVEREVENT";
@implementation Webserver
{
    bool hasListeners;
}

RCT_EXPORT_MODULE(WebServer)

- (NSArray<NSString *> *)supportedEvents
{
    return @[
        @"ZIPEVENT",
        @"SERVEREVENT"
    ];
}

- (void)startObserving
{
    hasListeners = YES;
}
- (void)stopObserving
{
    hasListeners = NO;
}
- (void) emit: (NSString *)eventName body:(id) body
{
    if (hasListeners) { // Only send events if anyone is listening
        [self sendEventWithName:@"EventReminder" body:body];
    }
}

/**
 part one
 */
RCT_EXPORT_METHOD(unzip: (NSString *)zipPath
                  destinationPath: (NSString *) destinationPath
                  onError: (RCTResponseSenderBlock) onError)
{
    NSFileManager *fileManager = [NSFileManager defaultManager];
    BOOL isFile = [fileManager isReadableFileAtPath:zipPath];
    if(!isFile) {
        onError(@[@"target zip path is not valid"]);
        return;
    }
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        [self emit:zipevent body:@{
            @"id": [NSUUID UUID].UUIDString,
            @"event": @"onStart"
        }];
        [SSZipArchive unzipFileAtPath:zipPath toDestination:destinationPath delegate:self];
    });
}
- (void) zipArchiveDidUnzipArchiveAtPath:(NSString *)path zipInfo:(unz_global_info)zipInfo unzippedPath:(NSString *)unzippedPath
{
    [self emit:zipevent body:@{
        @"id": [NSUUID UUID].UUIDString,
        @"event": @"onSuccess"
    }];
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
