package io.openfuture.chain.nio.client.handler

import io.netty.channel.ChannelHandlerContext
import io.openfuture.chain.component.node.NodeClock
import io.openfuture.chain.nio.base.BaseHandler
import io.openfuture.chain.protocol.CommunicationProtocol
import io.openfuture.chain.protocol.CommunicationProtocol.Packet
import io.openfuture.chain.protocol.CommunicationProtocol.TimeSyncRequest
import io.openfuture.chain.protocol.CommunicationProtocol.Type.TIME_SYNC_REQUEST
import io.openfuture.chain.protocol.CommunicationProtocol.Type.TIME_SYNC_RESPONSE
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class TimeSyncClientHandler(
        private val clock: NodeClock
) : BaseHandler(TIME_SYNC_RESPONSE) {

    companion object {
        private val log = LoggerFactory.getLogger(TimeSyncClientHandler::class.java)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        val request = Packet.newBuilder()
                .setType(TIME_SYNC_REQUEST)
                .setTimeSyncRequest(TimeSyncRequest.newBuilder()
                        .setNodeTimestamp(clock.nodeTime())
                        .build())
                .build()
        ctx.writeAndFlush(request)

        log.info("Message $TIME_SYNC_REQUEST was sent to ${ctx.channel().remoteAddress()}")

        ctx.fireChannelActive()
    }

    override fun packetReceived(ctx: ChannelHandlerContext, message: Packet) {
        log.info("Message $TIME_SYNC_RESPONSE received from ${ctx.channel().remoteAddress()}")

        val offset = calculateTimeOffset(message.timeSyncResponse)
        clock.addTimeOffset(ctx.channel().remoteAddress().toString(), offset)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        clock.removeTimeOffset(ctx.channel().remoteAddress().toString())
        ctx.fireChannelInactive()
    }

    fun calculateTimeOffset(response: CommunicationProtocol.TimeSyncResponse): Long {
        val networkLatency = (clock.nodeTime() - response.nodeTimestamp) / 2
        val expectedNetworkTimestamp = response.nodeTimestamp + networkLatency
        return response.networkTimestamp - expectedNetworkTimestamp
    }

}