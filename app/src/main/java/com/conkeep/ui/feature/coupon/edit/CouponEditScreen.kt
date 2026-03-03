package com.conkeep.ui.feature.coupon.edit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.conkeep.ui.component.ConKeepConfirmDialog
import com.conkeep.ui.component.EvenlyTextTopBar
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.feature.coupon.component.AmountInputField
import com.conkeep.ui.feature.coupon.component.MemoInputField
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.theme.ConKeepColors.bgInput
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderDefault
import com.conkeep.ui.theme.ConKeepColors.borderFocused
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.brandPrimary
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium16
import com.conkeep.ui.util.spToDp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponEditScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: CouponEditViewModel,
) {
    val coupon by viewModel.couponUiModel.collectAsStateWithLifecycle()
    val selectedImageUriStatus by viewModel.selectedImageUriStatus.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val pickMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let {
                viewModel.pickCouponImage(uri)
            }
        }
    val context = LocalContext.current
    val couponEditedMessage = stringResource(R.string.coupon_edit_screen_coupon_edited_alert)
    val couponDataIsSameMessage =
        stringResource(R.string.coupon_edit_screen_coupon_data_is_same_alert)

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { toastMessage ->
            when (toastMessage) {
                CouponEditEvent.CouponEdited -> {
                    Toast.makeText(context, couponEditedMessage, Toast.LENGTH_SHORT).show()
                }

                CouponEditEvent.CouponDataIsSame -> {
                    Toast.makeText(context, couponDataIsSameMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CouponEditScreenContent(
        isCouponModifiedChecker = viewModel::isCouponModified,
        onBackClick = {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                backStack.removeLastOrNull()
            }
        },
        onCouponSave = { viewModel.saveCouponInfo() },
        onImagePickClick = {
            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        },
        setNewBrandName = { viewModel.setNewBrandName(it) },
        setNewProductName = { viewModel.setNewProductName(it) },
        setNewPinNumber = { viewModel.setNewPinNumber(it) },
        setNewExpiryDate = { viewModel.setNewExpiryDate(it) },
        setNewAmount = { viewModel.setNewAmount(it) },
        toggleMonetary = { viewModel.toggleMonetary() },
        setNewMemo = { viewModel.setNewMemo(it) },
        couponUiModel = coupon,
        selectedImageUri =
            when (selectedImageUriStatus) {
                is SelectedImageUriStatus.Selected -> (selectedImageUriStatus as SelectedImageUriStatus.Selected).localAbsolutePath.toUri()
                is SelectedImageUriStatus.Uploaded -> (selectedImageUriStatus as SelectedImageUriStatus.Uploaded).localAbsolutePath.toUri()
                else -> null
            },
        modifier = Modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponEditScreenContent(
    isCouponModifiedChecker: () -> Boolean,
    onBackClick: () -> Unit,
    onCouponSave: () -> Unit,
    onImagePickClick: () -> Unit,
    setNewBrandName: (String) -> Unit,
    setNewProductName: (String) -> Unit,
    setNewPinNumber: (String) -> Unit,
    setNewExpiryDate: (ExpiryDate) -> Unit,
    setNewAmount: (String) -> Unit,
    toggleMonetary: () -> Unit,
    setNewMemo: (String) -> Unit,
    couponUiModel: CouponUiModel?,
    selectedImageUri: Uri?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    var showModifiedDialog by rememberSaveable { mutableStateOf(false) }
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
                            onClick = {
                                onCouponSave()
                            },
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
                    InputText(
                        stringResource(R.string.coupon_edit_screen_input_title_product_name),
                        couponUiModel.name ?: "",
                        setNewProductName,
                        stringResource(R.string.coupon_edit_screen_input_product_name_placeholder),
                    )
                    InputText(
                        stringResource(R.string.coupon_edit_screen_input_title_pin_number),
                        couponUiModel.number ?: "",
                        setNewPinNumber,
                        stringResource(R.string.coupon_edit_screen_input_pin_number_placeholder),
                    )
                    InputDate(
                        currentExpiryDate = couponUiModel.expiryDate,
                        onDateChanged = {
                            setNewExpiryDate(it)
                        },
                    )
                    InputAmount(
                        couponUiModel.amount,
                        couponUiModel.isMonetary,
                        toggleMonetary,
                        setNewAmount,
                    )
                }
                InputMemo(
                    couponUiModel?.memo ?: "",
                    updatedText = setNewMemo,
                )
            }
        }

        if (showModifiedDialog) {
            ConKeepConfirmDialog(
                title = stringResource(R.string.coupon_detail_screen_modified_dialog_title),
                description = stringResource(R.string.coupon_detail_screen_modified_dialog_message),
                confirmText = stringResource(R.string.coupon_detail_screen_modified_dialog_confirm_text),
                cancelText = stringResource(R.string.coupon_detail_screen_modified_dialog_cancel_text),
                onConfirm = {
                    onCouponSave()
                    showModifiedDialog = false
                    onBackClick()
                },
                onDismiss = {
                    showModifiedDialog = false
                },
                onCancel = {
                    showModifiedDialog = false
                    Toast.makeText(context, "변경사항을 취소하고 돌아갑니다", Toast.LENGTH_SHORT).show()
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

@Composable
private fun InputAmount(
    amount: String,
    isMonetary: Boolean,
    toggleMonetary: () -> Unit,
    updatedText: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.Start,
    ) {
        ToggleCheckbox(
            onToggleClick = {
                updatedText(amount)
                toggleMonetary()
            },
            isChecked = !isMonetary,
            titleText = stringResource(R.string.coupon_edit_screen_amount_title),
            toggleLabel = stringResource(R.string.coupon_edit_screen_is_not_monetary_label),
        )
        AnimatedVisibility(
            visible = isMonetary,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            AmountInputField(
                amountText = amount,
                onAmountChange = { newAmount ->
                    updatedText(newAmount)
                },
                onSave = {},
                showLeadingIcon = true,
                showTrailingIcon = false,
                autoSave = false,
            )
        }
    }
}

@Composable
private fun InputMemo(
    text: String,
    updatedText: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            stringResource(R.string.coupon_detail_screen_memo_title),
            style = PretendardMedium16,
            color = textSecondary,
        )

        MemoInputField(
            memo = text,
            onMemoChange = { updatedText(it) },
            onSave = {},
            autosave = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToggleCheckbox(
    titleText: String,
    toggleLabel: String,
    onToggleClick: () -> Unit,
    isChecked: Boolean,
) {
    val checkedImageVector = ImageVector.vectorResource(id = R.drawable.ic_checked)
    val uncheckedImageVector = ImageVector.vectorResource(id = R.drawable.ic_unchecked)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titleText,
            style = PretendardMedium16,
            color = textSecondary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 토글
            Text(
                toggleLabel,
                style = PretendardMedium16,
                color = textSecondary,
            )
            Surface(
                onClick = onToggleClick,
                color = Color.Transparent,
                shape = CircleShape,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isChecked) checkedImageVector else uncheckedImageVector,
                        contentDescription = stringResource(R.string.coupon_edit_screen_expiry_date_is_null_title),
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InputDate(
    currentExpiryDate: ExpiryDate,
    onDateChanged: (ExpiryDate) -> Unit,
) {
    var isInteracting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy((11 - ((36 - 16f.spToDp(context)) / 2)).dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ToggleCheckbox(
            onToggleClick = {
                onDateChanged(
                    when (currentExpiryDate) {
                        is ExpiryDate.Empty ->
                            ExpiryDate.Success(
                                currentExpiryDate.value ?: Clock.System
                                    .now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date,
                            )

                        ExpiryDate.Processing -> ExpiryDate.Processing
                        is ExpiryDate.Success ->
                            ExpiryDate.Empty(
                                currentExpiryDate.value,
                            )
                    },
                )
            },
            titleText = stringResource(R.string.coupon_edit_screen_expiry_date_title),
            isChecked = currentExpiryDate !is ExpiryDate.Success,
            toggleLabel = stringResource(R.string.coupon_edit_screen_expiry_date_is_null_title),
        )
        AnimatedVisibility(
            visible = currentExpiryDate is ExpiryDate.Success,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            val displayDate =
                (currentExpiryDate as? ExpiryDate.Success)?.value
                    ?: Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date

            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(bgInput, RoundedCornerShape(10.dp))
                        .border(
                            width = if (isInteracting) 2.dp else 1.dp,
                            color = if (isInteracting) borderFocused else borderSubtle,
                            shape = RoundedCornerShape(10.dp),
                        ).pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Press -> isInteracting = true
                                        PointerEventType.Release, PointerEventType.Exit ->
                                            isInteracting =
                                                false
                                    }
                                }
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                WheelDatePicker(
                    modifier = Modifier.padding(vertical = 8.dp),
                    startDate = displayDate,
                    size = DpSize((maxWidth.value * 0.85).dp, 120.dp), // 적절한 여백이 포함된 사이즈
                    rowCount = 3,
                    textStyle = PretendardMedium16,
                    textColor = textPrimary,
                    selectorProperties =
                        WheelPickerDefaults.selectorProperties(
                            enabled = true,
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border =
                                BorderStroke(
                                    if (isInteracting) 2.dp else 1.5.dp,
                                    brandPrimary.copy(alpha = 0.7f),
                                ),
                        ),
                    onSnappedDate = { snappedDate ->
                        onDateChanged(ExpiryDate.Success(snappedDate))
                    },
                )
            }
        }
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
        isDirty = false,
    )

@Preview(showBackground = true, name = "미사용 쿠폰")
@Composable
private fun CouponEditScreenContentPreview() {
    ConKeepTheme {
        CouponEditScreenContent(
            isCouponModifiedChecker = { false },
            onBackClick = {},
            onCouponSave = {},
            onImagePickClick = {},
            setNewBrandName = {},
            setNewProductName = {},
            setNewPinNumber = {},
            setNewExpiryDate = {},
            setNewAmount = {},
            toggleMonetary = {},
            setNewMemo = {},
            couponUiModel = fakeCoupon,
            selectedImageUri = null,
        )
    }
}
