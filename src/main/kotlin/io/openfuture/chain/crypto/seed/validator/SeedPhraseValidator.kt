package io.openfuture.chain.crypto.seed.validator

import io.openfuture.chain.crypto.seed.SeedConstant
import io.openfuture.chain.crypto.seed.SeedConstant.BYTE_SIZE
import io.openfuture.chain.crypto.seed.SeedConstant.MULTIPLICITY_VALUE
import io.openfuture.chain.crypto.seed.SeedConstant.SECOND_BYTE_OFFSET
import io.openfuture.chain.crypto.seed.SeedConstant.SECOND_BYTE_SKIP_BIT_SIZE
import io.openfuture.chain.crypto.seed.SeedConstant.THIRD_BYTE_OFFSET
import io.openfuture.chain.crypto.seed.SeedConstant.WORD_INDEX_SIZE
import io.openfuture.chain.entity.SeedWord
import io.openfuture.chain.exception.SeedValidationException
import io.openfuture.chain.repository.SeedWordRepository
import io.openfuture.chain.util.HashUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

@Component
class SeedPhraseValidator(
        private val seedWordRepository: SeedWordRepository
) {

    companion object {
        private const val MULTIPLICITY_WITH_CHECKSUM_VALUE = 33
        private const val MAX_BYTE_SIZE_MOD = 7
        private const val OUT_OF_BYTE_SIZE = WORD_INDEX_SIZE - BYTE_SIZE
        private const val MIN_FIRST_BIT_INDEX_IN_THREE_BYTE = 6
        private const val THIRD_BYTE_SKIP_BIT_SIZE = 13
    }

    fun validate(seedPhrase: String) {
        val seedPhraseWords = seedPhrase.split(StringUtils.SPACE)
        val seedWordIndexes = seedPhraseWords.map { getWord(it).index }.toIntArray()
        validate(seedWordIndexes)
    }

    private fun validate(seedWordIndexes: IntArray) {
        val seedWordSize = seedWordIndexes.size
        val entropyPlusChecksumSize = seedWordSize * WORD_INDEX_SIZE
        val entropySize = entropyPlusChecksumSize * MULTIPLICITY_VALUE / MULTIPLICITY_WITH_CHECKSUM_VALUE
        val checksumSize = entropySize / MULTIPLICITY_VALUE

        if (entropyPlusChecksumSize != entropySize + checksumSize) {
            throw SeedValidationException("Invalid word count = $seedWordSize")
        }

        val entropyWithChecksum = ByteArray((entropyPlusChecksumSize + MAX_BYTE_SIZE_MOD) / BYTE_SIZE)
        wordIndexesToEntropyWithCheckSum(seedWordIndexes, entropyWithChecksum)
        val entropy = Arrays.copyOf(entropyWithChecksum, entropyWithChecksum.size - 1)
        val entropySha = HashUtils.sha256(entropy)[0]
        val mask = ((1 shl (BYTE_SIZE - checksumSize)) - 1).inv().toByte()

        val lastByte = entropyWithChecksum[entropyWithChecksum.size - 1]
        if (entropySha xor lastByte and mask != 0.toByte()) {
            throw SeedValidationException("Invalid checksum for seed phrase")
        }
    }

    private fun getWord(wordValue: String): SeedWord {
        return seedWordRepository.findOneByValue(wordValue).orElseThrow {
            throw SeedValidationException("Word $wordValue not found")
        }
    }

    private fun wordIndexesToEntropyWithCheckSum(wordIndexes: IntArray, entropyWithChecksum: ByteArray) {
        var wordIndex = 0
        var entropyOffset = 0
        while (wordIndex < wordIndexes.size) {
            writeNextWordIndexToArray(entropyWithChecksum, wordIndexes[wordIndex], entropyOffset)
            wordIndex++
            entropyOffset += WORD_INDEX_SIZE
        }
    }

    /**
     * Method adds [WORD_INDEX_SIZE] last bits to [bytes] from [offset]
     *
     * @bytes byte array to fill it from [offset] bit to [offset] + [WORD_INDEX_SIZE] bit with value [WORD_INDEX_SIZE]
     * last bits
     * @value value to fill it to [bytes]
     * @offset bit index in [bytes] array from which it value will be written to [bytes]
     */
    private fun writeNextWordIndexToArray(bytes: ByteArray, value: Int, offset: Int) {
        val byteSkip = offset / BYTE_SIZE
        val bitSkip = offset % BYTE_SIZE

        writeFirstByteToArray(bytes, byteSkip, bitSkip, value)
        writeSecondByteToArray(bytes, byteSkip, bitSkip, value)
        writeThirdByteToArray(bytes, byteSkip, bitSkip, value)
    }

    /**
     * Writes to [byteSkip] byte of [bytes] part of value with [SeedConstant.BYTE_SIZE] - bitSkip bits
     * from [bitSkip] bit to byte edge of [byteSkip] byte
     *
     * firstValue is byte value to fill it from byteSkip bit to byte edge bit with first part of [value]
     * toWrite the value where it shift right by bytes [OUT_OF_BYTE_SIZE] with [bitSkip] to get the byte value of first
     * byte
     * Next the computed value is added to firstValue and assigned to byte in array
     *
     * @param bytes byte array to fill [byteSkip] byte with [SeedConstant.BYTE_SIZE] - bitSkip bits of [value]
     * @param byteSkip byte index in [bytes] array from which value will be written to [bytes]
     * @param bitSkip bit index of [byteSkip] in [bytes] array from which value will be written to [bytes]
     * @param value value to write part of it to [bytes] array
     */
    private fun writeFirstByteToArray(bytes: ByteArray, byteSkip: Int, bitSkip: Int, value: Int) {
        val firstValue = bytes[byteSkip]
        val toWrite = (value shr (OUT_OF_BYTE_SIZE + bitSkip)).toByte()
        bytes[byteSkip] = (firstValue or toWrite)
    }

    /**
     * Writes to [byteSkip] + 1 byte of [bytes] part of value with [SeedConstant.BYTE_SIZE] - bitSkip bits
     * from 0 bit to [WORD_INDEX_SIZE] - bits size written in previous byte, but no more than [SeedConstant.BYTE_SIZE]
     * bits
     *
     * firstValue is byte value to fill it from 0 bit to WORD_INDEX_SIZE - [SeedConstant.BYTE_SIZE] + bitSkip bit with
     * value from [value] parameter
     *
     *
     * @param bytes byte array to fill [byteSkip] + 1 byte from [value]
     * @param byteSkip previous byte index in [bytes] array from which value will be written to [bytes]
     * @param bitSkip bit index of [byteSkip] in [bytes] array from which value will be written to [bytes]
     * @param value value to write part of it to [bytes] array
     */
    private fun writeSecondByteToArray(bytes: ByteArray, byteSkip: Int, bitSkip: Int, value: Int) {
        val valueInByte = bytes[byteSkip + 1]
        val i = SECOND_BYTE_SKIP_BIT_SIZE - bitSkip
        val toWrite = (if (i > 0) value shl i else value shr -i).toByte()
        bytes[byteSkip + SECOND_BYTE_OFFSET] = (valueInByte or toWrite)
    }

    /**
     * Writes to [byteSkip] + 2 byte of [bytes] first part of value if value wasn't written fully in previous bytes
     *
     * @param bytes byte array to fill [byteSkip] + 2 byte from [value] if [value] wasn't written fully in previous
     * bytes
     * @param byteSkip before previous byte index in [bytes] array from which value will be written to [bytes]
     * @param bitSkip bit index of [byteSkip] in [bytes] array from which value will be written to [bytes]
     * @param value value to write part of it to [bytes] array
     */
    private fun writeThirdByteToArray(bytes: ByteArray, byteSkip: Int, bitSkip: Int, value: Int) {
        if (bitSkip >= MIN_FIRST_BIT_INDEX_IN_THREE_BYTE) {
            val lastByteIndex = byteSkip + THIRD_BYTE_OFFSET
            val lastByteValue = bytes[lastByteIndex]
            val toWrite = (value shl (THIRD_BYTE_SKIP_BIT_SIZE - bitSkip)).toByte()
            bytes[lastByteIndex] = (lastByteValue or toWrite)
        }
    }

}
