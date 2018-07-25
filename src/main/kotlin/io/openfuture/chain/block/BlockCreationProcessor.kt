package io.openfuture.chain.block

import io.openfuture.chain.block.validation.BlockValidationProvider
import io.openfuture.chain.component.converter.transaction.impl.RewardTransactionEntityConverter
import io.openfuture.chain.component.node.NodeClock
import io.openfuture.chain.crypto.key.NodeKeyHolder
import io.openfuture.chain.crypto.signature.SignatureManager
import io.openfuture.chain.crypto.util.HashUtils
import io.openfuture.chain.domain.block.BlockCreationEvent
import io.openfuture.chain.domain.block.PendingBlock
import io.openfuture.chain.domain.block.Signature
import io.openfuture.chain.domain.transaction.data.RewardTransactionData
import io.openfuture.chain.entity.block.Block
import io.openfuture.chain.entity.block.BlockType
import io.openfuture.chain.entity.block.GenesisBlock
import io.openfuture.chain.entity.block.MainBlock
import io.openfuture.chain.entity.transaction.Transaction
import io.openfuture.chain.property.ConsensusProperties
import io.openfuture.chain.service.BlockService
import io.openfuture.chain.service.ConsensusService
import io.openfuture.chain.service.DelegateService
import io.openfuture.chain.util.BlockUtils
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BlockCreationProcessor(
    private val service: BlockService,
    private val signatureCollector: SignatureCollector,
    private val keyHolder: NodeKeyHolder,
    private val validationService: BlockValidationProvider,
    private val consensusService: ConsensusService,
    private val clock: NodeClock,
    private val delegateService: DelegateService,
    private val rewardTransactionEntityConverter: RewardTransactionEntityConverter,
    private val consensusProperties: ConsensusProperties
) {

    fun approveBlock(pendingBlock: PendingBlock): PendingBlock {
        val block = pendingBlock.block

        if (!validationService.isValid(block)) {
            throw IllegalArgumentException("Inbound block is not valid")
        }

        val hash = HashUtils.fromHexString(block.hash)
        val key = HashUtils.fromHexString(pendingBlock.signature.publicKey)
        if (!SignatureManager.verify(hash, pendingBlock.signature.value, key)) {
            throw IllegalArgumentException("Inbound block's signature is invalid")
        }

        if (!signatureCollector.addBlockSignature(pendingBlock)) {
            throw IllegalArgumentException("Either signature is already exists, or not related to pending block")
        }
        return signCreatedBlock(block)
    }

    @EventListener
    fun fireBlockCreation(event: BlockCreationEvent) {
        val previousBlock = service.getLastMain()
        val genesisBlock = service.getLastGenesis()
        val nextProducer = BlockUtils.getBlockProducer(genesisBlock.activeDelegates, previousBlock)
        if (HashUtils.toHexString(keyHolder.getPublicKey()) == nextProducer.publicKey) {
            create(event.pendingTransactions, previousBlock, genesisBlock)
        }
    }

    private fun signCreatedBlock(block: Block): PendingBlock {
        val publicKey = HashUtils.toHexString(keyHolder.getPublicKey())
        val value = SignatureManager.sign(HashUtils.fromHexString(block.hash), keyHolder.getPrivateKey())
        val signature = Signature(value, publicKey)
        val pendingBlock = PendingBlock(block, signature)
        signatureCollector.setPendingBlock(pendingBlock)
        return pendingBlock
    }

    private fun create(transactionsFromPool: MutableList<Transaction>, previousBlock: Block, genesisBlock: GenesisBlock) {
        val blockType = if (consensusService.isGenesisBlockNeeded()) BlockType.GENESIS else BlockType.MAIN

        val time = clock.networkTime()
        val privateKey = keyHolder.getPrivateKey()
        val block = when (blockType) {
            BlockType.MAIN -> {
                val transactions = prepareTransactions(transactionsFromPool)

                MainBlock(
                    privateKey,
                    previousBlock.height + 1,
                    previousBlock.hash,
                    BlockUtils.calculateMerkleRoot(transactions),
                    time,
                    transactions
                )
            }
            BlockType.GENESIS -> {
                GenesisBlock(
                    privateKey,
                    previousBlock.height + 1,
                    previousBlock.hash,
                    time,
                    genesisBlock.epochIndex + 1,
                    delegateService.getActiveDelegates()
                )
            }
        }

        signCreatedBlock(block)
    }

    private fun prepareTransactions(transactionsFromPool: MutableList<Transaction>): MutableList<Transaction> {
        val fees = transactionsFromPool.map { it.fee }.sum()
        val delegate = delegateService.getByPublicKey(HashUtils.toHexString(keyHolder.getPublicKey()))
        val rewardTransactionData = RewardTransactionData((fees + consensusProperties.rewardBlock!!),
            consensusProperties.feeRewardTx!!, delegate.address, consensusProperties.genesisAddress!!)

        val rewardTransaction = rewardTransactionEntityConverter.toEntity(clock.networkTime(), rewardTransactionData)

        return mutableListOf(rewardTransaction, *transactionsFromPool.toTypedArray())
    }

}