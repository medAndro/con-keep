package com.conkeep.data.local.file

import android.content.Context
import android.net.Uri
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
        fun getShareUriWithCustomName(
            absolutePath: String,
            newFileName: String,
        ): Uri? {
            try {
                val sourceFile = File(absolutePath)
                if (!sourceFile.exists()) return null

                val extension = sourceFile.extension
                val sanitizedFileName =
                    newFileName.replace(Regex("[^a-zA-Z0-9가-힣]"), "_").let {
                        if (it.length > 127) {
                            it.take(20)
                        } else {
                            it
                        }
                    }
                val finalFileName = "$sanitizedFileName.$extension"

                val shareDir =
                    File(context.cacheDir, "share_tmp").apply {
                        if (!exists()) mkdirs()
                        listFiles()?.forEach { it.delete() }
                    }

                val tempFile = File(shareDir, finalFileName)
                sourceFile.copyTo(tempFile, overwrite = true)

                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile,
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
