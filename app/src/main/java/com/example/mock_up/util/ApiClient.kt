import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClientBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

//var URL = "http://149.248.20.141:80"
class ApiClient {

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }



    fun GET(url: String?): String {
        var inputStream: InputStream? = null
        var result = ""
        try {
            // 1. create HttpClient
            val httpclient: HttpClient = HttpClientBuilder.create().build()
            // 2. make GET request to the given URL
            val httpResponse: HttpResponse = httpclient.execute(HttpGet(url))
            // 3. receive response as inputS    tream
            inputStream = httpResponse.getEntity().getContent()
            // 4. convert inputstream to string
            if (inputStream != null) result =
                convertInputStreamToString(inputStream).toString()
            else result = "Did not work!"
        } catch (e: Exception) {
            Log.d("InputStream", e.localizedMessage)
        }
        return result
    }
    @Throws(IOException::class)
    private fun convertInputStreamToString(inputStream: InputStream): String? {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = ""
        var result: String? = ""
        while ((bufferedReader.readLine().also { line = it }) != null)
            result += line
        inputStream.close()
        return result
    }


    fun POST(url: String, jsonBody: String): String {
        // 1. 创建 HttpURLConnection
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.readTimeout = 15000
        conn.connectTimeout = 15000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        // 2. 将 JSON 数据写入请求体
        val os: OutputStream = conn.outputStream
        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
        writer.write(jsonBody)
        writer.flush()
        writer.close()
        os.close()

        // 3. 检查响应码并返回响应消息
        val responseCode = conn.responseCode
        return if (responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.bufferedReader().use { it.readText() }  // 返回服务器响应
        } else {
            "ERROR: $responseCode"
        }
    }
}
