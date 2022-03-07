import { NativeModules, Platform } from 'react-native';

const { WebServer: NativeWebServer } = NativeModules;

/**
 * Function Part 1
 */
const unzip = (options: {
    zipPath: string;
    destinationPath: string;
}) => {
    let { zipPath = '', destinationPath = '' } = options;
    return new Promise((resolve, reject) => {
        NativeWebServer.unzip(
            zipPath,
            destinationPath,
            (success) => {
                if(Platform.OS === 'ios'){
                    resolve(success == 1);
                    return;
                }
                resolve(success);
            },
            (error:string) => {
                reject(new Error(error))
            }
        )
    });
}


/**
 * Function Part 2
 */
const startWithPort = (options: {
    directoryPath: string;
    port: number;
    indexFileName?: string; // index.html
    cacheAge?: number; // 3600
}) => {
    let { directoryPath, port, indexFileName = 'index.html', cacheAge = 3600 } = options;
    return new Promise((resolve, reject) => {
        NativeWebServer.startWithPort(
            directoryPath,
            port,
            indexFileName,
            cacheAge,
            (started) => {
                if(Platform.OS === 'ios') {
                    resolve(started === 1);
                    return;
                }
                resolve(started);
            }
        )
    });
}

const stop = () => {
    NativeWebServer.stop();
}

const isRunning = () => {
    return new Promise((resolve, reject) => {
        NativeWebServer.isRunning((v) => {
            if(Platform.OS === 'ios') {
                resolve(v === 1);
            }
            resolve(false);
        })
    })
}

export default ({
    unzip,
    startWithPort,
    stop,
    isRunning
})