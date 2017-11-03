package hello

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.util.CharsetUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.net.InetSocketAddress

import com.google.protobuf.Message
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import protofiles.protojava.MessagingProto
import protofiles.protojava.CommonProtos as CommonProto

internal class UdpClientHandler : SimpleChannelInboundHandler<DatagramPacket>() {
    @Throws(Exception::class)
    public override fun messageReceived(channelHandlerContext: ChannelHandlerContext,
                                        packet: DatagramPacket) {
        val size = packet.content().array().size

        println("udp response size $size")
        //channelHandlerContext.close()
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
        cause.printStackTrace()
    }
}

class UdpNetty {
    private val channel_ : Channel
    private val group_ = NioEventLoopGroup()
    private val addr_ = InetSocketAddress("u.codein.net", 1000)
    init {
        val b = Bootstrap()
        b.group(group_).channel(NioDatagramChannel::class.java)
                //.option(ChannelOption.SO_BROADCAST,true)
                .handler(UdpClientHandler())

        channel_ = b.bind(0).sync().channel()
    }
    fun close() {
        group_.shutdownGracefully()
        if (!channel_.closeFuture().await(100)) {
            println("关闭超时！！！")
        }
    }
    @Throws(Exception::class)
    fun sendMsg(msg: Message, type: Int, id: Long = 0) {
        var body = msg.toByteArray()
        val payload = ByteArray(size = body.size + 10)
        payload[0] = ((type shr 7) and 0xff).toByte()
        payload[1] = (type and 0xff).toByte()
        payload[2] = (id shr 56).toByte()
        payload[3] = (id shr 48).toByte()
        payload[4] = (id shr 40).toByte()
        payload[5] = (id shr 32).toByte()
        payload[6] = (id shr 24).toByte()
        payload[7] = (id shr 16).toByte()
        payload[8] = (id shr 8).toByte()
        payload[9] = id.toByte()

        (0 until body.size).forEach {
            payload[it + 10] = body[it]
        }

        var packet = DatagramPacket(Unpooled.copiedBuffer(payload), addr_)
        try {
            channel_.writeAndFlush( packet).sync()

        } finally {
        }
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            var b = MessagingProto.GameLiveStateInfo.newBuilder()
            b.gameId = 1234L
            b.state = MessagingProto.GameLiveState.game_state_ready
            val c = b.clientInfoBuilder
            c.uid = 8000000001L
            c.version = 1000000300000L
            c.type = CommonProto.ClientType.IOS
            c.oem = "Kotlin"
            c.gpsBuilder.longitude = 113.12345678
            c.gpsBuilder.latitude = 22.87654321
            c.deviceId = "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk"
            c.token = "oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo"

            val msg = b.build()
            val str = msg.toString()
            println("msg -> $str")

            UdpNetty().sendMsg(msg, MessagingProto.LiveMessageType.LMT_GAME_LIVE_STATE_VALUE, 123L)
            runBlocking {
                delay(1000)
            }
        }
    }
}
