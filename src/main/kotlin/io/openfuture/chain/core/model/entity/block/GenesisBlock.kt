package io.openfuture.chain.core.model.entity.block

import io.openfuture.chain.core.model.entity.Delegate
import io.openfuture.chain.crypto.util.HashUtils
import io.openfuture.chain.crypto.util.SignatureUtils
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils
import javax.persistence.*

@Entity
@Table(name = "genesis_blocks")
class GenesisBlock(
    height: Long,
    previousHash: String,
    timestamp: Long,
    reward: Long,
    publicKey: String,

    @Column(name = "epoch_index", nullable = false)
    var epochIndex: Long,

    @ManyToMany(fetch = FetchType.EAGER, cascade = [(CascadeType.ALL)])
    @JoinTable(name = "delegate2genesis",
        joinColumns = [JoinColumn(name = "genesis_id")],
        inverseJoinColumns = [(JoinColumn(name = "delegate_id"))])
    var activeDelegates: Set<Delegate>

) : BaseBlock(height, previousHash, timestamp, reward, publicKey,
    ByteUtils.toHexString(HashUtils.doubleSha256((previousHash + timestamp + height + publicKey).toByteArray()))
) {

    override fun sign(privateKey: ByteArray): GenesisBlock {
        this.signature = SignatureUtils.sign((previousHash + timestamp + height).toByteArray(), privateKey)
        return this
    }

}