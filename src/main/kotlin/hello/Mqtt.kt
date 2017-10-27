package hello

import java.net.*
import java.io.*
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
import java.net.InetAddress



//import org.eclipse.paho.mqttv5.client.*
//import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence

interface MqttListener {
    fun onGameContactInfo(msg: MessagingProto.GameContactInfo, uid: Long, mid: Long) {}
    fun onImAddContact(msg: MessagingProto.ImAddContact, uid: Long, mid: Long) {}
}

class Mqtt constructor(var uid: Long, private var listener: MqttListener): MqttCallbackExtended {

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
            var msgUid = 0L
            var channel = 0
            when {
                topic.startsWith("codein/live/") -> {
                    topic.substring(12).toLong()
                }
                topic.startsWith("im/user/") -> {
                    channel = 1
                }
                topic.startsWith("im/group/") -> {
                    channel = 1
                };
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

            when(channel) {
                0 -> when (LiveMessageType.forNumber(type)) {
                    LiveMessageType.LMT_GAME_CONTACT_INFO -> {
                        var mqttMsg = MessagingProto.GameContactInfo.parseFrom(arr.sliceArray(10 until arr.size));
                        listener.onGameContactInfo(mqttMsg, msgUid, id)
                    }
                    else -> {
                    }
                }
                1 -> when(InstantMessageType.forNumber(type)) {
                    InstantMessageType.IM_ADD_CONTACT -> {
                        var mqttMsg = MessagingProto.ImAddContact.parseFrom(arr.sliceArray(10 until arr.size))
                        listener.onImAddContact(mqttMsg, msgUid, id)
                    }
                    else -> {

                    }
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

    fun sendLiveMsg(msg: com.google.protobuf.Message, type: Int) {
        var body = msg.toByteArray()
        val payload = ByteArray(size = body.size + 10)
        payload[0] = ((type shr 8) and 0xff).toByte()
        payload[1] = (type and 0xff).toByte()
        payload[2] = (body.size shr 56).toByte()
        payload[3] = (body.size shr 48).toByte()
        payload[4] = (body.size shr 40).toByte()
        payload[5] = (body.size shr 32).toByte()
        payload[6] = (body.size shr 24).toByte()
        payload[7] = (body.size shr 16).toByte()
        payload[8] = (body.size shr 8).toByte()
        payload[9] = body.size.toByte()

        (0 until body.size).forEach {
            payload[it + 10] = body[it]
        }

        return publish("codein/live", 1, payload)
    }

    fun sendImMsg(msg: com.google.protobuf.Message, type: Int) {
        var body = msg.toByteArray()
        val payload = ByteArray(size = body.size + 10)
        payload[0] = ((type shr 8) and 0xff).toByte()
        payload[1] = (type and 0xff).toByte()
        payload[2] = (body.size shr 56).toByte()
        payload[3] = (body.size shr 48).toByte()
        payload[4] = (body.size shr 40).toByte()
        payload[5] = (body.size shr 32).toByte()
        payload[6] = (body.size shr 24).toByte()
        payload[7] = (body.size shr 16).toByte()
        payload[8] = (body.size shr 8).toByte()
        payload[9] = body.size.toByte()


        (0 until body.size).forEach {
            payload[it + 10] = body[it]
        }

        return publish("im/sys", 1, payload)
    }

    fun subscribe(topic: String, qos: Int) {
        println("subscribing to topic $topic qos $qos")
        client?.subscribe(topic, qos)
    }

    var qos_ = 1
    var topics_ = arrayOf("im/sys/#")
    var broker_ = "tcp://gw.codein.net:1883"
    var clientId_ = ""
    var userName_ =  "codein_os_kt"
    var password_ = "os.cOdein.tv"

    init {
        if (clientId_.length < 1) {
            try {
                val addr = InetAddress.getLocalHost()
                val hostname = addr.hostName
                clientId_ = hostname + "_" + uid.toString()
            } catch (e: UnknownHostException) {
                clientId_ = "coro_" + (Math.random() * 1e12).toLong().toString(16)
            }
        }
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

    fun stop() {
        client?.disconnect()
    }

    companion object {
        private fun testMqtt(uid: Long) {
            var mqtt = Mqtt(uid, object: MqttListener{})
            mqtt.start()
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            //(1230000 until 1231200L).forEach {
            (1230001 .. 1230002L).forEach {
                testMqtt(it)
            }
        }
    }
}