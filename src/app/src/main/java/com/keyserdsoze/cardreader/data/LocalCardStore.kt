package com.keyserdsoze.cardreader.data

import android.content.Context
import com.keyserdsoze.cardreader.model.CardEntry
import com.keyserdsoze.cardreader.model.CardTombstone
import com.keyserdsoze.cardreader.model.CodeFormat
import com.keyserdsoze.cardreader.model.WalletDocument
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InstallationIdStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "cardreader-writer-id")

    @Synchronized
    fun get(): String {
        val current = runCatching { file.readText().trim() }.getOrDefault("")
        if (current.isNotBlank()) return current
        val generated = UUID.randomUUID().toString()
        file.parentFile?.mkdirs()
        file.writeText(generated)
        return generated
    }
}

class LocalCardStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    val writerId: String = InstallationIdStore(context).get()
    private val _document = MutableStateFlow(load())
    val document: StateFlow<WalletDocument> = _document.asStateFlow()

    @Synchronized
    fun add(
        name: String,
        value: String,
        format: CodeFormat,
        colorArgb: Int,
        notes: String = "",
        now: Long = System.currentTimeMillis(),
    ): CardEntry {
        val card = CardEntry(
            name = name.trim(),
            value = value,
            format = format,
            colorArgb = colorArgb,
            notes = notes.trim(),
            updatedAt = now,
            writerId = writerId,
        )
        replace(_document.value.copy(cards = _document.value.cards + card))
        return card
    }

    @Synchronized
    fun update(card: CardEntry, now: Long = System.currentTimeMillis()) {
        val updated = card.copy(updatedAt = now, writerId = writerId)
        replace(_document.value.copy(
            cards = _document.value.cards.map { if (it.id == card.id) updated else it },
            tombstones = _document.value.tombstones.filterNot { it.id == card.id },
        ))
    }

    @Synchronized
    fun delete(id: String, now: Long = System.currentTimeMillis()) {
        if (_document.value.cards.none { it.id == id }) return
        val tombstone = CardTombstone(id, now, writerId)
        replace(_document.value.copy(
            cards = _document.value.cards.filterNot { it.id == id },
            tombstones = _document.value.tombstones.filterNot { it.id == id } + tombstone,
        ))
    }

    @Synchronized
    fun replace(document: WalletDocument) {
        val normalized = document.copy(
            cards = document.cards.sortedWith(compareByDescending<CardEntry> { it.favorite }.thenBy { it.name.lowercase() }),
            tombstones = document.tombstones.sortedBy { it.id },
        )
        preferences.edit().putString(KEY_DOCUMENT, CardJsonCodec.encode(normalized)).commit()
        _document.value = normalized
    }

    private fun load(): WalletDocument = runCatching {
        CardJsonCodec.decode(preferences.getString(KEY_DOCUMENT, null).orEmpty())
    }.getOrDefault(WalletDocument())

    private companion object {
        const val PREFERENCES = "cardreader_wallet"
        const val KEY_DOCUMENT = "wallet_document"
    }
}
