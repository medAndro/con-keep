package com.conkeep.domain.usecase.coupon

import android.content.Context
import android.net.Uri
import com.conkeep.data.local.file.LocalFileManager
import com.conkeep.data.processor.CouponPreProcessResult
import com.conkeep.di.annotation.IoDispatcher
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class PreLocalProcessCouponUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val fileManager: LocalFileManager,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend operator fun invoke(uri: Uri): Result<CouponPreProcessResult> =
            withContext(ioDispatcher) {
                runCatching {
                    // 원본 복사
                    val tempFile =
                        fileManager.createRawImageCacheFileFromUri(uri)
                            ?: throw Exception("임시 파일 생성 실패")

                    try {
                        // 최적화 및 바코드 스캔
                        val optimizedFile = fileManager.optimizeImage(tempFile)
                        val barcode = scanBarcodeFromFile(optimizedFile)

                        // 캐시 저장
                        val finalPath =
                            fileManager.saveToCache(optimizedFile)
                                ?: throw Exception("로컬 이미지 캐시 저장 실패")

                        CouponPreProcessResult(
                            localCachePath = finalPath,
                            barcode = barcode,
                            mimeType = "image/webp",
                        )
                    } finally {
                        // 어떤 상황에서도 임시 파일은 삭제
                        if (tempFile.exists()) tempFile.delete()
                    }
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
