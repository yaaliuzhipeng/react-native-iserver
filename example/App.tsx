import React, { useEffect, useState } from 'react';
import {View} from 'react-native';
import RNFS from 'react-native-fs';
import WebView from 'react-native-webview';
import WebServer from './WebServer';

const App = (props) => {

    const [url,setURL] = useState('');

    async function mockFlow() {
        let {jobId,promise: task} = RNFS.downloadFile({
            //⚠️ Replace the fromUrl with your own file address
            fromUrl: '',
            toFile: RNFS.DocumentDirectoryPath + '/tmp.zip',
        });
        try {
            let webPath = RNFS.DocumentDirectoryPath + '/webapp';
            //step 1
            // await task
            console.log('download zip success , path => ',`${RNFS.DocumentDirectoryPath}/tmp.zip`);

            //step 2
            await WebServer.unzip({
                zipPath: RNFS.DocumentDirectoryPath + '/tmp.zip',
                destinationPath: webPath
            })
            console.log('unzip zip-file success');

            //step 3
            //start server
            let started = await WebServer.startWithPort({
                directoryPath: webPath,
                port: 8080
            })
            if(started) {
                console.log('local server started at port : 8080');
            }else{
                console.log('local server start failed, check server dir');
            }
            requestAnimationFrame(() => {
                setURL('http://localhost:8080')
            })
        } catch (error) {
            console.log('download zip failed ,',error);
        }
    }

    useEffect(() => {
        // mockFlow();
    },[])

    return (
        <View style={{flex:1}}>
            {url != '' && <WebView source={{uri: url}} containerStyle={{flex:1}}/>}
        </View>
    )
}
export default App;