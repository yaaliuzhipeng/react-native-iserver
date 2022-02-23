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
                if(Platform.OS === 'ios') resolve(success == 1);
                //other platform
                resolve(true);
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
                if(Platform.OS === 'ios') resolve(started === 1);
                //other platform
                resolve(true);
            }
        )
    });
}

export default ({
    unzip,
    startWithPort
})