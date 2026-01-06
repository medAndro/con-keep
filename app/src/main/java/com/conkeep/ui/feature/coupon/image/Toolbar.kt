package com.conkeep.ui.feature.coupon.image

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Black,
) {
    TopAppBar(
        modifier = modifier.padding(horizontal = 24.dp),
        title = { },

        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(45.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = stringResource(R.string.topbar_back_description),
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
        },

        actions = {
            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(45.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = stringResource(R.string.topbar_share_description),
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.size(45.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_save),
                    contentDescription = stringResource(R.string.topbar_save_description),
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
        },

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0x00000000)
@Composable
private fun MainToolbarPreview() {
    Toolbar(
        onBackClick = { },
        onSaveClick = { },
        onShareClick = { },
        iconTint = Color.Black,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MainToolbarDarkPreview() {
    Toolbar(
        onBackClick = { },
        onSaveClick = { },
        onShareClick = { },
        iconTint = Color.White,
    )
}
