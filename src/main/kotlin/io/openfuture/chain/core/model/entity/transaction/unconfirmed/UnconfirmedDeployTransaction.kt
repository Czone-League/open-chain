package io.openfuture.chain.core.model.entity.transaction.unconfirmed

import io.openfuture.chain.core.model.entity.transaction.TransactionFooter
import io.openfuture.chain.core.model.entity.transaction.TransactionHeader
import io.openfuture.chain.core.model.entity.transaction.payload.DeployTransactionPayload
import io.openfuture.chain.network.message.core.DeployTransactionMessage
import io.openfuture.chain.rpc.domain.transaction.request.DeployTransactionRequest
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "u_deploy_transactions")
class UnconfirmedDeployTransaction(
    header: TransactionHeader,
    footer: TransactionFooter,

    @Embedded
    var payload: DeployTransactionPayload

) : UnconfirmedTransaction(header, footer, payload) {

    companion object {
        fun of(message: DeployTransactionMessage): UnconfirmedDeployTransaction = UnconfirmedDeployTransaction(
            TransactionHeader(message.timestamp, message.fee, message.senderAddress),
            TransactionFooter(message.hash, message.senderSignature, message.senderPublicKey),
            DeployTransactionPayload(message.bytecode)
        )

        fun of(request: DeployTransactionRequest): UnconfirmedDeployTransaction = UnconfirmedDeployTransaction(
                TransactionHeader(request.timestamp!!, request.fee!!, request.senderAddress!!),
                TransactionFooter(request.hash!!, request.senderSignature!!, request.senderPublicKey!!),
                DeployTransactionPayload(request.bytecode!!)
        )
    }


    override fun toMessage(): DeployTransactionMessage = DeployTransactionMessage(
        header.timestamp,
        header.fee,
        header.senderAddress,
        footer.hash,
        footer.senderSignature,
        footer.senderPublicKey,
        payload.bytecode
    )

}