package com.conkeep.ui.feature.coupon.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponDetailScreen(
    id: String,
    backStack: NavBackStack<NavKey>,
    viewModel: CouponDetailViewModel,
) {
    val coupon by viewModel.coupon.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("쿠폰 상세") },
                navigationIcon = {
                    Button(onClick = {
                        backStack.removeLastOrNull()
                    }) {
                        Text("←")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
        ) {
            Text("쿠폰 ID: $id", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            coupon.let { coupon ->
                Text("번호: ${coupon?.number}")
                Text("이름: ${coupon?.name}")
                Text("유효기간: ${coupon?.expiryDate}")

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.useCoupon() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("쿠폰 사용하기")
                }
            }
        }
    }
}
