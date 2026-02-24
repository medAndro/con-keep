package com.conkeep.ui.feature.coupon.common

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.conkeep.R

@Composable
fun rememberCouponActionHandler(
    onSaveRequested: (onResult: (Boolean?) -> Unit) -> Unit,
    onShareRequested: ((Uri?) -> Unit) -> Unit = {},
): CouponActionManager {
    val context = LocalContext.current
    val saveSuccessMsg = stringResource(R.string.coupon_image_saved)
    val saveFailMsg = stringResource(R.string.coupon_image_save_failed)
    val shareFailMsg = stringResource(R.string.coupon_image_processing_failed)
    val filePermissionMsg = stringResource(R.string.coupon_image_save_permission_not_grant)
    val shareTitle = stringResource(R.string.coupon_image_share_title)

    // 저장 로직 실행
    val executeSave = {
        onSaveRequested { success ->
            Toast
                .makeText(
                    context,
                    if (success == true) saveSuccessMsg else saveFailMsg,
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    // 권한 요청 런처
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                executeSave()
            } else {
                val activity = context as? Activity
                if (activity != null) {
                    val shouldShowRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        )
                    if (!shouldShowRationale) {
                        // Todo: 영구 거부 - 설정으로 이동 스낵바 버튼 추가
                        Toast
                            .makeText(
                                context,
                                filePermissionMsg,
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        }

    return remember {
        CouponActionManager(
            saveImage = {
                val hasPermission =
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                if (hasPermission) {
                    executeSave()
                } else {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            },
            shareImage = {
                onShareRequested { shareUri ->
                    if (shareUri != null) {
                        val intent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        context.startActivity(Intent.createChooser(intent, shareTitle))
                    } else {
                        Toast.makeText(context, shareFailMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}

class CouponActionManager(
    val saveImage: () -> Unit,
    val shareImage: () -> Unit,
)
