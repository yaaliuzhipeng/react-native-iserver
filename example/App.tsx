import React, { useEffect, useState } from 'react';
import {Platform, View} from 'react-native';
import RNFS from 'react-native-fs';
import WebView from 'react-native-webview';
import WebServer from '../WebServer';

const App = (props) => {

    const [url,setURL] = useState('http://www.baidu.com');
    const dir = Platform.OS === 'ios' ? RNFS.DocumentDirectoryPath : RNFS.DocumentDirectoryPath;

    async function mockFlow() {
        let {jobId,promise: task} = RNFS.downloadFile({
            //⚠️ Replace the fromUrl with your own file address
            fromUrl: 'https://macromap-1254235053.cos.ap-chengdu.myqcloud.com/bale001.zip',
            toFile: dir + '/tmp.zip',
        });
        try {
            let webPath = dir + '/webapp';
            //step 1
            let zipExists = await RNFS.exists(`${dir}/tmp.zip`)
            if(!zipExists) {
                await task
            }
            console.log('download zip success , path => ',`${dir}/tmp.zip`);

            //step 2
            await WebServer.unzip({
                zipPath: dir + '/tmp.zip',
                destinationPath: webPath
            })
            console.log('unzip zip-file success');

            //step 3
            //start server
            // let started = await WebServer.startWithPort({
            //     directoryPath: webPath,
            //     port: 8080
            // })
            // if(started) {
            //     console.log('local server started at port : 8080');
            // }else{
            //     console.log('local server start failed, check server dir');
            // }
            // requestAnimationFrame(() => {
            //     setURL('http://localhost:8080')
            // })
        } catch (error) {
            console.log('download zip failed ,',error);
        }
    }

    useEffect(() => {
        mockFlow();
    },[])

    return (
        <View style={{flex:1}}>
            {url != '' && <WebView source={{uri: url}} containerStyle={{flex:1}}/>}
        </View>
    )
}
export default App;