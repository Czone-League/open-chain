package io.openfuture.chain.network.property

import io.openfuture.chain.network.domain.NetworkAddress
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Component
@Validated
@ConfigurationProperties(value = "node")
class NodeProperties(

    /** Node Server Host */
    @field:NotNull
    var host: String? = null,

    /** Node Server Port */
    @field:NotNull
    var port: Int? = null,

    /** Root Nodes List */
    @field:NotEmpty
    @field:Size(min = 1, max = 8)
    var rootNodes: List<String> = emptyList(),

    /** Node Communication Protocol Version */
    @field:NotNull
    var version: String? = null,

    /** */
    @field:NotNull
    var bossCount: Int? = null,

    /** */
    @field:NotNull
    var backlog: Int? = null,

    /** */
    @field:NotNull
    var keepAlive: Boolean? = null,

    /** */
    @field:NotNull
    var connectionTimeout: Int? = null,

    /** Directly Connected Peers Number */
    @field:NotNull
    @field:Min(1)
    var peersNumber: Int? = null,

    /** Private key path */
    @field:NotNull
    var privateKeyPath: String? = null,

    /** Public key path */
    @field:NotNull
    var publicKeyPath: String? = null

) {

    fun getRootAddresses(): List<NetworkAddress> = rootNodes.map {
        val split = it.split(':')
        NetworkAddress(split[0], split[1].toInt())
    }

}