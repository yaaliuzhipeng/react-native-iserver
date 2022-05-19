import React, { useEffect, useState } from 'react';
import { Platform, Pressable, View, Text } from 'react-native';
import RNFS from 'react-native-fs';
import WebView from 'react-native-webview';
import WebServer from '../src/WebServer';

const Button = (props: {
    label: any;
    onPress: any;
}) => {
    const { label, onPress } = props;
    return (
        <Pressable onPress={onPress} style={{ backgroundColor: '#f3f3f5', paddingHorizontal: 12, paddingVertical: 3, borderRadius: 8, margin: 5 }}>
            <Text style={{ fontSize: 17, color: '#222' }}>{label}</Text>
        </Pressable>
    )
}

const App = (props) => {

    const [url, setURL] = useState('');
    const dir = Platform.OS === 'ios' ? RNFS.DocumentDirectoryPath : RNFS.DocumentDirectoryPath;
    const webDir = dir + '/x';
    const zipName =  '/x.zip';

    async function donwloadZip() {
        let { jobId, promise: task } = RNFS.downloadFile({
            //⚠️ Replace the fromUrl with your own file address
            fromUrl: '',
            toFile: dir + zipName,
        });
        try {
            console.log('downloading zip')
            //step 1
            let zipExists = await RNFS.exists(`${dir}${zipName}`)
            if (!zipExists) {
                await task
            }
            console.log('download zip success , path => ', `${dir}${zipName}`);
        } catch (error) {

        }
    }
    useEffect(() => {
        WebServer.listen({
            onStart: () => {
                console.log('开始解压');
            },
            onSuccess: () => {
                console.log('解压缩完成');
            }
        })
    },[])
    function unzipZipFile() {
        WebServer.unzip({
            zipPath: dir + zipName,
            destinationPath: webDir
        })
    }
    async function startServer() {
        //start server
        let started = await WebServer.startWithPort({
            directoryPath: webDir,
            port: 8080
        })
        if(started) {
            console.log('local server started at port : 8080');
        }else{
            console.log('local server start failed, check server dir');
        }
        setTimeout(() => {
            setURL('http://localhost:8080')
        }, 200);
    }

    return (
        <View style={{ flex: 1 }}>
            <View style={{ flex: 3 }}>
                {url != '' && <WebView source={{ uri: url }} containerStyle={{ flex: 1 }} />}
            </View>
            <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
                <Button label={'下载ZIP包'} onPress={donwloadZip} />
                <Button label={'解压缩ZIP包'} onPress={unzipZipFile} />
                <Button label={'启动服务'} onPress={startServer} />
            </View>
        </View>
    )
}
export default App;