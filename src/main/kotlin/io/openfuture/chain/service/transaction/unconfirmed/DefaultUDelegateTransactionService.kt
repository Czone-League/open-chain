package io.openfuture.chain.service.transaction.unconfirmed

import io.openfuture.chain.domain.rpc.transaction.DelegateTransactionRequest
import io.openfuture.chain.domain.transaction.DelegateTransactionDto
import io.openfuture.chain.domain.transaction.data.DelegateTransactionData
import io.openfuture.chain.entity.transaction.DelegateTransaction
import io.openfuture.chain.entity.transaction.unconfirmed.UDelegateTransaction
import io.openfuture.chain.repository.UDelegateTransactionRepository
import io.openfuture.chain.service.UDelegateTransactionService
import org.springframework.stereotype.Service

@Service
class DefaultUDelegateTransactionService(
    repository: UDelegateTransactionRepository
) : DefaultUTransactionService<DelegateTransaction, UDelegateTransaction, DelegateTransactionData, DelegateTransactionDto, DelegateTransactionRequest>(repository),
    UDelegateTransactionService