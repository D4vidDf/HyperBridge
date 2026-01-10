package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


sealed class ShapeStyle(
    val topRadius: Dp,
    val bottomRadius: Dp
) {
    data object None : ShapeStyle(topRadius = 0.dp, bottomRadius = 0.dp)
    data object ExtraSmall : ShapeStyle(topRadius = 2.dp, bottomRadius = 1.dp)
    data object Small : ShapeStyle(topRadius = 4.dp, bottomRadius = 2.dp)
    data object Medium : ShapeStyle(topRadius = 15.dp, bottomRadius = 5.dp)
    data object Large : ShapeStyle(topRadius = 24.dp, bottomRadius = 4.dp)
    data object ExtraLarge : ShapeStyle(topRadius = 48.dp, bottomRadius = 16.dp)
}

fun getExpressiveShape(
    groupSize: Int,
    index: Int,
    style: ShapeStyle = ShapeStyle.Large
): Shape {
    if (groupSize <= 1) return RoundedCornerShape(style.topRadius)

    val large = style.topRadius
    val small = style.bottomRadius

    return when (index) {
        0 -> RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomEnd = small,
            bottomStart = small
        )
        groupSize - 1 -> RoundedCornerShape(
            topStart = small,
            topEnd = small,
            bottomEnd = large,
            bottomStart = large
        )
        else -> RoundedCornerShape(small)
    }
}