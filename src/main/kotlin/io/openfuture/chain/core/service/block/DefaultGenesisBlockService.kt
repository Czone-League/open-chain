package io.openfuture.chain.core.service.block

import io.openfuture.chain.consensus.property.ConsensusProperties
import io.openfuture.chain.core.component.NodeKeyHolder
import io.openfuture.chain.core.exception.NotFoundException
import io.openfuture.chain.core.model.entity.Delegate
import io.openfuture.chain.core.model.entity.block.GenesisBlock
import io.openfuture.chain.core.model.entity.block.payload.GenesisBlockPayload
import io.openfuture.chain.core.repository.BlockRepository
import io.openfuture.chain.core.service.BlockService
import io.openfuture.chain.core.service.DefaultDelegateService
import io.openfuture.chain.core.service.GenesisBlockService
import io.openfuture.chain.core.util.BlockUtils
import io.openfuture.chain.crypto.util.SignatureUtils
import io.openfuture.chain.network.component.node.NodeClock
import io.openfuture.chain.network.message.core.GenesisBlockMessage
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DefaultGenesisBlockService(
    blockService: BlockService,
    private val repository: BlockRepository<GenesisBlock>,
    private val delegateService: DefaultDelegateService,
    private val clock: NodeClock,
    private val consensusProperties: ConsensusProperties,
    private val keyHolder: NodeKeyHolder
) : BaseBlockService(blockService), GenesisBlockService {

    @Transactional(readOnly = true)
    override fun getLast(): GenesisBlock = repository.findFirstByOrderByHeightDesc()
        ?: throw NotFoundException("Last block not found")

    override fun create(): GenesisBlock {
        val timestamp = clock.networkTime()
        val lastBlock = blockService.getLast()
        val height = lastBlock.height + 1
        val previousHash = lastBlock.hash
        val reward = consensusProperties.rewardBlock!!
        val payload = createPayload()
        val hash = BlockUtils.createHash(timestamp, height, previousHash, reward, payload)
        val signature = SignatureUtils.sign(hash, keyHolder.getPrivateKey())
        val publicKey = ByteUtils.toHexString(keyHolder.getPublicKey())

        return GenesisBlock(timestamp, height, previousHash, reward, ByteUtils.toHexString(hash), signature, publicKey, payload)
    }

    private fun createPayload(): GenesisBlockPayload {
        val epochIndex = getLast().payload.epochIndex + 1
        val delegates = delegateService.getActiveDelegates()
        return GenesisBlockPayload(epochIndex, delegates)
    }

    @Transactional
    override fun add(message: GenesisBlockMessage) {
        if (null != repository.findOneByHash(message.hash)) {
            return
        }

        val block = GenesisBlock.of(message)
        val delegates = message.delegates.map { delegateService.getByPublicKey(it) }

        if (!isValid(block, delegates)) {
            return
        }

        block.payload.activeDelegates = delegates
        repository.save(block)
        // todo broadcast
    }

    @Transactional(readOnly = true)
    override fun isValid(message: GenesisBlockMessage): Boolean {
        val block = GenesisBlock.of(message)
        val delegates = message.delegates.map { delegateService.getByPublicKey(it) }
        return isValid(block, delegates)
    }

    private fun isValid(block: GenesisBlock, delegates: List<Delegate>): Boolean {
        val lastGenesisBlock = this.getLast()
        return isValidateActiveDelegates(delegates)
            && isValidEpochIndex(lastGenesisBlock.payload.epochIndex, block.payload.epochIndex)
            && super.isValid(block)
    }

    private fun isValidateActiveDelegates(delegates: List<Delegate>): Boolean {
        val persistDelegates = delegateService.getActiveDelegates()
        if (delegates.size != persistDelegates.size) {
            return false
        }

        val persistPublicKeys = persistDelegates.map { it.publicKey }
        val publicKeys = delegates.map { it.publicKey }

        return persistPublicKeys.containsAll(publicKeys)
    }

    private fun isValidEpochIndex(lastEpoch: Long, newEpoch: Long): Boolean {
        return (lastEpoch + 1 == newEpoch)
    }

}