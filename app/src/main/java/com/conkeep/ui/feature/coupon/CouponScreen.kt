package com.conkeep.ui.feature.coupon

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.navigation.Route
import com.conkeep.ui.feature.coupon.component.CouponCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: CouponViewModel = hiltViewModel(),
) {
    val coupons by viewModel.coupons.collectAsState()

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val pickMedia =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let {
                scope.launch {
                    val savedPath = saveImageToAppStorage(context, uri)
                    if (savedPath != null) {
                        viewModel.addDummyCoupon(savedPath)
                    }
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 쿠폰") },
                actions = {
                    Button(onClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    }) {
                        Text("+")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
        ) {
            items(coupons) { coupon ->
                CouponCard(
                    couponUiModel = coupon,
                    onClick = {
                        backStack.add(Route.CouponDetailScreen(id = coupon.id))
                    },
                )
            }
        }
    }
}

suspend fun saveImageToAppStorage(
    context: Context,
    uri: Uri,
): String? =
    withContext(Dispatchers.IO) {
        try {
            // 1. MIME 타입 확인
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") != true) {
                return@withContext null
            }

            // 2. 파일 확장자 추출
            val extension =
                MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(mimeType) ?: "jpg"

            // 3. 앱 내부 저장소에 파일 생성
            val fileName = "coupon_${System.currentTimeMillis()}.$extension"
            val destinationFile = File(context.filesDir, fileName)

            // 4. URI에서 InputStream으로 읽어서 파일로 복사
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 5. 저장된 파일 경로 반환 (Room DB에 저장할 경로)
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
