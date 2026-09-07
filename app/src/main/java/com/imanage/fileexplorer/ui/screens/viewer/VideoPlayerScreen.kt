package com.imanage.fileexplorer.ui.screens.viewer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.imanage.fileexplorer.util.formatFileSize
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

enum class VideoAspectMode(val label: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Zoom / Fill", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    FIXED_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
}

enum class OrientationState(val label: String, val icon: ImageVector, val orientation: Int) {
    SENSOR("Auto Rotate", Icons.Default.ScreenRotation, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED),
    LANDSCAPE("Landscape", Icons.Default.StayCurrentLandscape, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
    PORTRAIT("Portrait", Icons.Default.StayCurrentPortrait, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    filePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val file = remember(filePath) { File(filePath) }

    // Audio Manager for volume gestures
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }

    // Playback state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    // Video metadata
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    // UI overlays & HUD state
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(VideoAspectMode.FIT) }
    var orientationState by remember { mutableStateOf(OrientationState.SENSOR) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Gesture HUDs
    var brightnessHud by remember { mutableStateOf<Float?>(null) } // 0f to 1f
    var volumeHud by remember { mutableStateOf<Int?>(null) } // 0 to maxVolume
    var doubleTapSeekLeft by remember { mutableStateOf(false) }
    var doubleTapSeekRight by remember { mutableStateOf(false) }

    // Sheets & Dialogs
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSpecsDialog by remember { mutableStateOf(false) }

    // Initial brightness extraction
    val initialBrightness = remember {
        val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        if (windowBrightness in 0f..1f) windowBrightness else 0.5f
    }
    var currentBrightness by remember { mutableFloatStateOf(initialBrightness) }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }
        }
        exoPlayer.addListener(listener)

        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = originalOrientation
            // Restore window brightness
            activity?.let { act ->
                val layoutParams = act.window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                act.window.attributes = layoutParams
            }
        }
    }

    // Periodic position updater
    LaunchedEffect(isPlaying, isSeeking) {
        while (true) {
            if (!isSeeking) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(300)
        }
    }

    // Auto-hide controls after 3.5s of inactivity when playing
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(3500)
            showControls = false
        }
    }

    // Clear gesture HUDs after inactivity
    LaunchedEffect(brightnessHud) {
        if (brightnessHud != null) {
            delay(1200)
            brightnessHud = null
        }
    }
    LaunchedEffect(volumeHud) {
        if (volumeHud != null) {
            delay(1200)
            volumeHud = null
        }
    }
    LaunchedEffect(doubleTapSeekLeft) {
        if (doubleTapSeekLeft) {
            delay(650)
            doubleTapSeekLeft = false
        }
    }
    LaunchedEffect(doubleTapSeekRight) {
        if (doubleTapSeekRight) {
            delay(650)
            doubleTapSeekRight = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ─── 1. ExoPlayer Surface ───
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exoPlayer
                    useController = false
                    resizeMode = aspectMode.resizeMode
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = aspectMode.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // ─── 2. Gesture Touch Layer ───
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = constraints.maxWidth.toFloat()
            val screenHeight = constraints.maxHeight.toFloat()

            var dragStartX by remember { mutableFloatStateOf(0f) }
            var dragAccumY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Tap gestures: single tap (toggle controls), double tap (seek)
                    .pointerInput(isLocked) {
                        if (isLocked) {
                            detectTapGestures(
                                onTap = { showControls = !showControls }
                            )
                        } else {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { offset ->
                                    val tapX = offset.x
                                    if (tapX < screenWidth * 0.4f) {
                                        // Seek back 10s
                                        val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                        exoPlayer.seekTo(newPos)
                                        currentPosition = newPos
                                        doubleTapSeekLeft = true
                                    } else if (tapX > screenWidth * 0.6f) {
                                        // Seek forward 10s
                                        val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                                        exoPlayer.seekTo(newPos)
                                        currentPosition = newPos
                                        doubleTapSeekRight = true
                                    } else {
                                        // Middle double tap: toggle play/pause
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    }
                                }
                            )
                        }
                    }
                    // Vertical Drag Gestures: Left side brightness, Right side volume
                    .pointerInput(isLocked) {
                        if (isLocked) return@pointerInput

                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                dragAccumY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumY -= dragAmount // Dragging up increases value

                                val isLeftSide = dragStartX < (screenWidth / 2f)
                                val deltaFraction = dragAccumY / (screenHeight * 0.7f)

                                if (isLeftSide) {
                                    // Brightness Adjustment
                                    val newBrightness = (currentBrightness + deltaFraction * 0.05f).coerceIn(0.01f, 1f)
                                    currentBrightness = newBrightness
                                    brightnessHud = newBrightness
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        lp.screenBrightness = newBrightness
                                        act.window.attributes = lp
                                    }
                                } else {
                                    // Volume Adjustment
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val volDelta = if (dragAccumY > 30f) 1 else if (dragAccumY < -30f) -1 else 0
                                    if (volDelta != 0) {
                                        dragAccumY = 0f
                                        val newVol = (currentVol + volDelta).coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        volumeHud = newVol
                                    }
                                }
                            }
                        )
                    }
            )
        }

        // ─── 3. Gesture HUD Badges (Brightness, Volume, Double-Tap) ───
        // Brightness HUD
        AnimatedVisibility(
            visible = brightnessHud != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp)
        ) {
            brightnessHud?.let { b ->
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = if (b > 0.6f) Icons.Default.BrightnessHigh else if (b > 0.2f) Icons.Default.BrightnessMedium else Icons.Default.BrightnessLow,
                            contentDescription = "Brightness",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(b)
                                    .background(Color(0xFFFFD54F))
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${(b * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Volume HUD
        AnimatedVisibility(
            visible = volumeHud != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp)
        ) {
            volumeHud?.let { v ->
                val fraction = if (maxVolume > 0) v.toFloat() / maxVolume else 0f
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = if (v == 0) Icons.Default.VolumeMute else if (fraction > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Volume",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .background(Color(0xFF64B5F6))
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${(fraction * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Double-Tap Left Seek Ripple (-10s)
        AnimatedVisibility(
            visible = doubleTapSeekLeft,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "-10s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Double-Tap Right Seek Ripple (+10s)
        AnimatedVisibility(
            visible = doubleTapSeekRight,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "+10s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ─── 4. Screen Lock Indicator ───
        if (isLocked) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        isLocked = false
                        showControls = true
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Unlock Screen", modifier = Modifier.size(28.dp))
                }
            }
        }

        // ─── 5. Main Controls Overlay (Top bar & Bottom bar) ───
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Text(
                            text = file.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (videoWidth > 0 && videoHeight > 0) {
                            Text(
                                text = "${videoWidth}x${videoHeight} · ${formatFileSize(file.length())}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Aspect Ratio Button
                    IconButton(onClick = {
                        val modes = VideoAspectMode.values()
                        val nextIdx = (aspectMode.ordinal + 1) % modes.size
                        aspectMode = modes[nextIdx]
                    }) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio (${aspectMode.label})",
                            tint = Color.White
                        )
                    }

                    // Playback Speed Button
                    TextButton(onClick = { showSpeedDialog = true }) {
                        Text(
                            text = if (playbackSpeed == 1.0f) "1.0x" else "${playbackSpeed}x",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Picture-in-Picture Button
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = {
                            val rational = if (videoWidth > 0 && videoHeight > 0) {
                                val clampedW = videoWidth.coerceIn((videoHeight * 0.41841).toInt(), (videoHeight * 2.39).toInt())
                                Rational(clampedW, videoHeight)
                            } else Rational(16, 9)

                            try {
                                val pipParams = PictureInPictureParams.Builder()
                                    .setAspectRatio(rational)
                                    .build()
                                activity?.enterPictureInPictureMode(pipParams)
                            } catch (e: Exception) { }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture-in-Picture",
                                tint = Color.White
                            )
                        }
                    }

                    // Media Info Specs Button
                    IconButton(onClick = { showSpecsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Media Specs",
                            tint = Color.White
                        )
                    }

                    // Lock Screen Button
                    IconButton(onClick = {
                        isLocked = true
                        showControls = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = Color.White
                        )
                    }
                }

                // Middle Center Play/Pause Large Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play/Pause Main
                        FilledIconButton(
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.9f),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Timeline Slider & Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatDuration(if (isSeeking) seekPosition.toLong() else currentPosition),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = if (isSeeking) seekPosition else currentPosition.toFloat(),
                            onValueChange = { pos ->
                                isSeeking = true
                                seekPosition = pos
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(seekPosition.toLong())
                                currentPosition = seekPosition.toLong()
                                isSeeking = false
                            },
                            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = formatDuration(duration),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bottom Row Quick Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Aspect Display
                        AssistChip(
                            onClick = {
                                val modes = VideoAspectMode.values()
                                val nextIdx = (aspectMode.ordinal + 1) % modes.size
                                aspectMode = modes[nextIdx]
                            },
                            label = { Text(aspectMode.label, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.12f)
                            ),
                            border = null
                        )

                        // Screen Rotation Toggle
                        IconButton(onClick = {
                            val nextState = when (orientationState) {
                                OrientationState.SENSOR -> OrientationState.LANDSCAPE
                                OrientationState.LANDSCAPE -> OrientationState.PORTRAIT
                                OrientationState.PORTRAIT -> OrientationState.SENSOR
                            }
                            orientationState = nextState
                            activity?.requestedOrientation = nextState.orientation
                        }) {
                            Icon(
                                imageVector = orientationState.icon,
                                contentDescription = orientationState.label,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // ─── 6. Playback Speed Selector Dialog ───
    if (showSpeedDialog) {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Text("Playback Speed", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (playbackSpeed == speed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .pointerInput(speed) {
                                    detectTapGestures {
                                        playbackSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                color = if (playbackSpeed == speed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (playbackSpeed == speed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // ─── 7. Media Specs Inspector Dialog ───
    if (showSpecsDialog) {
        val videoFormat = exoPlayer.videoFormat
        val audioFormat = exoPlayer.audioFormat

        AlertDialog(
            onDismissRequest = { showSpecsDialog = false },
            icon = {
                Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("Media Details", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecItem(label = "Filename", value = file.name)
                    SpecItem(label = "Resolution", value = if (videoWidth > 0) "${videoWidth} x ${videoHeight} px" else "Unknown")
                    SpecItem(label = "Duration", value = formatDuration(duration))
                    SpecItem(label = "File Size", value = formatFileSize(file.length()))
                    SpecItem(label = "Video Codec", value = videoFormat?.sampleMimeType ?: "Hardware Decoded")
                    SpecItem(label = "Frame Rate", value = if ((videoFormat?.frameRate ?: 0f) > 0) "${videoFormat?.frameRate?.toInt()} fps" else "Variable")
                    SpecItem(label = "Audio Codec", value = audioFormat?.sampleMimeType ?: "Standard Stream")
                    SpecItem(label = "Audio Channels", value = if ((audioFormat?.channelCount ?: 0) > 0) "${audioFormat?.channelCount} ch (${audioFormat?.sampleRate ?: 0} Hz)" else "Stereo")
                    SpecItem(label = "Storage Path", value = file.absolutePath)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpecsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
