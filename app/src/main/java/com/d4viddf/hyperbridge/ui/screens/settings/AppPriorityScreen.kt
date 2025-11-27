package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.util.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PriorityAppInfo(val name: String, val packageName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPriorityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }
    val packageManager = context.packageManager

    val enabledPackages by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val savedOrder by preferences.appPriorityListFlow.collectAsState(initial = emptyList())

    val displayList = remember { mutableStateListOf<PriorityAppInfo>() }
    var isLoaded by remember { mutableStateOf(false) }

    // Load Data
    LaunchedEffect(enabledPackages, savedOrder) {
        if (enabledPackages.isEmpty()) {
            displayList.clear()
            isLoaded = true
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val ordered = savedOrder.filter { enabledPackages.contains(it) }
            val newApps = enabledPackages.filter { !savedOrder.contains(it) }
            val finalPackageList = ordered + newApps

            val infoList = finalPackageList.map { pkg ->
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    PriorityAppInfo(name, pkg)
                } catch (e: Exception) {
                    PriorityAppInfo(pkg, pkg)
                }
            }

            withContext(Dispatchers.Main) {
                displayList.clear()
                displayList.addAll(infoList)
                isLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.priority_order)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.priority_order_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoaded) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayList.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_active_bridges), color = MaterialTheme.colorScheme.error)
                }
            } else {
                DraggableLazyList(
                    items = displayList,
                    onMove = { from, to ->
                        displayList.apply { add(to, removeAt(from)) }
                    },
                    onDragEnd = {
                        scope.launch {
                            preferences.setAppPriorityOrder(displayList.map { it.packageName })
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DraggableLazyList(
    items: List<PriorityAppInfo>,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit
) {
    val listState = rememberLazyListState()

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Auto-scroll config
    val density = LocalDensity.current
    val scrollThreshold = with(density) { 50.dp.toPx() } // Zone size at top/bottom
    var scrollVelocity by remember { mutableFloatStateOf(0f) }

    // AUTO-SCROLL LOOP
    LaunchedEffect(scrollVelocity) {
        if (scrollVelocity != 0f) {
            while (true) {
                listState.scrollBy(scrollVelocity)
                delay(16) // ~60fps frame time
                if (scrollVelocity == 0f) break
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // Identify item under finger
                        listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
                            ?.let {
                                draggingIndex = it.index
                                dragOffset = 0f
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y

                        // --- SMART SCROLL LOGIC ---
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        val touchY = change.position.y

                        // Check if we are in the Top Zone
                        if (touchY < scrollThreshold) {
                            // Only scroll UP if the user isn't actively dragging DOWN
                            // and we aren't already at the top
                            scrollVelocity = if (listState.canScrollBackward && dragAmount.y <= 0) -20f else 0f
                        }
                        // Check if we are in the Bottom Zone
                        else if (touchY > viewportHeight - scrollThreshold) {
                            // Only scroll DOWN if user isn't dragging UP
                            // and we aren't already at the bottom
                            scrollVelocity = if (listState.canScrollForward && dragAmount.y >= 0) 20f else 0f
                        } else {
                            scrollVelocity = 0f
                        }

                        // --- SWAP LOGIC ---
                        val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                        val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == currentIndex }

                        if (currentItemInfo != null) {
                            val itemHeight = currentItemInfo.size
                            val threshold = itemHeight * 0.5f

                            // Move Down
                            if (dragOffset > threshold) {
                                if (currentIndex < items.lastIndex) {
                                    onMove(currentIndex, currentIndex + 1)
                                    draggingIndex = currentIndex + 1
                                    dragOffset -= itemHeight
                                }
                            }
                            // Move Up
                            else if (dragOffset < -threshold) {
                                if (currentIndex > 0) {
                                    onMove(currentIndex, currentIndex - 1)
                                    draggingIndex = currentIndex - 1
                                    dragOffset += itemHeight
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        draggingIndex = null
                        dragOffset = 0f
                        scrollVelocity = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        draggingIndex = null
                        dragOffset = 0f
                        scrollVelocity = 0f
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items, key = { _, item -> item.packageName }) { index, item ->

            val isDragging = index == draggingIndex
            // Visual Pop
            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
            val alpha = if (isDragging) 0.9f else 1f

            val areButtonsEnabled = draggingIndex == null

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationY = if (isDragging) dragOffset else 0f
                    }
                    .shadow(elevation, RoundedCornerShape(12.dp))
                    // Only animate items that are NOT being dragged
                    .then(if (!isDragging) Modifier.animateItem() else Modifier)
            ) {
                PriorityAppItem(
                    rank = index + 1,
                    app = item,
                    isFirst = index == 0,
                    isLast = index == items.lastIndex,
                    areButtonsEnabled = areButtonsEnabled,
                    onMoveUp = {
                        if (index > 0) {
                            onMove(index, index - 1)
                            onDragEnd()
                        }
                    },
                    onMoveDown = {
                        if (index < items.lastIndex) {
                            onMove(index, index + 1)
                            onDragEnd()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PriorityAppItem(
    rank: Int,
    app: PriorityAppInfo,
    isFirst: Boolean,
    isLast: Boolean,
    areButtonsEnabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val buttonTint = if (areButtonsEnabled) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle (Visual)
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = if (areButtonsEnabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                modifier = Modifier
                    .size(48.dp)
                    .padding(12.dp)
            )

            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )

            AppIconLoader(packageName = app.packageName)

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = app.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            // Buttons (Transparent when dragging to keep layout stable)
            IconButton(
                onClick = onMoveUp,
                modifier = Modifier.size(32.dp),
                enabled = areButtonsEnabled && !isFirst
            ) {
                if (!isFirst) {
                    Icon(Icons.Default.ArrowUpward, stringResource(R.string.move_up), tint = buttonTint, modifier = Modifier.size(20.dp))
                }
            }

            IconButton(
                onClick = onMoveDown,
                modifier = Modifier.size(32.dp),
                enabled = areButtonsEnabled && !isLast
            ) {
                if (!isLast) {
                    Icon(Icons.Default.ArrowDownward, stringResource(R.string.move_down), tint = buttonTint, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun AppIconLoader(packageName: String) {
    val context = LocalContext.current
    var iconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                iconBitmap = drawable.toBitmap()
            } catch (e: Exception) { }
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}