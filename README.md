<p align="center">
  <img src="https://github.com/yaaliuzhipeng/react-native-iserver/blob/main/raw/logo.png" alt="ReactNative IServer" width="539" />
</p>

<h4 align="center">
  A Powerful Local Static Server
</h4>

<p align="center">
  Build Powerful App With Local Static Server Which Support Both <em>Android</em> And <em>iOS</em> <em>Amazing</em> ⚡️
</p>

<p align="center">
  <a href="https://github.com/yaaliuzhipeng/react-native-iserver">
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License">
  </a>
</p>

|   | IServer |
| - | ------------ |
| ⚡️ | **Powerful Kotlin** android-side is build on scratch with kotlin |
| 😎 | **Unzip Feature**. unzip files through this module |
| 📱 | **Multiplatform**. iOS, Android |
| ⏱ | **Fast.** About TO Migrating to RN New Arch(JSI) |
| ⚠️ | **Static typing** Full-Support [TypeScript](https://typescriptlang.org) |

## Why MLKitTranslator?

Key capabilities

- Both android and ios are supported. ios-side is powered by GCDWebserver and android-side is build on scratch by myself, stable and reliable
- I'm also about to migrating this package to JSI or Turbomodule when it's ready to go

## Installation

> yarn add react-native-iserver

or

> npm i --save react-native-iserver


## Usage


> ETC: you'd like to download a zip file and start local server to server these files

>ETC: 比如你想下载一个压缩包、解压后并启动本地静态资源服务器来提供文件的访问


```typescript
import RNFS from 'react-native-fs';
import IServer from 'react-native-iserver';

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
            zipPath: path,
            destinationPath: serverPath,
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
                directoryPath: serverPath,
                port
            }).then(started => {
                console.log('the server start ' + (started ? 'success' :'failed'))
            })
        }
    })
}, [])
```

## APIS

### startWithPort
> 启动本地静态资源服务
```typescript
(options: {
    directory: string;
    port: number;
    indexFileName?: string; // index.html
    cacheAge?: number; // 3600 on ios and 0 on android
}) => void;
```
### isRunning
> server是否正在运行
```typescript
() => void;
```
### stop
> 停止本地server
```typescript
() => void;
```
### unzip
> 解压本地ZIP压缩包,解压状态需通过listen方法进行监听
```typescript
(options: {
    zip: string;
    dest: string;
    onError?: (e: string) => void;
}) => void;
```
### listen
> 解压本地ZIP压缩包,解压状态需通过listen方法进行监听
```typescript
(configs?: {
    onStart?: () => void;
    onSuccess?: () => void;
    onError?: (e) => void;
}) => void;
```
## Author and license

**ReactNativeIServer** was created by [@yaaliuzhipeng](https://github.com/yaaliuzhipeng)

react-native-iserver is available under the MIT license. See the [LICENSE file](./LICENSE) for more info.