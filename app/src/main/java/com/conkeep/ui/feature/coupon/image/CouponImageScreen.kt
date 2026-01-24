package com.conkeep.ui.feature.coupon.image

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.bgFullscreen
import com.conkeep.ui.theme.ConKeepColors.bgFullscreenTransparency
import com.conkeep.ui.theme.ConKeepColors.textWhite
import com.conkeep.ui.util.DisableHaptic
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponImageScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: CouponImageViewModel,
) {
    val coupon by viewModel.coupon.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val window = remember(context) { (context as? Activity)?.window }
    val shareTitle = stringResource(R.string.coupon_image_share_title)
    val saveSuccessMsg = stringResource(R.string.coupon_image_saved)
    val saveFailMsg = stringResource(R.string.coupon_image_save_failed)
    val shareFailMsg = stringResource(R.string.coupon_image_processing_failed)
    val filePermissionMsg = stringResource(R.string.coupon_image_save_permission_not_grant)

    fun executeSave() {
        viewModel.saveCoupon { success ->
            Toast
                .makeText(
                    context,
                    if (success == true) saveSuccessMsg else saveFailMsg,
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    fun checkPermission(): Boolean =
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            when (isGranted) {
                true -> {
                    executeSave()
                }

                false -> {
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
        }

    if (window != null) {
        val controller =
            remember(window, view) {
                WindowCompat.getInsetsController(window, view)
            }

        DisposableEffect(controller) {
            controller.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    CouponImageContents(
        imageUri = coupon?.localImagePath ?: "",
        onBackClick = {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                backStack.removeLastOrNull()
            }
        },
        onSaveClick = {
            when (checkPermission()) {
                true -> executeSave()
                false -> permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        },
        onShareClick = {
            viewModel.shareCoupon { shareUri ->
                if (shareUri != null) {
                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    context.startActivity(Intent.createChooser(intent, shareTitle))
                } else {
                    Toast
                        .makeText(
                            context,
                            shareFailMsg,
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponImageContents(
    imageUri: String,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    val isPreview = LocalInspectionMode.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(bgFullscreen),
    ) {
        if (isPreview) {
            Image(
                painter = painterResource(id = R.drawable.ic_corn_ms_emoji),
                contentDescription = stringResource(R.string.coupon_image_screen_image_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
            )
        } else {
            DisableHaptic {
                ZoomableAsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                    contentDescription = stringResource(R.string.coupon_image_screen_image_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                )
            }
        }

        Toolbar(
            onBackClick = onBackClick,
            onSaveClick = onSaveClick,
            onShareClick = onShareClick,
            iconTint = textWhite,
            backgroundColor = bgFullscreenTransparency,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CouponImageContentsPreview() {
    CouponImageContents(
        imageUri = "dummy_for_preview",
    )
}
