package com.example

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.ServerSocket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

val InvalidPortException = Exception("invalid port number, available range: [1024 - 65535]")
val InvalidDirectoryException = Exception("target is not valid directory")
val NotExistedDirectoryException = Exception("directory not existed")
val ServerClientReinitException = Exception("cannot re-init server client")

/**
 * ServerClient Init may throw BindException (Address already in use)
 */
class ServerClient {
    private var directory: File? = null

    // WellKnown Port : 0 - 1023
    // Register Port  : 1024 - 49151
    // Dynamic Port   : 49152 - 65535
    private var port: Int = 8999
    private var serverSocket: ServerSocket? = null
    private var indexFile: String = "index.html"
    private var stopRequired: Boolean = false

    companion object {
        fun joinPath(vararg args: String): String {
            var path = ""
            for (arg in args) {
                var narg = ""
                narg = if (!arg.startsWith("/")) "/$arg" else arg
                if (narg.endsWith("/")) {
                    narg = narg.removeSuffix("/")
                }
                path += narg
            }
            return path
        }
    }

    public fun isRunning(): Boolean {
        val closed = serverSocket?.isClosed ?: true
        return !closed
    }

    private fun joinPath(vararg args: String): String {
        var path = ""
        for (arg in args) {
            var narg = ""
            narg = if (!arg.startsWith("/")) "/$arg" else arg
            if (narg.endsWith("/")) {
                narg = narg.removeSuffix("/")
            }
            path += narg
        }
        return path
    }

    /**
     * @throws InvalidDirectoryException
     * @throws NotExistedDirectoryException
     */
    private fun prepareDirectory(directory: File) {
        if (!directory.isDirectory) {
            throw InvalidDirectoryException
        }
        if (!directory.exists()) {
            throw NotExistedDirectoryException
        }
        this.directory = directory
    }

    /**
     * @throws InvalidPortException
     * @throws ServerClientReinitException
     * @throws BindException
     */
    private fun prepareServerSocket(port: Int): ServerSocket {
        if (port <= 1024 || port > 65535) {
            throw InvalidPortException
        }
        if (serverSocket != null) {
            throw ServerClientReinitException
        }
        this.port = port
        //may throw BindException(address already in use)
        return ServerSocket(port)
    }

    private fun listenAndServe(serverSocket: ServerSocket) {
        while (true) {
            if (stopRequired) {
                stopRequired = false
                if (this.serverSocket != null) this.serverSocket!!.close()
                this.serverSocket = null
                break
            }
            val client = serverSocket.accept();
            Thread(java.lang.Runnable {
                val ins = client.getInputStream()
                val streamData = HttpInputStreamData(ins, bufferSize = 2048)
                if (streamData.readIOException != null) {
                    HttpResponse(client.getOutputStream()).error(500, null)
                    return@Runnable
                }
                if (streamData.lines.size == 0) return@Runnable
                val requestData = HttpRequestData(streamData.lines)
                if (requestData.requestPath.origin == "/") {
                    val rootFile = File(directory!!.path)
                    if (rootFile.isDirectory) {
                        HttpResponse(client.getOutputStream()).file(joinPath(directory!!.path, indexFile))
                    } else {
                        HttpResponse(client.getOutputStream()).error(500, null)
                    }
                } else {
                    HttpResponse(client.getOutputStream()).file(joinPath(directory!!.path, requestData.requestPath.path))
                }
            }).start()
        }
    }

    fun stop() {
        if (!stopRequired) {
            stopRequired = true
        }
    }

    fun launch(rootPath: String) {
        launchWithPort(this.port, rootPath)
    }

    fun launchWithPort(port: Int, rootPath: String) {
        launchWithPort(port, rootPath, null)
    }

    fun launchWithPort(port: Int, rootPath: String, indexFile: String?) {
        if (indexFile != null) {
            this.indexFile = indexFile
        }
        prepareDirectory(File(rootPath))
        val serverSocket = prepareServerSocket(port)
        println("local server's running at: $port")
        listenAndServe(serverSocket)
    }
}

/**
 * @HttpInputStreamData
 * @description
 * 接收Socket客户端拿取的InputStream、读取其中的字节内容、根据\r\n分隔符
 * 将所有内容拆分成 行 为单位的ArrayList
 */
class HttpInputStreamData(ins: InputStream, bufferSize: Int?) {
    public var lines = kotlin.collections.ArrayList<String>()
    public var readIOException: IOException? = null

    init {
        val size = bufferSize ?: 1024
        var bytes = ByteArray(size)
        var len: Int
        while (true) {
            try {
                len = ins.read(bytes, 0, size)
            } catch (e: IOException) {
                readIOException = e
                break
            }
            var line = ""
            var preChar = ' '
            for (i in 0..0.coerceAtLeast((len - 1))) {
                // 两种情况会出现 \r
                // 1 => 每行请求头的行尾为 \r\n
                // 2 => 请求头与请求体的分隔 \r\n
                val char = bytes[i].toInt().toChar()
                when (char) {
                    '\n', '\r' -> {
                        if (preChar == '\r' && char == '\n') {
                            if (line == "") {
                                //上次\r\n出现、清空line后、line并未新增字符并直接再次出现\r\n
                                //当前为请求头和请求体的分隔行
                                lines.add("\\r\\n")
                            } else {
                                lines.add(line)
                                line = ""
                            }
                        }
                    }
                    else -> line += char
                }
                preChar = char
            }
            if (len == -1 || len < size) {
                break
            }
        }
    }
}

/**
 * @HttpRequestData
 * @description
 * 将HttpInputStreamData的字节行数组解析出对应的请求路径、方法、请求头和请求体
 */
class HttpRequestData(request: List<String>) {
    var requestPath: RequestPath = RequestPath("")
    var method: String = HttpMethod.GET
    var header: HashMap<String, String> = HashMap()

    data class RequestPath(val origin: String) {
        //origin => /xxx.png?param1=load
        lateinit var path: String
        lateinit var params: String

        init {
            if (origin.isNotEmpty()) {
                val sepInd = origin.indexOf("?")
                if (sepInd != -1) { //存在路径参数
                    params = origin.substringAfter("?")
                    path = origin.substringBefore("?")
                } else {//无路径参数
                    path = origin
                }
            }
        }
    }

    var isReferer: Boolean = false
        get() {
            var ref = false
            for (key in header.keys) {
                if (key == "Referer" || key == "referer") {
                    ref = true
                    break
                }
            }
            return ref
        }

    init {
        if (request.isNotEmpty()) {
            //处理请求类型、非GET/POST 直接返回null
            var goOn = true
            if (request[0].indexOf(string = HttpMethod.GET) != -1) {
                this.method = HttpMethod.GET
            } else if (request[0].indexOf(string = HttpMethod.POST) != -1) {
                this.method = HttpMethod.POST
            } else { //invalid header
                goOn = false
            }
            if (goOn) {
                val path = extractPath(request[0])
                requestPath = RequestPath(path)
                //处理剩余header以及body
                var isHeadBodySep = false
                request.forEach { h ->
                    if (h.startsWith("GET") || h.startsWith("POST")) {
                        this.header["head"] = h
                    } else {
                        if (h == "\\r\\n") {
                            //内容为请求头请求行分隔符
                            isHeadBodySep = true
                        } else {
                            if (isHeadBodySep) {
                                //后面内容为body
                            } else {
                                //内容为header
                                val heps = h.split(":", limit = 2)
                                if (heps.isNotEmpty()) {
                                    this.header[heps[0]] = (if (heps.size == 2) heps[1] else "")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractPath(value: String): String {
        val seps = value.split(" ")
        if (seps.size < 2) {
            return ""
        }
        return seps[1]
    }
}

class HttpResponseHeader {
    private var defaultVersion: String = "1.1"
    private var builder: StringBuilder = StringBuilder()
    private val sep = ":"
    private val dsep = ","

    private fun genStatus(code: Int): String {
        val status = when (code) {
            200 -> "200 OK"
            304 -> "304 Not Modified"
            400 -> "400 Bad Request"
            401 -> "401 Unauthorized"
            402 -> "402 Payment Required"
            403 -> "403 Forbidden"
            404 -> "404 Not Found"
            405 -> "405 Method Not Allowed"
            500 -> "500 Internal Server Error"
            501 -> "501 Not Implemented"
            502 -> "502 Bad Gateway"
            503 -> "503 Service Unavailable"
            504 -> "504 Gateway Timeout"
            505 -> "505 HTTP Version Not Supported"
            else -> "500 Internal Server Error"
        }
        return status
    }

    private fun contentType(builder: StringBuilder, type: String) {
        builder.append(HttpHeader.ContentType, sep, type, ENDOFL)
    }

    fun build(): String {
        //check reduplicate header key
        return builder.append(ENDOFL).toString()
    }

    fun initial(status: Int, version: HttpVersion?): HttpResponseHeader {
        builder.append("HTTP/${version ?: defaultVersion} ${genStatus(status)}", ENDOFL)
        builder.append(HttpHeader.Date, sep, Date(), ENDOFL)
        return this
    }

    fun cache(directive: String, expire: String?, seconds: Int?): HttpResponseHeader {
        var s = HttpHeader.CacheControl + sep + directive
        if (expire != null) {
            val e = expire + "=" + (seconds ?: 0)
            s = "$s$dsep$e"
        }
        builder.append(s, ENDOFL)
        return this
    }

    fun acceptRanges(ranges: List<String>): HttpResponseHeader {
        builder.append(HttpHeader.AcceptRanges, sep, ranges.joinToString(dsep), ENDOFL)
        return this
    }

    fun contentLength(length: Int): HttpResponseHeader {
        builder.append(HttpHeader.ContentLength, sep, length, ENDOFL)
        return this
    }

    fun lastModified(date: Date): HttpResponseHeader {
        builder.append(HttpHeader.LastModified, sep, date, ENDOFL)
        return this
    }

    fun mime(value: String): HttpResponseHeader {
        this.contentType(builder, value)
        return this
    }

    fun text(mime: String): HttpResponseHeader {
        this.contentType(builder, mime.toString())
        return this
    }

    fun video(mime: String): HttpResponseHeader {
        this.contentType(builder, mime.toString())
        return this
    }

    fun image(mime: String): HttpResponseHeader {
        this.contentType(builder, mime.toString())
        return this
    }
}

class HttpResponse {
    private val out: OutputStream?;

    constructor(out: OutputStream?) {
        this.out = out;
    }

    private fun serve(value: String) {
        if (out != null) {
            out.write(value.toByteArray())
            out.flush()
            out.close()
        }
    }

    private fun serveBytes(value: ByteArray) {
        if (out != null) {
            try {
                out.write(value)
                out.flush()
            } catch (e: IOException) {
                println("client may abort connection,cannot write anymore")
            }
            out.close()
        }
    }

    fun error(status: Int, data: String?) {
        val header = HttpResponseHeader()
            .initial(status, null)
            .text(HttpMime.Companion.Text.html)
            .contentLength((data ?: "").length)
            .build() // append sep line  [\r\n]
        serve(header + (data ?: ""))
    }

    fun file(path: String) {
        val f = File(path)
        if (f.isFile) {
            val ranges = ArrayList<String>()
            ranges.add("bytes")
            val header = HttpResponseHeader()
                .initial(200, null)
                .lastModified(Date(f.lastModified()))
                .acceptRanges(ranges)
            var hasContentType: Boolean = true
            when (f.extension) {
                "html", "htm" -> header.text(HttpMime.Companion.Text.html)
                "css" -> header.text(HttpMime.Companion.Text.css)
                "js" -> header.text(HttpMime.Companion.Text.javascript)
                "json" -> header.text(HttpMime.Companion.Application.json)
                "xml" -> header.text(HttpMime.Companion.Application.xml)
                // Images
                "svg" -> header.image(HttpMime.Companion.Image.svg)
                "ico" -> header.image(HttpMime.Companion.Image.ico)
                "png", "Png", "PNG" -> header.image(HttpMime.Companion.Image.png)
                "jpeg", "JPEG", "Jpeg" -> header.image(HttpMime.Companion.Image.jpeg)
                "jpg", "JPG", "Jpg" -> header.image(HttpMime.Companion.Image.jpg)
                "gif", "Gif", "GIF" -> header.image(HttpMime.Companion.Image.gif)
                "webp", "Webp", "WEBP" -> header.image(HttpMime.Companion.Image.webp)
                "heif", "HEIF", "Heif" -> header.image(HttpMime.Companion.Image.heif)
                "heic", "HEIC", "Heic" -> header.image(HttpMime.Companion.Image.heic)
                // Videos
                "mp4" -> header.video(HttpMime.Companion.Video.mp4)
                "webm" -> header.video(HttpMime.Companion.Video.webm)
                "ogg" -> header.video(HttpMime.Companion.Video.ogg)
                // Audios
                "wav" -> header.mime(HttpMime.Companion.Audio.wav)
                "wave" -> header.mime(HttpMime.Companion.Audio.wave)
                // Font
                "ttf" -> header.mime(HttpMime.Companion.Font.ttf)
                "woff" -> header.mime(HttpMime.Companion.Font.woff)
                "woff2" -> header.mime(HttpMime.Companion.Font.woff2)
                else -> {
                    hasContentType = false
                }
            }

            val contentBytes = f.readBytes()
            if (hasContentType) {
                header.contentLength(contentBytes.size)
            }
            header.cache(
                CacheControlDirective.noCache,
                CacheControlExpire.maxAge,
                0
            )
            val headerBytes = header.build().toByteArray()
//            var respBytes = ByteArray(contentBytes.size + headerBytes.size)
//            System.arraycopy(headerBytes, 0, respBytes, 0, headerBytes.size)
//            System.arraycopy(contentBytes, 0, respBytes, headerBytes.size, contentBytes.size)
//            serveBytes(respBytes)
            if (out != null) {
                try {
                    out.write(headerBytes,0,headerBytes.size)
                    val fin = f.inputStream()
                    val finb = ByteArray(4096)
                    while (true) {
                        val count = fin.read(finb,0,4096)
                        if(count == -1) {
                            break
                        }
                        out.write(finb,0,count)
                    }
                    out.flush()
                    fin.close()
                } catch (e: IOException) {
                    println("client may abort connection,cannot write anymore")
                }
                out.close()
            }
        } else {
            //文件不存在
            error(404, null)
        }
    }
}

/**
 * Constants Definition
 */

const val ENDOFL = "\r\n"

class HttpHeader {
    companion object {
        const val WWWAuthenticate = "WWW-Authenticate"
        const val Authorization = "Authorization"
        const val ProxyAuthenticate = "Proxy-Authenticate"
        const val ProxyAuthorization = "Proxy-Authorization"
        const val Age = "Age"
        const val Date = "Date"
        const val CacheControl = "Cache-Control"
        const val ClearSiteData = "Clear-Site-Data"
        const val Expires = "Expires"
        const val Pragma = "Pragma"
        const val AcceptCH = "Accept-CH"
        const val LastModified = "Last-Modified"
        const val ETag = "ETag"
        const val IfMatch = "If-Match"
        const val IfNoneMatch = "If-None-Match"
        const val IfModifiedSince = "If-Modified-Since"
        const val IfUnmodifiedSince = "If-Unmodified-Since"
        const val Vary = "Vary"
        const val Connection = "Connection"
        const val KeepAlive = "Keep-Alive"
        const val Accept = "Accept"
        const val AcceptEncoding = "Accept-Encoding"
        const val AcceptLanguage = "Accept-Language"
        const val Expect = "Expect"
        const val MaxForwards = "Max-Forwards"
        const val Cookie = "Cookie"
        const val SetCookie = "Set-Cookie"
        const val AccessControlAllowOrigin = "Access-Control-Allow-Origin"
        const val AccessControlAllowCredentials = "Access-Control-Allow-Credentials"
        const val AccessControlAllowHeaders = "Access-Control-Allow-Headers"
        const val AccessControlAllowMethods = "Access-Control-Allow-Methods"
        const val AccessControlExposeHeaders = "Access-Control-Expose-Headers"
        const val AccessControlMaxAge = "Access-Control-Max-Age"
        const val AccessControlRequestHeaders = "Access-Control-Request-Headers"
        const val AccessControlRequestMethod = "Access-Control-Request-Method"
        const val Origin = "Origin"
        const val TimingAllowOrigin = "Timing-Allow-Origin"
        const val ContentDisposition = "ContentDisposition"
        const val ContentLength = "Content-Length"
        const val ContentType = "Content-Type"
        const val ContentEncoding = "Content-Encoding"
        const val ContentLanguage = "Content-Language"
        const val ContentLocation = "Content-Location"
        const val Forwarded = "Forwarded"
        const val Via = "Via"
        const val Location = "Location"
        const val From = "From"
        const val Host = "Host"
        const val Referer = "Referer"
        const val ReferrerPolicy = "Referrer-Policy"
        const val UserAgent = "User-Agent"
        const val Allow = "Allow"
        const val Server = "Server"
        const val AcceptRanges = "Accept-Ranges"
        const val Range = "Range"
        const val IfRange = "If-Range"
        const val ContentRange = "Content-Range"
        const val CrossOriginEmbedderPolicy = "Cross-Origin-Embedder-Policy"
        const val CrossOriginOpenerPolicy = "Cross-Origin-Opener-Policy"
        const val CrossOriginResourcePolicy = "Cross-Origin-Resource-Policy"
        const val ContentSecurityPolicy = "Content-Security-Policy"
        const val ContentSecurityPolicyReportOnly = "Content-Security-Policy-Report-Only"
        const val ExpectCT = "Expect-CT"
        const val FeaturePolicy = "Feature-Policy"
        const val StrictTransportSecurity = "Strict-Transport-Security"
        const val UpgradeInsecureRequests = "Upgrade-Insecure-Requests"
        const val XContentTypeOptions = "X-Content-Type-Options"
        const val XDownloadOptions = "X-Download-Options"
        const val Upgrade = "Upgrade"
    }
}

class HttpMethod {
    companion object {
        const val GET = "GET"
        const val POST = "POST"
    }
}

class HttpVersion {
    companion object {
        const val one = "1.0"
        const val one_one = "1.1"
    }
}

// link: https://www.iana.org/assignments/media-types/media-types.xhtml
class HttpMime {
    companion object {
        const val all = "*/*"

        class Image {
            companion object {
                const val png = "image/png"
                const val jpg = "image/jpg"
                const val jpeg = "image/jpeg"
                const val webp = "image/webp"
                const val gif = "image/gif"
                const val heif = "image/heif"
                const val heic = "image/heic"
                const val svg = "image/svg+xml"
                const val ico = "image/x-icon"
            }
        }

        class Video {
            companion object {
                const val mp4 = "video/mp4"
                const val ogg = "video/ogg"
                const val webm = "video/webm"
            }
        }

        class Audio {
            companion object {
                const val wav = "audio/wav"
                const val wave = "audio/wave"
                const val webm = "audio/webm"
                const val ogg = "audio/ogg"
            }
        }

        class Text {
            companion object {
                const val plain = "text/plain"
                const val html = "text/html"
                const val css = "text/css"
                const val javascript = "text/javascript"
            }
        }

        class Application {
            companion object {
                const val gzip = "application/gzip"
                const val json = "application/json"
                const val pdf = "application/pdf"
                const val xml = "application/xml"
                const val zip = "application/zip"
                const val javascript = "application/javascript"
            }
        }

        class Font {
            companion object {
                const val collection = "font/collection"
                const val otf = "font/otf"
                const val sfnt = "font/sfnt"
                const val ttf = "font/ttf"
                const val woff = "font/woff"
                const val woff2 = "font/woff2"
            }
        }
    }
}

class CacheControlDirective {
    companion object {
        const val noCache = "no-cache"
        const val noStore = "no-store"
        const val Public = "public"
        const val Private = "private"
    }
}

class CacheControlExpire {
    companion object {
        const val maxAge = "max-age"
        const val sMaxAge = "s-maxage"
        const val maxStale = "max-stale"
        const val minFresh = "min-fresh"
    }
}