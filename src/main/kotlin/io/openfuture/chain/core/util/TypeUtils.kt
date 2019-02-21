package io.openfuture.chain.core.util

import io.openfuture.chain.core.model.entity.block.Block
import io.openfuture.chain.core.model.entity.transaction.confirmed.Transaction

typealias TransactionValidateHandler = (tx: Transaction) -> Unit

typealias BlockValidateHandler = (block: Block, lastBlock: Block, new: Boolean) -> Unit