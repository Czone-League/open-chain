package io.openfuture.chain.block.validation

import io.openfuture.chain.config.ServiceTests
import io.openfuture.chain.entity.block.MainBlock
import io.openfuture.chain.entity.transaction.VoteTransaction
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class MainBlockValidatorTests : ServiceTests() {

    private lateinit var mainBlockValidator: MainBlockValidator

    @Before
    fun setUp() {
        mainBlockValidator = MainBlockValidator()
    }

    @Test
    fun isValidShouldReturnTrue() {
        val block = MainBlock(
            ByteArray(1),
            123,
            "prev_block_hash",
            "0e09773036394004cb8c340e639a89d7a18e924e8a3d048b49864aeb017e07a0",
            1512345678L,
            mutableListOf(
                VoteTransaction(
                    1500000000L,
                    1000L,
                    "recipient_address",
                    "sender_key",
                    "sender_address",
                    "sender_signature",
                    "hash",
                    1,
                    "delegate_key"
                ),
                VoteTransaction(
                    1500000001L,
                    1002L,
                    "recipient_address2",
                    "sender_key2",
                    "sender_address2",
                    "sender_signature2",
                    "hash2",
                    2,
                    "delegate_key2"
                )
            )
        )

        val isBlockValid = mainBlockValidator.isValid(block)

        Assertions.assertThat(isBlockValid).isTrue()
    }

    @Test
    fun isValidShouldReturnFalse() {
        val block = MainBlock(
            ByteArray(1),
            123,
            "prev_block_hash",
            "0000000000000000000000000000000000000000000000000000000000000000",
            1512345678L,
            mutableListOf(
                VoteTransaction(
                    1500000000L,
                    1000L,
                    "recipient_address",
                    "sender_key",
                    "sender_address",
                    "sender_signature",
                    "hash",
                    1,
                    "delegate_key"
                ),
                VoteTransaction(
                    1500000001L,
                    1002L,
                    "recipient_address2",
                    "sender_key2",
                    "sender_address2",
                    "sender_signature2",
                    "hash2",
                    2,
                    "delegate_key2"
                )
            )
        )

        val isBlockValid = mainBlockValidator.isValid(block)

        Assertions.assertThat(isBlockValid).isFalse()
    }
}