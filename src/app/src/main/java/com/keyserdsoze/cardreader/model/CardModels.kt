package com.keyserdsoze.cardreader.model

import java.util.UUID

enum class CodeFormat(val label: String) {
    QR_CODE("QR Code"),
    EAN_13("EAN-13"),
    EAN_8("EAN-8"),
    UPC_A("UPC-A"),
    UPC_E("UPC-E"),
    CODE_128("Code 128"),
    CODE_39("Code 39"),
    CODE_93("Code 93"),
    CODABAR("Codabar"),
    ITF("ITF"),
    DATA_MATRIX("Data Matrix"),
    AZTEC("Aztec"),
    PDF_417("PDF417"),
    ;

    companion object {
        fun fromName(value: String): CodeFormat = entries.firstOrNull { it.name == value } ?: QR_CODE
    }
}

data class CardEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val value: String,
    val format: CodeFormat,
    val colorArgb: Int = 0xFF315C9A.toInt(),
    val favorite: Boolean = false,
    val notes: String = "",
    val updatedAt: Long,
    val writerId: String,
)

data class CardTombstone(
    val id: String,
    val deletedAt: Long,
    val writerId: String,
)

data class WalletDocument(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val cards: List<CardEntry> = emptyList(),
    val tombstones: List<CardTombstone> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA = 1
    }
}
