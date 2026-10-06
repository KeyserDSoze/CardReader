package com.keyserdsoze.cardreader.data.sync

import com.keyserdsoze.cardreader.model.CardEntry
import com.keyserdsoze.cardreader.model.CardTombstone
import com.keyserdsoze.cardreader.model.CodeFormat
import com.keyserdsoze.cardreader.model.WalletDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardMergeEngineTest {
    @Test
    fun newerEditWinsAcrossDevices() {
        val local = WalletDocument(cards = listOf(card("Local", 10, "a")))
        val remote = WalletDocument(cards = listOf(card("Remote", 20, "b")))
        assertEquals("Remote", CardMergeEngine.merge(local, remote).cards.single().name)
    }

    @Test
    fun newerDeletePreventsOfflineResurrection() {
        val local = WalletDocument(cards = listOf(card("Old", 10, "a")))
        val remote = WalletDocument(tombstones = listOf(CardTombstone("id", 20, "b")))
        val merged = CardMergeEngine.merge(local, remote)
        assertTrue(merged.cards.isEmpty())
        assertEquals("id", merged.tombstones.single().id)
    }

    @Test
    fun independentCardsAreUnioned() {
        val local = WalletDocument(cards = listOf(card("One", 10, "a", "1")))
        val remote = WalletDocument(cards = listOf(card("Two", 10, "b", "2")))
        assertEquals(setOf("1", "2"), CardMergeEngine.merge(local, remote).cards.map { it.id }.toSet())
    }

    private fun card(name: String, time: Long, writer: String, id: String = "id") = CardEntry(
        id = id,
        name = name,
        value = "123",
        format = CodeFormat.CODE_128,
        updatedAt = time,
        writerId = writer,
    )
}
