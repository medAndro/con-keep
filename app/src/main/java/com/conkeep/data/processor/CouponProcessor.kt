package com.conkeep.data.processor

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class CouponProcessor
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun processImage(uri: Uri): Pair<String?, String?> =
            withContext(Dispatchers.IO) {
                val path = saveImageToAppStorage(context, uri)
                val barcode = scanBarcodeFromUri(context, uri)
                path to barcode
            }

        suspend fun saveImageToAppStorage(
            context: Context,
            uri: Uri,
        ): String? =
            withContext(Dispatchers.IO) {
                try {
                    // 1. MIME 타입 확인
                    val mimeType = context.contentResolver.getType(uri)
                    if (mimeType?.startsWith("image/") != true) {
                        return@withContext null
                    }

                    // 2. 파일 확장자 추출
                    val extension =
                        MimeTypeMap
                            .getSingleton()
                            .getExtensionFromMimeType(mimeType) ?: "jpg"

                    // 3. 앱 내부 저장소에 파일 생성
                    val fileName = "coupon_${System.currentTimeMillis()}.$extension"
                    val destinationFile = File(context.filesDir, fileName)

                    // 4. URI에서 InputStream으로 읽어서 파일로 복사
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 5. 저장된 파일 경로 반환 (Room DB에 저장할 경로)
                    destinationFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
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
