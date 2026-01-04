package com.conkeep.data.local.file

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
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
        suspend fun saveCouponImage(
            uri: Uri,
            mimeType: String?,
        ): String? =
            withContext(Dispatchers.IO) {
                try {
                    val extension =
                        MimeTypeMap
                            .getSingleton()
                            .getExtensionFromMimeType(mimeType) ?: "jpg"

                    val fileName = "coupon_${System.currentTimeMillis()}.$extension"
                    val destinationFile = File(context.filesDir, fileName)

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
    }
