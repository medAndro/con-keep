package com.conkeep.data.processor

import android.content.Context
import android.net.Uri
import com.conkeep.data.local.file.LocalFileManager
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CouponProcessor
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val fileManager: LocalFileManager,
    ) {
        suspend fun preProcessImage(uri: Uri): CouponPreProcessResult =
            withContext(Dispatchers.IO) {
                val mimeType = context.contentResolver.getType(uri)
                val path = fileManager.saveCouponImage(uri, mimeType)
                val barcode = scanBarcodeFromUri(context, uri)

                CouponPreProcessResult(
                    localPath = path,
                    barcode = barcode,
                    mimeType = mimeType,
                )
            }

        suspend fun scanBarcodeFromUri(
            context: Context,
            uri: Uri,
        ): String? =
            withContext(Dispatchers.IO) {
                try {
                    val image = InputImage.fromFilePath(context, uri)

                    // 스캐너 옵션 (쿠폰은 보통 CODE_128, EAN_13, QR이지만 전체 검사)
                    val options =
                        BarcodeScannerOptions
                            .Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_ALL_FORMATS,
                            ).build()

                    val scanner = BarcodeScanning.getClient(options)

                    val barcodes = scanner.process(image).await()

                    // 인식된 바코드들 중 첫 번째 값의 displayValue 반환
                    barcodes.firstOrNull()?.displayValue
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
    }
