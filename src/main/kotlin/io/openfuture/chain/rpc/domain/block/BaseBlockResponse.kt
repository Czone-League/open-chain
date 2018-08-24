package io.openfuture.chain.rpc.domain.block

abstract class BaseBlockResponse(
    val timestamp: Long,
    val height: Long,
    val previousHash: String,
    val reward: Long,
    val hash: String,
    val signature: String,
    val publicKey: String
)