package hello

import java.util.*
import kotlin.experimental.and
import kotlin.concurrent.thread
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.sql.Timestamp

import com.google.protobuf.Message
import protofiles.protojava.CommonProtos as CommonProto
import protofiles.protojava.MessagingProto
import protofiles.protojava.UserProto
import protofiles.protojava.CodeInProtos
import protofiles.protojava.MessagingProto.LiveMessageType
import protofiles.protojava.MessagingProto.InstantMessageType

//import org.eclipse.paho.mqttv5.client.*
//import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence

interface MqttListener {
    fun onGameContactInfo(msg: MessagingProto.GameContactInfo, uid: Long, mid: Long) {}
}

class Mqtt : MqttCallbackExtended {
    private var listener = object: MqttListener{
        // null
    }

    private var brokerUrl: String? = null
    private var client: MqttClient? = null

    override fun connectComplete(reconnect: Boolean, serverURI: String) {
        val time = Timestamp(System.currentTimeMillis())
        for (topic in topics_) {
            subscribe(topic, qos_)
        }
    }

    override fun connectionLost(cause: Throwable?) {
        val time = Timestamp(System.currentTimeMillis())
        Thread.sleep(1000L)
        try {
            connect(broker_, clientId_, true, userName_, password_)
            for (topic in topics_) {
                subscribe(topic, qos_)
            }
        } catch (e: MqttException) {
            println("reason " + e.reasonCode)
            println("msg " + e.message)
            println("loc " + e.localizedMessage)
            println("cause " + e.cause)
            println("excep " + e)
            e.printStackTrace()
        }
    }

    override fun messageArrived(topic: String?, msg: MqttMessage?) {
        val time = Timestamp(System.currentTimeMillis())

        if (msg?.payload == null || topic == null || msg.payload.size < 10) {
            return
        }
        try {
            var uid = 0L
            when {
                topic.startsWith("codein/live/") -> {
                    topic.substring(12).toLong()
                }
                topic.startsWith("im/user/") -> return
                topic.startsWith("im/group/") -> return
                else -> return
            }

            val arr = msg.payload as ByteArray
            val type = (arr[0] and 0xff.toByte()).toInt() shl 8 or
                    (arr[1] and 0xff.toByte()).toInt()
            val id = (arr[0] and 0xff.toByte()).toLong() shl 56 or
                    ((arr[3] and 0xff.toByte()).toLong() shl 48) or
                    ((arr[4] and 0xff.toByte()).toLong() shl 40) or
                    ((arr[5] and 0xff.toByte()).toLong() shl 32) or
                    ((arr[6] and 0xff.toByte()).toLong() shl 24) or
                    ((arr[7] and 0xff.toByte()).toLong() shl 16) or
                    ((arr[8] and 0xff.toByte()).toLong() shl 8) or
                    (arr[9] and 0xff.toByte()).toLong()

            //when (InstantMessageType.forNumber(type)) {
            //    InstantMessageType.IM_CHAT_MESSAGE -> {
            //        var req = MessagingProto.ImChatMsg.parseFrom(arr.sliceArray(10 until arr.size));
            //        val str = req.toString()
            //        onImChatMsg(req, topic, id);
            //    }
            //    else -> {
            //    }
            //}
            when (LiveMessageType.forNumber(type)) {
                LiveMessageType.LMT_GAME_CONTACT_INFO -> {
                    var msg = MessagingProto.GameContactInfo.parseFrom(arr.sliceArray(10 until arr.size));
                    listener.onGameContactInfo(msg, uid, id)
                }
                else -> {
                }
            }
        } catch (e: com.google.protobuf.InvalidProtocolBufferException) {
        } catch (e: Exception) {

        }

        //println("Msg Arrive Time: $time Topic: $topic Qos: ${msg?.qos}\nPayload: $str")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        //LOGGER.info("message delivery complete. token $token")
    }


    fun connect(brokerUrl: String, clientId: String, cleanSession: Boolean, userName: String?, password: String?): Boolean {
        this.brokerUrl = brokerUrl
        val persistence = MemoryPersistence()
        var ok = false
        try {
            val connOpt = MqttConnectOptions()
            connOpt.isAutomaticReconnect = true
            connOpt.isCleanSession = cleanSession
            if (userName != null) {
                connOpt.userName = userName;
                if (password != null)
                    connOpt.password = password.toCharArray()
            }


            client = MqttClient(brokerUrl, clientId, persistence)
            client?.setCallback(this)
            client?.connect(connOpt)
            ok = true
        } catch (e: MqttException) {
            e.printStackTrace()
            println("error connect to $brokerUrl $e")
        }

        return ok;
    }

    fun publish(topic: String, qos: Int, payload: ByteArray){
        val time = Timestamp(System.currentTimeMillis())
        println("topic: $topic qos: $qos payload: ${payload.toString()}")
        val msg = MqttMessage()
        msg.payload = payload
        msg.qos = qos
        client?.publish(topic, msg)
    }

    fun subscribe(topic: String, qos: Int) {
        println("subscribing to topic $topic qos $qos")
        client?.subscribe(topic, qos)
    }

    var qos_ = 1
    var topics_ = arrayOf("im/sys/#")
    var broker_ = "tcp://localhost:1883"
    var clientId_ = "kt_let_" + (Math.random() * 1e12).toLong().toString(16)
    var userName_ =  "codein_os_kt"
    var password_ = "os.cOdein.tv"

    init {
        if (clientId_.length < 1)
            clientId_ = "kt_let_" + (Math.random() * 1e8).toLong().toString()
    }

    fun start() {
        try {
            if (connect(broker_, clientId_, true, userName_, password_)) {
            }
        } catch (e: MqttException) {
            println("reason " + e.reasonCode)
            println("msg " + e.message)
            println("loc " + e.localizedMessage)
            println("cause " + e.cause)
            println("excep " + e)
            e.printStackTrace()
        }
    }

    companion object {
        private fun testMqtt(testId: Int) {
            val req = CodeInProtos.GetMwRequest.newBuilder()
            req.gameId = 0
            val clinfo = req.clientInfoBuilder
            clinfo.uid = 2017009L
            clinfo.token = "kt_coro_http"
            clinfo.deviceId = "kt_coro_dev_000"
            val reply = CodeInProtos.GetMwReply.newBuilder()
            val start = System.currentTimeMillis()
            Http().gwCall("GetMw", req.build(), reply, object: GwCallback {
                override fun onReply(reply: Message) {
                    if (reply is CodeInProtos.GetMwReply) {
                        val timeCost = System.currentTimeMillis() - start
                        (0 until reply.listCount)
                                .map { reply.getList(it) }
                                .forEach { println("id: ${it.id} ${it.words}") }
                        println("test-$testId time cost: ${timeCost}ms latency: ${reply.latency}us, time: ${reply.timeUs}us")
                    }

                }

                override fun onError() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

            })
            val topic = "codein/live/a"
            try {
                val uid = topic.substring(12).toLong()
                println("uid = $uid")
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            (0 until 1).forEach {
                testMqtt(it)
            }
        }
    }
}