package com.keyserdsoze.cardreader.data.sync

import com.keyserdsoze.cardreader.model.CardEntry
import com.keyserdsoze.cardreader.model.CardTombstone
import com.keyserdsoze.cardreader.model.WalletDocument

object CardMergeEngine {
    fun merge(local: WalletDocument, remote: WalletDocument): WalletDocument {
        val localCards = local.cards.associateBy { it.id }
        val remoteCards = remote.cards.associateBy { it.id }
        val localDeletes = local.tombstones.associateBy { it.id }
        val remoteDeletes = remote.tombstones.associateBy { it.id }
        val ids = localCards.keys + remoteCards.keys + localDeletes.keys + remoteDeletes.keys
        val cards = mutableListOf<CardEntry>()
        val deletes = mutableListOf<CardTombstone>()

        ids.forEach { id ->
            val candidates = listOfNotNull(
                localCards[id]?.let(::VersionedCard), remoteCards[id]?.let(::VersionedCard),
                localDeletes[id]?.let(::VersionedDelete), remoteDeletes[id]?.let(::VersionedDelete),
            )
            when (val winner = candidates.maxWithOrNull(versionComparator)) {
                is VersionedCard -> cards += winner.card
                is VersionedDelete -> deletes += winner.tombstone
                null -> Unit
            }
        }
        return WalletDocument(cards = cards, tombstones = deletes)
    }

    private sealed interface Versioned {
        val timestamp: Long
        val writerId: String
        val deleteRank: Int
        val stableValue: String
    }

    private data class VersionedCard(val card: CardEntry) : Versioned {
        override val timestamp = card.updatedAt
        override val writerId = card.writerId
        override val deleteRank = 0
        override val stableValue = listOf(
            card.name, card.value, card.format.name, card.colorArgb.toString(), card.favorite.toString(), card.notes,
        ).joinToString("\u0000")
    }

    private data class VersionedDelete(val tombstone: CardTombstone) : Versioned {
        override val timestamp = tombstone.deletedAt
        override val writerId = tombstone.writerId
        override val deleteRank = 1
        override val stableValue = ""
    }

    private val versionComparator = Comparator<Versioned> { left, right ->
        compareValues(left.timestamp, right.timestamp)
            .takeIf { it != 0 }
            ?: compareValues(left.deleteRank, right.deleteRank).takeIf { it != 0 }
            ?: compareValues(left.writerId, right.writerId).takeIf { it != 0 }
            ?: compareValues(left.stableValue, right.stableValue)
    }
}
