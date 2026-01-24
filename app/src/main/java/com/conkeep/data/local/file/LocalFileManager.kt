package com.conkeep.data.local.file

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
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
        private val imageFolderName = "coupon_images"

        suspend fun saveCouponImage(
            uri: Uri,
            mimeType: String?,
        ): String? =
            withContext(Dispatchers.IO) {
                try {
                    val imageDir =
                        File(context.filesDir, imageFolderName).apply {
                            if (!exists()) mkdirs()
                        }

                    val extension =
                        MimeTypeMap
                            .getSingleton()
                            .getExtensionFromMimeType(mimeType) ?: "jpg"

                    val fileName = "coupon_${System.currentTimeMillis()}.$extension"
                    val destinationFile = File(imageDir, fileName)

                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    destinationFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
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

                    val shareDir =
                        File(context.cacheDir, "share_tmp").apply {
                            if (!exists()) mkdirs()
                            listFiles()?.forEach { it.delete() }
                        }

                    val tempFile = File(shareDir, finalFileName)

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
                    val contentResolver = context.contentResolver

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        // --- Android 10 (API 29) 이상 ---
                        val values =
                            ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                                put(MediaStore.Images.Media.MIME_TYPE, "image/$extension")
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
                        // --- Android 9 (API 28) 이하 ---
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
                                put(MediaStore.Images.Media.MIME_TYPE, "image/$extension")
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
    }
