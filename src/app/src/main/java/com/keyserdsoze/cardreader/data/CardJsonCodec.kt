package com.keyserdsoze.cardreader.data

import com.keyserdsoze.cardreader.model.CardEntry
import com.keyserdsoze.cardreader.model.CardTombstone
import com.keyserdsoze.cardreader.model.CodeFormat
import com.keyserdsoze.cardreader.model.WalletDocument
import org.json.JSONArray
import org.json.JSONObject

object CardJsonCodec {
    fun encode(document: WalletDocument): String = JSONObject()
        .put("schemaVersion", document.schemaVersion)
        .put("cards", JSONArray().apply { document.cards.sortedBy { it.id }.forEach { put(encodeCard(it)) } })
        .put("tombstones", JSONArray().apply { document.tombstones.sortedBy { it.id }.forEach { put(encodeTombstone(it)) } })
        .toString()

    fun decode(raw: String): WalletDocument {
        if (raw.isBlank()) return WalletDocument()
        val root = JSONObject(raw)
        val schema = root.optInt("schemaVersion", 1)
        require(schema == WalletDocument.CURRENT_SCHEMA) { "Unsupported wallet schema: $schema" }
        val cardsJson = root.optJSONArray("cards") ?: JSONArray()
        val tombstonesJson = root.optJSONArray("tombstones") ?: JSONArray()
        val cards = buildList {
            repeat(cardsJson.length()) { add(decodeCard(cardsJson.getJSONObject(it))) }
        }
        val tombstones = buildList {
            repeat(tombstonesJson.length()) { add(decodeTombstone(tombstonesJson.getJSONObject(it))) }
        }
        require(cards.map { it.id }.distinct().size == cards.size) { "Duplicate card id" }
        require(tombstones.map { it.id }.distinct().size == tombstones.size) { "Duplicate tombstone id" }
        return WalletDocument(schema, cards, tombstones)
    }

    private fun encodeCard(card: CardEntry) = JSONObject()
        .put("id", card.id)
        .put("name", card.name)
        .put("value", card.value)
        .put("format", card.format.name)
        .put("colorArgb", card.colorArgb)
        .put("favorite", card.favorite)
        .put("notes", card.notes)
        .put("updatedAt", card.updatedAt)
        .put("writerId", card.writerId)

    private fun decodeCard(json: JSONObject) = CardEntry(
        id = json.getString("id"),
        name = json.getString("name"),
        value = json.getString("value"),
        format = CodeFormat.fromName(json.getString("format")),
        colorArgb = json.optInt("colorArgb", 0xFF315C9A.toInt()),
        favorite = json.optBoolean("favorite", false),
        notes = json.optString("notes", ""),
        updatedAt = json.getLong("updatedAt"),
        writerId = json.getString("writerId"),
    )

    private fun encodeTombstone(tombstone: CardTombstone) = JSONObject()
        .put("id", tombstone.id)
        .put("deletedAt", tombstone.deletedAt)
        .put("writerId", tombstone.writerId)

    private fun decodeTombstone(json: JSONObject) = CardTombstone(
        id = json.getString("id"),
        deletedAt = json.getLong("deletedAt"),
        writerId = json.getString("writerId"),
    )
}
