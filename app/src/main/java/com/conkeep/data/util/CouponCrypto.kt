package com.conkeep.data.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CouponCrypto {
    fun decryptPin(
        masterKeyBytes: ByteArray,
        cipherText: String,
    ): String? =
        runCatching {
            val raw = Base64.decode(cipherText, Base64.NO_WRAP)
            val iv = raw.sliceArray(0..11)
            val data = raw.sliceArray(12..raw.lastIndex)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(masterKeyBytes, "AES"),
                GCMParameterSpec(128, iv),
            )
            String(cipher.doFinal(data), Charsets.UTF_8)
        }.getOrNull()

    fun deriveImageKey(
        masterKeyBytes: ByteArray,
        couponId: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(masterKeyBytes, "HmacSHA256"))
        return Base64.encodeToString(
            mac.doFinal(couponId.toByteArray()),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }
}
