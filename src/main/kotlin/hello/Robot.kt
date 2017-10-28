package hello

import kotlinx.coroutines.experimental.delay
import protofiles.protojava.MessagingProto

class Robot constructor(var uid: Long): MqttListener {
    override fun onGameContactInfo(msg: MessagingProto.GameContactInfo, msgUid: Long, mid: Long) {}
    override fun onNearbyUserUpdate(msg: MessagingProto.NearbyUserUpdate, msgUid: Long, mid: Long) {
        val str = msg.toString()
        println("nearby $uid -> $str")
    }
    val lngNW: Double = 113.910938
    val latNW: Double = 22.520323
    val lngSE: Double = 113.952137
    val latSE: Double = 22.484561

    var lng: Double = 0.0
    var lat: Double = 0.0

    var http = Http()
    var mqtt = Mqtt(uid, this)

    var status = 1

    init {
        gps()
        mqtt.start()
    }

    private fun gps() {
        var r = Math.random()
        lng = lngNW + (lngSE - lngNW) * r
        r = Math.random()
        lat = latNW + (latSE - latNW) * r
    }

    suspend fun run(): Robot {
        println("$uid -> ($lng $lat)")
        var msg = MessagingProto.SearchNearbyUsers .newBuilder()
        msg.clientInfoBuilder.uid = uid
        msg.clientInfoBuilder.gpsBuilder.latitude = lat
        msg.clientInfoBuilder.gpsBuilder.longitude = lng
        mqtt.sendLiveMsg(msg.build(), MessagingProto.LiveMessageType.LMT_SEARCH_NEARBY_USERS_VALUE)
        delay((2000 * Math.random()).toLong())
        gps()
        println("$uid -> ($lng $lat)")
        delay((2000 * Math.random()).toLong())
        gps()
        println("$uid -> ($lng $lat)")
        delay((2000 * Math.random()).toLong())
        gps()
        println("$uid => ($lng $lat)")
        status = 0
        stop()
        return this
    }

    fun stop() {
        mqtt.stop()
    }
}