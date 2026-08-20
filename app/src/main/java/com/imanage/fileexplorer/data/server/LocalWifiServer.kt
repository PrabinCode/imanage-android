package com.imanage.fileexplorer.data.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Environment
import com.imanage.fileexplorer.data.model.FileItem
import java.io.*
import java.net.*
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "",
    val port: Int = 8080,
    val sessionPin: String = "",
    val activeConnections: Int = 0
)

object LocalWifiServer {

    private const val DEFAULT_PORT = 8080
    private var serverSocket: ServerSocket? = null
    private val threadPool = Executors.newCachedThreadPool()
    private var isServerActive = false
    private var currentPin = ""

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    suspend fun startServer(context: Context, port: Int = DEFAULT_PORT): Result<String> = withContext(Dispatchers.IO) {
        if (isServerActive) {
            return@withContext Result.success(_serverState.value.ipAddress)
        }

        try {
            val detectedIp = getLocalIpAddress(context)
            val displayIp = if (detectedIp.isNotEmpty() && detectedIp != "127.0.0.1" && detectedIp != "0.0.0.0") {
                detectedIp
            } else {
                "192.168.1.x"
            }

            currentPin = (1000 + Random.nextInt(9000)).toString()

            // Bind to all network interfaces on IO thread
            val socket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            serverSocket = socket
            isServerActive = true

            _serverState.value = ServerState(
                isRunning = true,
                ipAddress = displayIp,
                port = port,
                sessionPin = currentPin
            )

            threadPool.execute {
                while (isServerActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        threadPool.execute { handleClient(client) }
                    } catch (e: Exception) {
                        break
                    }
                }
            }

            Result.success(displayIp)
        } catch (e: Exception) {
            isServerActive = false
            Result.failure(e)
        }
    }

    suspend fun stopServer() = withContext(Dispatchers.IO) {
        isServerActive = false
        try {
            serverSocket?.close()
        } catch (e: Exception) { }
        serverSocket = null
        _serverState.value = ServerState(isRunning = false)
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { client ->
                val input = BufferedReader(InputStreamReader(client.getInputStream()))
                val output = client.getOutputStream()

                val requestLine = input.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val fullUri = parts[1]
                val uriParts = fullUri.split("?")
                val path = URLDecoder.decode(uriParts[0], "UTF-8")
                val queryParams = if (uriParts.size > 1) parseQuery(uriParts[1]) else emptyMap()

                // Headers
                val headers = mutableMapOf<String, String>()
                var headerLine = input.readLine()
                while (!headerLine.isNullOrEmpty()) {
                    val idx = headerLine.indexOf(":")
                    if (idx != -1) {
                        headers[headerLine.substring(0, idx).trim().lowercase()] = headerLine.substring(idx + 1).trim()
                    }
                    headerLine = input.readLine()
                }

                val cookieHeader = headers["cookie"] ?: ""
                val isAuthed = cookieHeader.contains("imanage_auth=$currentPin") || queryParams["pin"] == currentPin

                if (path == "/auth") {
                    val enteredPin = queryParams["pin"] ?: ""
                    if (enteredPin == currentPin) {
                        val response = "HTTP/1.1 302 Found\r\nSet-Cookie: imanage_auth=$currentPin; Path=/; HttpOnly\r\nLocation: /\r\n\r\n"
                        output.write(response.toByteArray())
                    } else {
                        sendHtml(output, getLoginPage("Invalid PIN! Try again."))
                    }
                    return
                }

                if (!isAuthed) {
                    sendHtml(output, getLoginPage(null))
                    return
                }

                if (path == "/download") {
                    val filePath = queryParams["path"]
                    if (filePath != null) {
                        val file = File(filePath)
                        if (file.exists() && file.isFile && file.canRead()) {
                            sendFileDownload(output, file)
                            return
                        }
                    }
                }

                // Serve Web Explorer UI
                val currentDirPath = queryParams["dir"] ?: Environment.getExternalStorageDirectory().absolutePath
                val currentDir = File(currentDirPath)
                val targetDir = if (currentDir.exists() && currentDir.isDirectory) currentDir else Environment.getExternalStorageDirectory()

                sendHtml(output, getExplorerPage(targetDir))
            }
        } catch (e: Exception) { }
    }

    private fun sendHtml(output: OutputStream, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun sendFileDownload(output: OutputStream, file: File) {
        val encodedName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Disposition: attachment; filename=\"$encodedName\"\r\n" +
                "Content-Length: ${file.length()}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
        }
        output.flush()
    }

    private fun getLoginPage(error: String?): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>I Manage — Local PC Transfer</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                    .card { background: #1e293b; padding: 2.5rem; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); width: 100%; max-width: 380px; text-align: center; }
                    h2 { margin-top: 0; color: #38bdf8; }
                    input { width: 100%; padding: 12px; font-size: 1.2rem; text-align: center; letter-spacing: 4px; border: 2px solid #334155; border-radius: 8px; background: #0f172a; color: #fff; margin: 16px 0; box-sizing: border-box; }
                    input:focus { outline: none; border-color: #38bdf8; }
                    button { width: 100%; padding: 12px; background: #0284c7; border: none; border-radius: 8px; color: #fff; font-size: 1rem; font-weight: bold; cursor: pointer; }
                    button:hover { background: #0369a1; }
                    .error { color: #f87171; font-size: 0.9rem; margin-bottom: 12px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>🛡️ I Manage Transfer</h2>
                    <p style="color: #94a3b8; font-size: 0.9rem;">Enter the 4-digit PIN shown on your phone screen.</p>
                    ${if (error != null) "<div class='error'>$error</div>" else ""}
                    <form action="/auth" method="GET">
                        <input type="password" name="pin" maxlength="4" placeholder="••••" autofocus required />
                        <button type="submit">Unlock Explorer</button>
                    </form>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getExplorerPage(currentDir: File): String {
        val files = currentDir.listFiles() ?: emptyArray()
        val sorted = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        val parentPath = currentDir.parentFile?.absolutePath

        val rows = StringBuilder()

        if (parentPath != null && currentDir.absolutePath != "/storage/emulated/0" && currentDir.absolutePath != "/") {
            val encodedParent = URLEncoder.encode(parentPath, "UTF-8")
            rows.append("""
                <tr>
                    <td colspan="4"><a href="/?dir=$encodedParent" class="folder-link">📁 .. (Go Up)</a></td>
                </tr>
            """.trimIndent())
        }

        for (f in sorted) {
            val name = f.name
            val encodedPath = URLEncoder.encode(f.absolutePath, "UTF-8")
            if (f.isDirectory) {
                val count = f.list()?.size ?: 0
                rows.append("""
                    <tr>
                        <td>📁 <a href="/?dir=$encodedPath" class="folder-link">$name</a></td>
                        <td>$count items</td>
                        <td>Folder</td>
                        <td>—</td>
                    </tr>
                """.trimIndent())
            } else {
                val size = FileItem.formatBytes(f.length())
                rows.append("""
                    <tr>
                        <td>📄 $name</td>
                        <td>$size</td>
                        <td>${f.extension.uppercase()}</td>
                        <td><a href="/download?path=$encodedPath" class="download-btn">⬇️ Download</a></td>
                    </tr>
                """.trimIndent())
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>I Manage — Local PC Explorer</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 2rem; }
                    .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #334155; padding-bottom: 1rem; margin-bottom: 1.5rem; }
                    .title { color: #38bdf8; font-size: 1.5rem; font-weight: bold; }
                    .path-bar { background: #1e293b; padding: 0.75rem 1rem; border-radius: 8px; font-family: monospace; color: #94a3b8; word-break: break-all; margin-bottom: 1rem; }
                    table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 12px; overflow: hidden; }
                    th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #334155; }
                    th { background: #0f172a; color: #94a3b8; font-size: 0.85rem; text-transform: uppercase; }
                    tr:hover { background: #334155; }
                    a { color: #38bdf8; text-decoration: none; font-weight: 500; }
                    a:hover { text-decoration: underline; }
                    .download-btn { background: #0284c7; color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 0.85rem; display: inline-block; }
                    .download-btn:hover { background: #0369a1; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">📱 I Manage Wireless Transfer</div>
                    <div style="color: #4ade80; font-size: 0.9rem;">● Connected via Local Wi-Fi (Offline)</div>
                </div>
                <div class="path-bar">📂 ${currentDir.absolutePath}</div>
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Size</th>
                            <th>Type</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun parseQuery(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx != -1) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun getLocalIpAddress(context: Context): String {
        // Strategy 1: Modern Android ConnectivityManager across all active networks
        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                for (network in cm.allNetworks) {
                    val linkProperties = cm.getLinkProperties(network)
                    if (linkProperties != null) {
                        for (linkAddress in linkProperties.linkAddresses) {
                            val address = linkAddress.address
                            if (address is Inet4Address && !address.isLoopbackAddress) {
                                val host = address.hostAddress
                                if (host != null && host != "127.0.0.1" && !host.startsWith("169.254")) {
                                    return host
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { }

        // Strategy 2: Low-level NetworkInterface enumeration
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in interfaces) {
                val addrs = Collections.list(nif.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host != "127.0.0.1" && !host.startsWith("169.254")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) { }

        // Strategy 3: WifiManager fallback
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (e: Exception) { }

        return "127.0.0.1"
    }
}
