package com.keyserdsoze.cardreader.data

import com.keyserdsoze.cardreader.model.CardEntry
import com.keyserdsoze.cardreader.model.CardTombstone
import com.keyserdsoze.cardreader.model.CodeFormat
import com.keyserdsoze.cardreader.model.WalletDocument
import org.junit.Assert.assertEquals
import org.junit.Test

class CardJsonCodecTest {
    @Test
    fun roundTripPreservesWallet() {
        val original = WalletDocument(
            cards = listOf(CardEntry("a", "Supermarket", "1234567890128", CodeFormat.EAN_13, updatedAt = 100, writerId = "phone-a")),
            tombstones = listOf(CardTombstone("deleted", 90, "phone-b")),
        )
        assertEquals(original, CardJsonCodec.decode(CardJsonCodec.encode(original)))
    }
}
