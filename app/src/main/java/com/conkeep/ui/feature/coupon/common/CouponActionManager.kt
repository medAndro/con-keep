// CouponActionManager.kt
package com.conkeep.ui.feature.coupon.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.conkeep.R

class CouponActionManager(
    private val context: Context,
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun handleSaveResult(isSuccess: Boolean) {
        val msgId =
            if (isSuccess) R.string.coupon_image_saved else R.string.coupon_image_save_failed
        Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show()
    }

    fun launchShareIntent(shareUri: Uri?) {
        if (shareUri != null) {
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val title = context.getString(R.string.coupon_image_share_title)
            context.startActivity(Intent.createChooser(intent, title))
        } else {
            Toast
                .makeText(context, R.string.coupon_image_processing_failed, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun copyToClipboard(text: String?) {
        if (text.isNullOrBlank()) return
        val clip = ClipData.newPlainText("coupon_number", text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, R.string.coupon_code_copy, Toast.LENGTH_SHORT).show()
    }
}
