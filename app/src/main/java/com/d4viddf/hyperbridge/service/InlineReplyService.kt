package com.d4viddf.hyperbridge.service

import android.app.PendingIntent
import android.app.RemoteInput
import android.app.Service
import android.content.Intent
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import androidx.core.graphics.toColorInt

fun safeParseColorFallback(hex: String?, fallback: Color): Color {
    if (hex == null) return fallback
    return try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        fallback
    }
}

class InlineReplyService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = intent?.getParcelableExtra<PendingIntent>("pending_intent")
        val resultKey = intent?.getStringExtra("result_key")
        val packageName = intent?.getStringExtra("package_name") ?: ""

        if (pendingIntent == null || resultKey == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(pendingIntent, resultKey, packageName)
        return START_NOT_STICKY
    }

    private fun showOverlay(pendingIntent: PendingIntent, resultKey: String, packageName: String) {
        if (composeView != null) return

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@InlineReplyService)
            setViewTreeViewModelStoreOwner(this@InlineReplyService)
            setViewTreeSavedStateRegistryOwner(this@InlineReplyService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val preferences = remember { com.d4viddf.hyperbridge.data.AppPreferences(this@InlineReplyService) }
                val themeRepository = remember { com.d4viddf.hyperbridge.data.theme.ThemeRepository(this@InlineReplyService) }
                val activeThemeId by preferences.activeThemeIdFlow.collectAsState(initial = null)
                val activeTheme by themeRepository.activeTheme.collectAsState()

                LaunchedEffect(activeThemeId) {
                    activeThemeId?.let { themeRepository.activateTheme(it) }
                }

                val replyModule = activeTheme?.apps?.get(packageName)?.replyConfig ?: activeTheme?.defaultReply
                
                val useBlur = replyModule?.useBlur ?: true
                val tfCornerRadius = replyModule?.textfieldCornerRadius ?: 24
                val btnCornerRadius = replyModule?.sendButtonCornerRadius ?: 24

                val tfBgColorHex = replyModule?.textfieldBackgroundColor
                val sendColorHex = replyModule?.sendColor
                val txtColorHex = replyModule?.textColor
                val colorMode = replyModule?.colorMode ?: com.d4viddf.hyperbridge.models.theme.ColorMode.DEFAULT

                HyperBridgeTheme {
                    var message by remember { mutableStateOf("") }
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        try {
                            focusRequester.requestFocus()
                        } catch (e: Exception) {
                            Log.e("InlineReplyService", "Focus failed", e)
                        }
                    }

                    val isMaterialYou = colorMode == com.d4viddf.hyperbridge.models.theme.ColorMode.MATERIAL_YOU
                    val tfBgColor = if (isMaterialYou) MaterialTheme.colorScheme.surfaceVariant else (tfBgColorHex?.let { safeParseColorFallback(it, MaterialTheme.colorScheme.surfaceVariant) } ?: MaterialTheme.colorScheme.surfaceVariant)
                    val sendParsedColor = if (isMaterialYou) MaterialTheme.colorScheme.primary else (sendColorHex?.let { safeParseColorFallback(it, MaterialTheme.colorScheme.primary) } ?: MaterialTheme.colorScheme.primary)
                    val txtColor = if (isMaterialYou) MaterialTheme.colorScheme.onSurface else (txtColorHex?.let { safeParseColorFallback(it, MaterialTheme.colorScheme.onSurface) } ?: MaterialTheme.colorScheme.onSurface)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                removeOverlayAndStop()
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            var tfRect by remember { mutableStateOf(android.graphics.Rect()) }
                            var btnRect by remember { mutableStateOf(android.graphics.Rect()) }

                            val currentView = LocalView.current
                            val displayMetrics = currentView.resources.displayMetrics
                            
                            LaunchedEffect(tfRect, btnRect, tfCornerRadius, btnCornerRadius, useBlur) {
                                if (useBlur && !tfRect.isEmpty && !btnRect.isEmpty) {
                                    currentView.outlineProvider = object : ViewOutlineProvider() {
                                        override fun getOutline(view: android.view.View, outline: Outline) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                val path = android.graphics.Path()
                                                path.addRoundRect(
                                                    android.graphics.RectF(tfRect),
                                                    tfCornerRadius.toFloat() * displayMetrics.density,
                                                    tfCornerRadius.toFloat() * displayMetrics.density,
                                                    android.graphics.Path.Direction.CW
                                                )
                                                path.addRoundRect(
                                                    android.graphics.RectF(btnRect),
                                                    btnCornerRadius.toFloat() * displayMetrics.density,
                                                    btnCornerRadius.toFloat() * displayMetrics.density,
                                                    android.graphics.Path.Direction.CW
                                                )
                                                outline.setPath(path)
                                            }
                                        }
                                    }
                                    val wParams = currentView.layoutParams as? WindowManager.LayoutParams
                                    if (wParams != null) {
                                        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            wParams.blurBehindRadius = 50
                                        }
                                        try { windowManager?.updateViewLayout(currentView, wParams) } catch (_: Exception) {}
                                    }
                                } else {
                                    currentView.outlineProvider = null
                                    val wParams = currentView.layoutParams as? WindowManager.LayoutParams
                                    if (wParams != null) {
                                        wParams.flags = wParams.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                                        try { windowManager?.updateViewLayout(currentView, wParams) } catch (_: Exception) {}
                                    }
                                }
                            }

                            
                            Box(modifier = Modifier
                                .weight(1f)
                                .heightIn(
                                    min = 56.dp,
                                    max = 200.dp
                                )
                                .onGloballyPositioned { coords ->
                                    val b = coords.boundsInWindow()
                                    tfRect = android.graphics.Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                                }
                            ) {
                                // Background Layer
                                Box(modifier = Modifier
                                    .matchParentSize()
                                    .background(if (useBlur) tfBgColor.copy(alpha = 0.5f) else tfBgColor, RoundedCornerShape(tfCornerRadius.dp))
                                )
                                
                                // Content Layer (Sharp)
                                TextField(
                                    value = message,
                                    onValueChange = { message = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.d4viddf.hyperbridge.R.string.reply_hint)) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = txtColor),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    ),
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            sendReply(pendingIntent, resultKey, message)
                                        }
                                    )
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(56.dp)
                                    .onGloballyPositioned { coords ->
                                        val b = coords.boundsInWindow()
                                        btnRect = android.graphics.Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                                    }
                            ) {
                                // Background Layer
                                Box(modifier = Modifier
                                    .matchParentSize()
                                    .background(if (useBlur) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(btnCornerRadius.dp))
                                )
                                
                                // Content Layer (Sharp)
                                IconButton(
                                    onClick = { sendReply(pendingIntent, resultKey, message) },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (message.isNotBlank()) sendParsedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    removeOverlayAndStop()
                    true
                } else {
                    false
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            // We do NOT use FLAG_NOT_FOCUSABLE because we need the keyboard to open!
            // But we must be careful to consume back presses to allow the user to exit.
        }

        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            Log.e("InlineReplyService", "Failed to add overlay. SYSTEM_ALERT_WINDOW permission missing?", e)
            stopSelf()
        }
    }

    private fun sendReply(pendingIntent: PendingIntent, resultKey: String, message: String) {
        if (message.isBlank()) return
        
        try {
            val replyIntent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(resultKey, message)
            
            val remoteInput = RemoteInput.Builder(resultKey).build()
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, bundle)
            
            pendingIntent.send(this, 0, replyIntent)
        } catch (e: PendingIntent.CanceledException) {
            Log.e("InlineReplyService", "PendingIntent canceled", e)
        }
        
        removeOverlayAndStop()
    }

    private fun removeOverlayAndStop() {
        try {
            composeView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e("InlineReplyService", "Failed to remove view", e)
        }
        composeView = null
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayAndStop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
