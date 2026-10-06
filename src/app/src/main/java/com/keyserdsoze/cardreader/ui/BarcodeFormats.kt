package com.keyserdsoze.cardreader.ui

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.keyserdsoze.cardreader.model.CodeFormat

object BarcodeFormats {
    fun fromMlKit(format: Int): CodeFormat? = when (format) {
        Barcode.FORMAT_QR_CODE -> CodeFormat.QR_CODE
        Barcode.FORMAT_EAN_13 -> CodeFormat.EAN_13
        Barcode.FORMAT_EAN_8 -> CodeFormat.EAN_8
        Barcode.FORMAT_UPC_A -> CodeFormat.UPC_A
        Barcode.FORMAT_UPC_E -> CodeFormat.UPC_E
        Barcode.FORMAT_CODE_128 -> CodeFormat.CODE_128
        Barcode.FORMAT_CODE_39 -> CodeFormat.CODE_39
        Barcode.FORMAT_CODE_93 -> CodeFormat.CODE_93
        Barcode.FORMAT_CODABAR -> CodeFormat.CODABAR
        Barcode.FORMAT_ITF -> CodeFormat.ITF
        Barcode.FORMAT_DATA_MATRIX -> CodeFormat.DATA_MATRIX
        Barcode.FORMAT_AZTEC -> CodeFormat.AZTEC
        Barcode.FORMAT_PDF417 -> CodeFormat.PDF_417
        else -> null
    }

    fun toZxing(format: CodeFormat): BarcodeFormat = when (format) {
        CodeFormat.QR_CODE -> BarcodeFormat.QR_CODE
        CodeFormat.EAN_13 -> BarcodeFormat.EAN_13
        CodeFormat.EAN_8 -> BarcodeFormat.EAN_8
        CodeFormat.UPC_A -> BarcodeFormat.UPC_A
        CodeFormat.UPC_E -> BarcodeFormat.UPC_E
        CodeFormat.CODE_128 -> BarcodeFormat.CODE_128
        CodeFormat.CODE_39 -> BarcodeFormat.CODE_39
        CodeFormat.CODE_93 -> BarcodeFormat.CODE_93
        CodeFormat.CODABAR -> BarcodeFormat.CODABAR
        CodeFormat.ITF -> BarcodeFormat.ITF
        CodeFormat.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        CodeFormat.AZTEC -> BarcodeFormat.AZTEC
        CodeFormat.PDF_417 -> BarcodeFormat.PDF_417
    }
}
