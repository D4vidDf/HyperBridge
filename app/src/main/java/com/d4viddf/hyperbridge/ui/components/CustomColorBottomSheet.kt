package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.d4viddf.hyperbridge.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorBottomSheet(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorAdded: (Color) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Extract initial HSL
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(initialColor.toArgb(), hsl)

    var hue by remember { mutableFloatStateOf(hsl[0]) }        // 0..360
    var saturation by remember { mutableFloatStateOf(hsl[1]) } // 0..1
    var lightness by remember { mutableFloatStateOf(hsl[2]) }  // 0..1

    // Convert current HSL back to Compose Color for the local preview
    val currentColor = remember(hue, saturation, lightness) {
        Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness)))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Header (Centered Title with Left-Aligned Close Button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }

                Text(
                    text = stringResource(R.string.colors_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 1. Hue Slider
            Text(
                text = stringResource(R.string.colors_label_hue),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            GradientSlider(
                value = hue,
                onValueChange = { hue = it }, // ONLY updates local state
                valueRange = 0f..360f,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Saturation Slider
            Text(
                text = stringResource(R.string.colors_label_saturation),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            GradientSlider(
                value = saturation,
                onValueChange = { saturation = it }, // ONLY updates local state
                valueRange = 0f..1f,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0f, lightness))),
                        Color(ColorUtils.HSLToColor(floatArrayOf(hue, 1f, lightness)))
                    )
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Lightness Slider
            Text(
                text = stringResource(R.string.colors_label_lightness),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            GradientSlider(
                value = lightness,
                onValueChange = { lightness = it }, // ONLY updates local state
                valueRange = 0f..1f,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black,
                        Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, 0.5f))),
                        Color.White
                    )
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 4. Preview and Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live preview of the constructed color
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { onColorAdded(currentColor) },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(stringResource(R.string.colors_action_done))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}