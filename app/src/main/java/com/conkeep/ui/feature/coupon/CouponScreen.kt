package com.conkeep.ui.feature.coupon

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
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.navigation.Route
import com.conkeep.ui.feature.coupon.component.CouponCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: CouponViewModel = hiltViewModel(),
) {
    val coupons by viewModel.coupons.collectAsState()

    val pickMedia =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let {
                viewModel.addDummyCouponFromUri(uri)
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
