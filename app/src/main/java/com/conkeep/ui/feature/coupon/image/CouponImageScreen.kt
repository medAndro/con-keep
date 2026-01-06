package com.conkeep.ui.feature.coupon.image

import android.app.Activity
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

    if (window != null) {
        val controller =
            remember(window, view) {
                WindowCompat.getInsetsController(window, view)
            }

        DisposableEffect(controller) {
            controller.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
