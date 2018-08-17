package io.openfuture.chain.rpc.controller.transaction

import io.openfuture.chain.config.ControllerTests
import io.openfuture.chain.core.model.entity.block.MainBlock
import io.openfuture.chain.core.model.entity.block.payload.MainBlockPayload
import io.openfuture.chain.core.model.entity.transaction.confirmed.DelegateTransaction
import io.openfuture.chain.core.model.entity.transaction.payload.DelegateTransactionPayload
import io.openfuture.chain.core.model.entity.transaction.unconfirmed.UnconfirmedDelegateTransaction
import io.openfuture.chain.core.service.DelegateTransactionService
import io.openfuture.chain.rpc.domain.transaction.request.DelegateTransactionRequest
import io.openfuture.chain.rpc.domain.transaction.response.DelegateTransactionResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.BDDMockito.given
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import reactor.core.publisher.Mono


@WebFluxTest(DelegateTransactionController::class)
class DelegateTransactionControllerTests : ControllerTests() {

    @MockBean
    private lateinit var service: DelegateTransactionService

    companion object {
        private const val DELEGATE_TRANSACTION_URL = "/rpc/transactions/delegate"
    }


    @Test
    fun addTransactionShouldReturnAddedTransaction() {
        val transactionRequest = DelegateTransactionRequest(1L, 1L, "senderAddress", "senderPublicKey", "senderSignature",
            "delegateKey")
        val unconfirmedDelegateTransaction = UnconfirmedDelegateTransaction(1L, 1L, "senderAddress", "senderPublicKey", "senderSignature",
            "hash", DelegateTransactionPayload("delegateKey"))
        val expectedResponse = DelegateTransactionResponse(unconfirmedDelegateTransaction)

        given(service.add(transactionRequest)).willReturn(unconfirmedDelegateTransaction)

        val actualResponse = webClient.post().uri(DELEGATE_TRANSACTION_URL)
            .body(Mono.just(transactionRequest), DelegateTransactionRequest::class.java)
            .exchange()
            .expectStatus().isOk
            .expectBody(DelegateTransactionResponse::class.java)
            .returnResult().responseBody!!

        assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse)
    }

    @Test
    fun getTransactionByHashShouldReturnTransaction() {
        val hash = "hash"
        val mainBlock = MainBlock(1, 1, "previousHash", 1, "hash", "signature", "publicKey", MainBlockPayload("merkleHash")).apply { id = 1 }
        val expectedTransaction = DelegateTransaction(1L, 1L, "senderAddress", "hash",
            "senderSignature", "senderPublicKey", mainBlock, DelegateTransactionPayload("delegateKey")).apply { id = 1 }

        given(service.getByHash(hash)).willReturn(expectedTransaction)

        val actualTransaction = webClient.get().uri("$DELEGATE_TRANSACTION_URL/$hash")
            .exchange()
            .expectStatus().isOk
            .expectBody(DelegateTransaction::class.java)
            .returnResult().responseBody!!

        assertThat(actualTransaction).isEqualTo(expectedTransaction)
    }

}

