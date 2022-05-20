import { NativeModules, Platform, NativeEventEmitter } from 'react-native';

const { WebServer: NativeWebServer } = NativeModules;
const emitter = new NativeEventEmitter(NativeWebServer);

/**
 * Function Part 1
 */
const unzip = (options: {
    zip: string;
    dest: string;
    onError?: (e: string) => void;
}) => {
    let { zip = '', dest = '', onError } = options;
    NativeWebServer.unzip(zip, dest, (e) => {
        if (onError) onError(e)
    })
}

const listen = (configs?: {
    onStart?: (data: any) => void;
    onSuccess?: (data: any) => void;
    onError?: (data: { id: string; message: string }) => void;
}) => {
    const { onStart, onSuccess, onError } = (configs ?? {})
    return emitter.addListener('ZIPEVENT', (data: any) => {
        if (data.event == 'onStart') {
            if (onStart) onStart({ id: data.id })
        } else if (data.event == 'onSuccess') {
            if (onSuccess) onSuccess({ id: data.id })
        } else if (data.event == 'onError') {
            if (onError) onError({ id: data.id, message: data.message })
        }
    })
}


/**
 * Function Part 2
 */
const startWithPort = (options: {
    directory: string;
    port: number;
    indexFileName?: string; // index.html
    cacheAge?: number; // 3600 on ios and 0 on android
}) => {
    let { directory, port, indexFileName = 'index.html', cacheAge = 3600 } = options;
    return new Promise((resolve, reject) => {
        NativeWebServer.startWithPort(
            directory,
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