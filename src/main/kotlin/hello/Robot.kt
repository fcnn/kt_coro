package hello

import protofiles.protojava.MessagingProto

class RobotMqttListener: MqttListener {
    override fun onGameContactInfo(msg: MessagingProto.GameContactInfo, uid: Long, mid: Long) {}
}

class Robot constructor(var uid: Long) {
    val lngNW: Double = 113.910938
    val latNW: Double = 22.520323
    val lngSE: Double = 113.952137
    val latSE: Double = 22.484561

    var lng: Double = 0.0
    var lat: Double = 0.0

    var http = Http()
    var mqtt = Mqtt(uid, RobotMqttListener())

    init {
        var r = Math.random()
        lng = lngNW + (lngSE - lngNW) * r
        r = Math.random()
        lat = latNW + (latSE - latNW) * r
        mqtt.start()
    }

    fun run() {
        var msg = MessagingProto.SearchNearbyUsers .newBuilder()
        msg.clientInfoBuilder.uid = uid
        msg.clientInfoBuilder.gpsBuilder.latitude = lat
        msg.clientInfoBuilder.gpsBuilder.longitude = lng
        mqtt.sendLiveMsg(msg.build(), MessagingProto.LiveMessageType.LMT_SEARCH_NEARBY_USERS_VALUE)
        println("robot $uid @ ($lng $lat)")
    }

    fun stop() {
        mqtt.stop()
    }
}