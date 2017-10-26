package hello

import java.io.IOException
import kotlinx.coroutines.experimental.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.MediaType

import protofiles.protojava.CommonProtos as CommonProto
import protofiles.protojava.MessagingProto
import protofiles.protojava.UserProto
import protofiles.protojava.CodeInProtos as CodeInProto

class Request {
    var name = "Request"
}

class Reply {
    var name = "Reply"
}

class Http {
    private val client = OkHttpClient()

    var name = "My Ng Client"
    var gwUrl = "http://gw.codein.net/api/"

    @Throws(Exception::class)
    fun get(url: String? = null) {
        val request = Request.Builder()
                .url(url  ?: "http://publicobject.com/helloworld.txt")
                //.tag(this)
                .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                //val http = call.request().tag() as Http
                //println("tag: ${http.name}")
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
        //call.enqueue(object : Callback {
        call.enqueue(object: Callback {
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

    fun post(name: String, req: com.google.protobuf.Message, rep: com.google.protobuf.Message.Builder) {
        var ba = ByteArray(10)
        rep.mergeFrom(ba)
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            var http = Http();
            //Http().get("http://gw.codein.net/protocol.html")
            http.post("https://gw.codein.net/echo", "{\"hello world\"}")
            //http.post("test", 0, Reply())
            //http.get("http://gw.codein.net/protocol.html")
            //http.post("https://gw.codein.net/echo", "{\"hello world\"}")
            var builder = CodeInProto.LoginRequest.newBuilder()
            var req = builder.build()
            http.post("hello", req, builder)
        }
    }
}
