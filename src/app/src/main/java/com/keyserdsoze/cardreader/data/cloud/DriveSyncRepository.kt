package com.keyserdsoze.cardreader.data.cloud

import com.keyserdsoze.cardreader.data.CardJsonCodec
import com.keyserdsoze.cardreader.data.LocalCardStore
import com.keyserdsoze.cardreader.data.sync.CardMergeEngine

data class SyncResult(val cardCount: Int)

class DriveSyncRepository(
    private val store: LocalCardStore,
    private val transport: DriveTransport,
) {
    suspend fun sync(): SyncResult {
        val managed = transport.list()
            .filter { it.appProperties[PROP_APP] == APP_MARKER && it.appProperties[PROP_KIND] == KIND_WALLET }
            .sortedByDescending { it.modifiedTime.orEmpty() }
        val local = store.document.value
        val remoteFile = managed.firstOrNull()
        val merged = if (remoteFile == null) {
            local
        } else {
            val remote = CardJsonCodec.decode(transport.read(remoteFile.id).toString(Charsets.UTF_8))
            CardMergeEngine.merge(local, remote)
        }
        store.replace(merged)
        val bytes = CardJsonCodec.encode(merged).toByteArray(Charsets.UTF_8)
        val properties = mapOf(PROP_APP to APP_MARKER, PROP_KIND to KIND_WALLET, PROP_SCHEMA to "1")
        if (remoteFile == null) {
            transport.create(FILE_NAME, properties, bytes)
        } else {
            transport.update(remoteFile.id, FILE_NAME, properties, bytes)
        }
        return SyncResult(merged.cards.size)
    }

    private companion object {
        const val FILE_NAME = "cardreader-wallet-v1.json"
        const val PROP_APP = "cr_app"
        const val PROP_KIND = "cr_kind"
        const val PROP_SCHEMA = "cr_schema"
        const val APP_MARKER = "card_reader"
        const val KIND_WALLET = "wallet"
    }
}
