package sample.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun NativeMap(locations: List<StoredLocation>, modifier: Modifier = Modifier)
