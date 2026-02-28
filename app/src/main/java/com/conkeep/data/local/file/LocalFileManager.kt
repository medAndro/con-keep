package com.conkeep.data.local.file

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFileManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val couponImageDir = File(context.filesDir, "coupon_images")
        private val tempImageDir = File(context.cacheDir, "temp_images")
        private val shareCacheDir = File(context.cacheDir, "share_tmp")
        private val downloadTempDir = File(context.cacheDir, "download_temp")

        init {
            cleanupOldCache()
        }

        /**
         * 24시간 이상 된 캐시 파일 자동 정리
         */
        private fun cleanupOldCache() {
            try {
                val currentTime = System.currentTimeMillis()
                val maxAge = 24 * 60 * 60 * 1000 // 24시간

                listOf(tempImageDir, shareCacheDir, downloadTempDir).forEach { dir ->
                    dir.listFiles()?.forEach { file ->
                        if (currentTime - file.lastModified() > maxAge) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 다운로드된 이미지 바이트를 임시 파일로 저장합니다.
         * @param bytes 이미지 바이트 데이터
         * @param prefix 파일명 접두사 (예: "download_", "coupon_")
         * @return 생성된 임시 파일, 실패 시 null
         */
        fun createTempFileFromBytes(
            bytes: ByteArray,
            prefix: String = "download",
        ): File? =
            try {
                if (!downloadTempDir.exists()) downloadTempDir.mkdirs()

                val tempFile =
                    File(
                        downloadTempDir,
                        "${prefix}_${System.currentTimeMillis()}.webp",
                    )

                tempFile.writeBytes(bytes)
                tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        /**
         * Compressor 입력을 위해 Uri의 데이터를 캐시 디렉토리에 임시 파일로 복사합니다.
         */
        fun createRawImageCacheFileFromUri(uri: Uri): File? =
            try {
                if (!tempImageDir.exists()) tempImageDir.mkdirs()

                val tempFile = File(tempImageDir, "raw_${System.currentTimeMillis()}.tmp")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        fun optimizeImage(tempFile: File): File {
            // 1. 이미지 크기 확인
            val options =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
            BitmapFactory.decodeFile(tempFile.absolutePath, options)

            val maxSize = MAX_SIZE
            val needsResize = options.outWidth > maxSize || options.outHeight > maxSize

            return when {
                needsResize -> {
                    // 2. 리사이징 필요 시
                    val ratio = options.outWidth.toFloat() / options.outHeight.toFloat()
                    val (targetWidth, targetHeight) =
                        if (options.outWidth > options.outHeight) {
                            maxSize to (maxSize / ratio).toInt()
                        } else {
                            (maxSize * ratio).toInt() to maxSize
                        }

                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                    val scaledBitmap = bitmap.scale(targetWidth, targetHeight, filter = true)
                    bitmap.recycle()

                    saveAsWebP(scaledBitmap, tempFile)
                }

                else -> {
                    // 3. 작은 이미지라 리사이징 필요 없을시
                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                    saveAsWebP(bitmap, tempFile)
                }
            }
        }

        private fun saveAsWebP(
            bitmap: Bitmap,
            originalFile: File,
        ): File {
            val webpFile = File(originalFile.parent, "${originalFile.nameWithoutExtension}.webp")
            FileOutputStream(webpFile).use {
                bitmap.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, it)
            }
            bitmap.recycle()
            originalFile.delete()
            return webpFile
        }

        /**
         * 압축이 완료된 파일을 영구 폴더(coupon_images)로 이동시키고 경로를 반환합니다.
         */
        fun saveProcessedFile(compressedFile: File): String? =
            try {
                val imageDir = couponImageDir.apply { if (!exists()) mkdirs() }
                val fileName = "coupon_${System.currentTimeMillis()}.webp"
                val destinationFile = File(imageDir, fileName)

                compressedFile.copyTo(destinationFile, overwrite = true)
                destinationFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                compressedFile.delete()
            }

        /**
         * 압축이 완료된 파일을 캐시 폴더로 이동시키고 경로를 반환합니다.
         */
        fun saveToCache(compressedFile: File): String? =
            try {
                val imageDir = tempImageDir.apply { if (!exists()) mkdirs() }
                val fileName = "coupon_cache_${System.currentTimeMillis()}.webp"
                val destinationFile = File(imageDir, fileName)

                compressedFile.copyTo(destinationFile, overwrite = true)
                destinationFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                compressedFile.delete()
            }

        /**
         * 특정 파일명으로 복사본을 만들어 공유용 URI 발행
         * @param absolutePath 원본 파일 경로
         * @param newFileName 보여주고 싶은 파일명 (확장자 제외)
         */
        suspend fun getShareUriWithCustomName(
            absolutePath: String,
            newFileName: String,
        ): Uri? =
            withContext(Dispatchers.IO) {
                try {
                    val sourceFile = File(absolutePath)
                    if (!sourceFile.exists()) return@withContext null

                    val extension = sourceFile.extension
                    val finalFileName = "${newFileName.fileNameSanitize()}.$extension"

                    if (!shareCacheDir.exists()) shareCacheDir.mkdirs()

                    // 1시간 이상 된 파일만 정리
                    val currentTime = System.currentTimeMillis()
                    shareCacheDir.listFiles()?.forEach { file ->
                        if (currentTime - file.lastModified() > 3600000) {
                            file.delete()
                        }
                    }

                    val tempFile = File(shareCacheDir, finalFileName)
                    sourceFile.copyTo(tempFile, overwrite = true)

                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

        /**
         * 공용 저장소(Pictures/ConKeep)로 이미지 내보내기
         */
        suspend fun exportImageToPublic(
            absolutePath: String,
            newFileName: String,
        ): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val sourceFile = File(absolutePath)
                    if (!sourceFile.exists()) return@withContext false

                    val extension = sourceFile.extension
                    val finalFileName = "${newFileName.fileNameSanitize()}.$extension"

                    // MIME type 정확히 매핑
                    val mimeType = getMimeTypeFromFile(sourceFile)

                    val contentResolver = context.contentResolver

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val values =
                            ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ConKeep")
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }

                        val uri =
                            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                ?: return@withContext false

                        contentResolver.openOutputStream(uri)?.use { output ->
                            sourceFile.inputStream().use { input -> input.copyTo(output) }
                        }

                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    } else {
                        val publicDir =
                            File(
                                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                                "ConKeep",
                            ).apply { if (!exists()) mkdirs() }

                        val destFile = File(publicDir, finalFileName)
                        sourceFile.copyTo(destFile, overwrite = true)

                        val values =
                            ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                                put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                            }
                        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

        fun String.fileNameSanitize(): String =
            this.replace(Regex("[^a-zA-Z0-9가-힣]"), "_").let {
                if (it.length > 127) {
                    it.take(127)
                } else {
                    it
                }
            }

        /**
         * 파일 헤더를 읽어 실제 MIME 타입을 반환합니다.
         * Coil 캐시 파일(.1, .0)처럼 확장자가 없는 파일에 유효합니다.
         */
        private fun getMimeTypeFromFile(file: File): String {
            val options =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true // 실제 비트맵을 로드하지 않고 정보만 읽음
                }
            BitmapFactory.decodeFile(file.absolutePath, options)
            return options.outMimeType ?: "image/webp" // 못 찾을 경우 기본값
        }

        companion object {
            private const val MAX_SIZE = 1530
            private const val WEBP_QUALITY = 88
        }
    }
