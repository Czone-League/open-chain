package io.openfuture.chain.network.service.message

import io.netty.channel.ChannelHandlerContext
import io.openfuture.chain.network.message.network.GreetingMessage
import io.openfuture.chain.network.message.network.address.NetworkAddressMessage
import io.openfuture.chain.network.property.NodeProperties
import io.openfuture.chain.network.service.ConnectionService
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class GreetingMessageService(
    private val service: ConnectionService,
    private val properties: NodeProperties
) {

    fun onChannelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(GreetingMessage(NetworkAddressMessage(properties.host!!, properties.port!!)))
        ctx.fireChannelActive()
    }

    fun onGreeting(ctx: ChannelHandlerContext, message: GreetingMessage) {
        service.addConnection(ctx.channel(), message.address)
    }

    fun onChannelInactive(ctx: ChannelHandlerContext) {
        service.removeConnection(ctx.channel())
        ctx.fireChannelInactive()
    }

}