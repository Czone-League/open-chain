package io.openfuture.chain.network.base

import io.netty.channel.ChannelHandlerContext
import io.openfuture.chain.protocol.CommunicationProtocol
import io.openfuture.chain.protocol.CommunicationProtocol.*
import io.openfuture.chain.service.NetworkService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class GetPeersHandler(
    private val networkService: NetworkService
) : BaseHandler(Type.GET_PEERS) {

    override fun packetReceived(ctx: ChannelHandlerContext, message: CommunicationProtocol.Packet) {

        val peers = networkService.getPeers().map { it -> Peer.newBuilder().setHost(it.host).setPort(it.port).build() }

        val response = Packet.newBuilder()
            .setType(Type.PEERS)
            .setPeers(Peers.newBuilder().addAllPeers(peers).build())
            .build()

        ctx.writeAndFlush(response)
    }

}