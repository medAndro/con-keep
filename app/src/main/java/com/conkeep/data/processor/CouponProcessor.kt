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
import java.io.File
import javax.inject.Inject

class CouponProcessor
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val fileManager: LocalFileManager,
    ) {
        suspend fun preProcessImage(uri: Uri): CouponPreProcessResult =
            withContext(Dispatchers.IO) {
                val tempFile =
                    fileManager.createRawImageCacheFileFromUri(uri)
                        ?: throw Exception("임시 파일 생성 실패")

                try {
                    val finalFile = fileManager.optimizeImage(tempFile)
                    val barcode = scanBarcodeFromFile(finalFile)
                    val finalPath =
                        fileManager.saveToCache(finalFile)
                            ?: throw Exception("로컬 이미지 캐시 저장 실패")

                    CouponPreProcessResult(
                        localPath = finalPath,
                        barcode = barcode,
                        mimeType = "image/webp",
                    )
                } finally {
                    if (tempFile.exists()) tempFile.delete()
                }
            }

        private suspend fun scanBarcodeFromFile(file: File): String? =
            withContext(Dispatchers.IO) {
                try {
                    val image = InputImage.fromFilePath(context, Uri.fromFile(file))
                    val options =
                        BarcodeScannerOptions
                            .Builder()
                            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                            .build()

                    val scanner = BarcodeScanning.getClient(options)
                    val barcodes = scanner.process(image).await()
                    barcodes.firstOrNull()?.displayValue
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
    }
