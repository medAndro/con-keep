package com.conkeep.ui.feature.coupon.detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.navigation.Route
import com.conkeep.ui.component.ConKeepConfirmDialog
import com.conkeep.ui.component.EvenlyTextTopBar
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.feature.coupon.edit.CouponEditViewModel
import com.conkeep.ui.feature.coupon.edit.TextInputField
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderDefault
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.PretendardMedium16
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponEditScreen(
    id: String,
    backStack: NavBackStack<NavKey>,
    viewModel: CouponEditViewModel,
) {
    val coupon by viewModel.coupon.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val pickMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let {
                viewModel.pickCouponImage(uri)
            }
        }

    CouponEditScreenContent(
        isCouponModifiedChecker = viewModel::isCouponModifiedChecker,
        onBackClick = {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                backStack.removeLastOrNull()
            }
        },
        onCouponSave = {},
        onImageClick = {
            coupon?.id?.takeIf { it.isNotEmpty() }?.let { couponId ->
                backStack.add(Route.CouponImageScreen(id = couponId))
            }
        },
        onUseCoupon = { viewModel.useCoupon() },
        onImagePickClick = {
            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        },
        setNewBrandName = { viewModel.setNewBrandName(it) },
        couponUiModel = coupon,
        selectedImageUri = selectedImageUri,
        id = id,
        modifier = Modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponEditScreenContent(
    isCouponModifiedChecker: () -> Boolean,
    onBackClick: () -> Unit,
    onCouponSave: () -> Unit,
    onImageClick: () -> Unit,
    onUseCoupon: () -> Unit,
    onImagePickClick: () -> Unit,
    setNewBrandName: (String) -> Unit,
    couponUiModel: CouponUiModel?,
    selectedImageUri: Uri?,
    id: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    var showModifiedDialog by rememberSaveable { mutableStateOf(false) }
    val removeIcon: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_close)
    val context = LocalContext.current

    BackHandler(enabled = true) {
        if (isCouponModifiedChecker()) {
            showModifiedDialog = true // 변경사항이 있으면 다이얼로그 노출
        } else {
            onBackClick() // 변경사항 없으면 바로 뒤로가기
        }
    }

    Scaffold(
        topBar = {
            EvenlyTextTopBar(
                middleText = stringResource(R.string.coupon_edit_screen_title),
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = stringResource(R.string.topbar_back_description),
                            onClick = {
                                if (isCouponModifiedChecker()) {
                                    showModifiedDialog = true
                                } else {
                                    onBackClick()
                                }
                            },
                        ),
                    ),
                rightButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_save,
                            contentDescription = stringResource(R.string.coupon_edit_screen_save_icon_description),
                            onClick = onCouponSave,
                        ),
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .background(color = bgSurface, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(width = 179.dp, height = 185.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImagePickClick() }
                                .border(1.dp, borderDefault, RoundedCornerShape(8.dp)),
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(selectedImageUri ?: couponUiModel?.r2Url)
                                    .crossfade(true)
                                    .build(),
                            contentDescription = stringResource(R.string.coupon_card_image_description),
                            placeholder = painterResource(R.drawable.ic_corn_ms_emoji),
                            error = painterResource(R.drawable.ic_corn_ms_emoji),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
                if (couponUiModel != null) {
                    InputText(
                        stringResource(R.string.coupon_edit_screen_input_title_brand),
                        couponUiModel.brand ?: "",
                        setNewBrandName,
                        stringResource(R.string.coupon_edit_screen_input_brand_placeholder),
                    )
                }
            }
        }

        if (showModifiedDialog) {
            ConKeepConfirmDialog(
                title = stringResource(R.string.coupon_detail_screen_modified_dialog_title),
                description = stringResource(R.string.coupon_detail_screen_modified_dialog_message),
                confirmText = stringResource(R.string.coupon_detail_screen_modified_dialog_confirm_text),
                cancelText = stringResource(R.string.coupon_detail_screen_modified_dialog_cancel_text),
                onConfirm = {
                    // todo: 저장 로직 실행
                    Toast.makeText(context, "저장하고 돌아갑니다", Toast.LENGTH_SHORT).show()
                    showModifiedDialog = false
                },
                onDismiss = {
                    showModifiedDialog = false
                },
                onCancel = {
                    showModifiedDialog = false
                    Toast.makeText(context, "그냥 돌아갑니다", Toast.LENGTH_SHORT).show()
                    onBackClick()
                },
            )
        }
    }
}

@Composable
private fun InputText(
    titleText: String,
    text: String,
    updatedText: (String) -> Unit,
    placeholderText: String,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            titleText,
            style = PretendardMedium16,
            color = textSecondary,
        )

        TextInputField(
            text = text,
            onTextChange = { updatedText(it) },
            placeholder = placeholderText,
        )
    }
}

private val fakeCoupon =
    CouponUiModel(
        id = "0",
        number = "1234-5678-9012",
        name = "스타벅스 아이스 아메리카노 T",
        brand = "스타벅스",
        expiryDate = ExpiryDate.Success(LocalDate.parse("2025-12-31")),
        isUsed = false,
        isExpired = false,
        status = CouponStatus.SUCCESS,
    )

@Preview(showBackground = true, name = "미사용 쿠폰")
@Composable
private fun CouponEditScreenContentPreview() {
    MaterialTheme {
        CouponEditScreenContent(
            isCouponModifiedChecker = { false },
            onBackClick = {},
            onCouponSave = {},
            onImageClick = {},
            onUseCoupon = {},
            onImagePickClick = {},
            setNewBrandName = {},
            couponUiModel = fakeCoupon,
            selectedImageUri = null,
            id = "0",
        )
    }
}
