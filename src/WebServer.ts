import { NativeModules, Platform, NativeEventEmitter } from 'react-native';

const { WebServer: NativeWebServer } = NativeModules;
const emitter = new NativeEventEmitter(NativeWebServer);

/**
 * Function Part 1
 */
const unzip = (options: {
    zipPath: string;
    destinationPath: string;
    onError?: (e: string) => void;
}) => {
    let { zipPath = '', destinationPath = '', onError } = options;
    NativeWebServer.unzip(zipPath, destinationPath, (e) => {
        if (onError) onError(e)
    })
}
const listen = (configs?: {
    onStart?: () => void;
    onSuccess?: () => void;
    onError?: (e) => void;
}) => {
    const { onStart, onSuccess, onError } = (configs ?? {})
    return emitter.addListener('ZIPEVENT', (data) => {
        console.log('data => ', data);
    })
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
                if (Platform.OS === 'ios') {
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
            if (Platform.OS === 'ios') {
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
    isRunning,
    listen
})