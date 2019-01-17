package io.openfuture.chain.core.model.entity.state.payload

import io.openfuture.chain.core.util.ByteConstants.LONG_BYTES
import java.nio.ByteBuffer
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class WalletPayload(

    @Column(name = "balance", nullable = false)
    val balance: Long = 0

) : StatePayload {

    override fun getBytes(): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(balance).array()

}