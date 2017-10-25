package hello

import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.MediaType

class Http {
    private val client = OkHttpClient()

    var name = "My Ng Client"
    @Throws(Exception::class)
    fun get(url: String? = null) {
        //client.protocols().removeAt(1)
        val request = Request.Builder()
                .url(url  ?: "http://publicobject.com/helloworld.txt")
                .tag(this)
                .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val http = call.request().tag() as Http
                println("tag: ${http.name}")
                response.body()!!.use { responseBody ->
                    if (!response.isSuccessful) throw IOException("Unexpected code " + response)

                    val responseHeaders = response.headers()
                    var i = 0
                    val size = responseHeaders.size()
                    while (i < size) {
                        println(responseHeaders.name(i) + ": " + responseHeaders.value(i))
                        i++
                    }

                    println(responseBody.string())
                }
            }
        })

    }
    fun post(url: String, json: String) {
        val JSON = MediaType.parse("application/json; charset=utf-8")
        val body = RequestBody.create(JSON, json)
        val request = Request.Builder()
                .url(url)
                .post(body)
                .tag(this)
                .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val http = call.request().tag() as Http
                println("tag: ${http.name}")
                response.body()!!.use { responseBody ->
                    if (!response.isSuccessful) throw IOException("Unexpected code " + response)

                    val responseHeaders = response.headers()
                    var i = 0
                    val size = responseHeaders.size()
                    while (i < size) {
                        println(responseHeaders.name(i) + ": " + responseHeaders.value(i))
                        i++
                    }

                    println(responseBody.string())
                }
            }
        })

    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            Http().get("http://gw.codein.net/protocol.html")
            Http().post("https://gw.codein.net/echo", "{\"hello world\"}")
        }
    }
}