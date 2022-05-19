import React, { useEffect, useState } from 'react';
import { Platform, Pressable, View, Text, StyleSheet } from 'react-native';
import RNFS from 'react-native-fs';
import WebView from 'react-native-webview';
import IServer from 'react-native-iserver';

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
    const docDir = Platform.OS === 'ios' ? RNFS.DocumentDirectoryPath : RNFS.DocumentDirectoryPath;
    const serverPath = '';
    const port = 8080;

    async function execute() {
        let url = ''
        let path = ''
        let { jobId, promise: task } = RNFS.downloadFile({
            fromUrl: url,
            toFile: path,
        });
        try {
            let zipExists = await RNFS.exists(path)
            if (!zipExists) {
                await task
            }
            IServer.unzip({
                zip: path,
                dest: serverPath,
                onError: (e) => {
                    //zip file is not valid
                }
            })
        } catch (error) {

        }
    }
    useEffect(() => {
        IServer.listen({
            onStart: () => {
                //starting to unzip file
            },
            onSuccess: () => {
                IServer.startWithPort({
                    directory: serverPath,
                    port
                }).then(started => {
                    console.log('the server start ' + (started ? 'success' : 'failed'))
                })
            }
        })
    }, [])

    return (
        <View style={{ flex: 1 }}>
            <View style={{ flex: 3 }}>
                {url != '' && <WebView source={{ uri: url }} containerStyle={{ flex: 1 }} />}
            </View>
            <View style={[styles.center]}>
                <Button label={'下载解压并启动服务'} onPress={execute} />
            </View>
        </View>
    )
}
export default App;

const styles = StyleSheet.create({
    center: {
        paddingVertical: 50,
        justifyContent: 'center',
        alignItems: 'center'
    }
})