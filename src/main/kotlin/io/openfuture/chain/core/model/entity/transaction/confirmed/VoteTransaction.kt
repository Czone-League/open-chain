package io.openfuture.chain.core.model.entity.transaction.confirmed

import io.openfuture.chain.core.model.entity.block.MainBlock
import io.openfuture.chain.core.model.entity.transaction.TransactionHeader
import io.openfuture.chain.core.model.entity.transaction.payload.VoteTransactionPayload
import io.openfuture.chain.core.model.entity.transaction.unconfirmed.UnconfirmedVoteTransaction
import io.openfuture.chain.network.message.core.VoteTransactionMessage
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "vote_transactions")
class VoteTransaction(
    header: TransactionHeader,
    hash: String,
    senderSignature: String,
    senderPublicKey: String,
    block: MainBlock,

    @Embedded
    val payload: VoteTransactionPayload

) : Transaction(header, hash, senderSignature, senderPublicKey, block) {

    companion object {
        fun of(message: VoteTransactionMessage, block: MainBlock): VoteTransaction = VoteTransaction(
            TransactionHeader(message.timestamp, message.fee, message.senderAddress),
            message.hash,
            message.senderSignature,
            message.senderPublicKey,
            block,
            VoteTransactionPayload(message.voteTypeId, message.delegateKey)
        )

        fun of(utx: UnconfirmedVoteTransaction, block: MainBlock): VoteTransaction = VoteTransaction(
            TransactionHeader(utx.header.timestamp, utx.header.fee, utx.header.senderAddress),
            utx.hash,
            utx.senderSignature,
            utx.senderPublicKey,
            block,
            utx.payload
        )
    }

    override fun toMessage(): VoteTransactionMessage = VoteTransactionMessage (
        header.timestamp,
        header.fee,
        header.senderAddress,
        hash,
        senderSignature,
        senderPublicKey,
        payload.voteTypeId,
        payload.delegateKey
    )

}