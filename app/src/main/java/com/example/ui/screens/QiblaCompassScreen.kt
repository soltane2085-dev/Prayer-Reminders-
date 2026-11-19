package com.example.ui.screens

import android.content.Context
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calculator.CityPreset
import com.example.calculator.CompassSensorState
import com.example.calculator.QiblaCompassSensorManager
import com.example.calculator.QiblaHelper
import com.example.ui.translation.Translation
import com.example.viewmodel.PrayerViewModel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaCompassScreen(viewModel: PrayerViewModel, context: Context) {
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }

    val customLat by viewModel.customLat.collectAsStateWithLifecycle()
    val customLon by viewModel.customLon.collectAsStateWithLifecycle()
    val customName by viewModel.customCityName.collectAsStateWithLifecycle()
    val customEnabled by viewModel.customLocEnabled.collectAsStateWithLifecycle()
    val currentCityIdx by viewModel.cityPresetIdx.collectAsStateWithLifecycle()

    val activeLat: Double = if (customEnabled) customLat.toDouble() else CityPreset.presets[currentCityIdx].latitude
    val activeLon: Double = if (customEnabled) customLon.toDouble() else CityPreset.presets[currentCityIdx].longitude
    val activeName = if (customEnabled) customName else CityPreset.presets[currentCityIdx].cityName

    val qiblaAngle = remember(activeLat, activeLon) {
        QiblaHelper.calculateQiblaDirection(activeLat, activeLon)
    }
    val distanceKm = remember(activeLat, activeLon) {
        QiblaHelper.calculateDistanceToKaaba(activeLat, activeLon)
    }
    val distanceMiles = remember(activeLat, activeLon) {
        QiblaHelper.calculateDistanceToKaabaMiles(activeLat, activeLon)
    }

    // Sensor Manager setup
    val sensorManager = remember { QiblaCompassSensorManager(context) }
    val compassState by sensorManager.compassState.collectAsStateWithLifecycle()

    var useManualDrag by remember { mutableStateOf(!compassState.hasMagnetometer) }
    var manualHeading by remember { mutableStateOf(0f) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    val displayHeading = if (useManualDrag) manualHeading else compassState.heading

    // Smooth compass rotation animation
    val animatedHeading by animateFloatAsState(
        targetValue = displayHeading,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "compass_heading"
    )

    val signedAngleDiff = remember(qiblaAngle, displayHeading) {
        QiblaHelper.calculateSignedAngleDiff(qiblaAngle, displayHeading)
    }
    val absAngleDiff = abs(signedAngleDiff)
    val isAligned = absAngleDiff <= 3.5f

    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(isAligned) {
        if (isAligned) {
            try {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (e: Exception) {
                // Ignore missing hardware gracefully
            }
        }
    }

    if (showCalibrationDialog) {
        CalibrationGuideDialog(
            appLang = appLang,
            onDismiss = { showCalibrationDialog = false }
        )
    }

    val isIslamicArt = com.example.ui.theme.LocalIsIslamicArtTheme.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isIslamicArt) Color(0xFFD4AF37).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
    ) {
        // 1. Qibla Compass Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qibla_compass_card"),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isAligned) Color(0xFF10B981).copy(alpha = 0.7f) else borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = t("qibla_title").uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 1.4.sp
                            )
                            Text(
                                text = activeName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = onSurfaceColor
                            )
                            Text(
                                text = t("coordinates")
                                    .replace("{lat}", String.format("%.4f", activeLat))
                                    .replace("{lon}", String.format("%.4f", activeLon)),
                                fontSize = 11.sp,
                                color = onSurfaceVariantColor
                            )
                        }

                        // Sensor accuracy pill / Calibration button
                        AssistChip(
                            onClick = { showCalibrationDialog = true },
                            label = {
                                Text(
                                    text = when (compassState.accuracy) {
                                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "★ " + t("sensor_accuracy_high").take(14)
                                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> t("sensor_accuracy_medium").take(14)
                                        else -> "⚠ " + t("calibrate_btn")
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when (compassState.accuracy) {
                                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF0F4E38)
                                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFF3A3816)
                                    else -> Color(0xFF4A1E24)
                                },
                                labelColor = when (compassState.accuracy) {
                                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF34D399)
                                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFFBBF24)
                                    else -> Color(0xFFF87171)
                                }
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = when (compassState.accuracy) {
                                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF10B981)
                                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                            ),
                            modifier = Modifier.testTag("btn_compass_calibration")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color(0xFF2E3162))
                    Spacer(modifier = Modifier.height(18.dp))

                    // Compass Dial View with Magnetometer + Accelerometer tilt bubble
                    var compassCenter by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates ->
                                    val size = coordinates.size
                                    compassCenter = Offset(size.width / 2f, size.height / 2f)
                                }
                                .pointerInput(useManualDrag) {
                                    if (useManualDrag) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val startPoint = change.position - dragAmount - compassCenter
                                            val endPoint = change.position - compassCenter
                                            val startAngle = atan2(startPoint.y, startPoint.x)
                                            val endAngle = atan2(endPoint.y, endPoint.x)
                                            val deltaAngle = Math.toDegrees((endAngle - startAngle).toDouble())
                                            manualHeading = ((manualHeading - deltaAngle.toFloat()) + 360f) % 360f
                                        }
                                    }
                                }
                                .testTag("qibla_compass_canvas")
                        ) {
                            val radius = size.minDimension / 2f
                            val center = size.center

                            // 1. Outer Chassis with Alignment Glow
                            drawCircle(
                                color = if (isAligned) Color(0xFF10B981) else Color(0xFF2E3162),
                                radius = radius,
                                style = Stroke(width = 4.dp.toPx())
                            )

                            if (isAligned) {
                                drawCircle(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    radius = radius - 6.dp.toPx(),
                                    style = Fill
                                )
                            }

                            // 2. Rotating Compass Rose
                            rotate(-animatedHeading, center) {
                                // Background subtle circle
                                drawCircle(
                                    color = Color(0xFF13152C),
                                    radius = radius - 6.dp.toPx(),
                                    style = Fill
                                )

                                // Dial Degree Ticks
                                for (angle in 0 until 360 step 5) {
                                    val angleRad = Math.toRadians(angle.toDouble())
                                    val isCardinal = angle % 90 == 0
                                    val isMajor = angle % 30 == 0
                                    val isMinor = !isMajor

                                    val tickLength = when {
                                        isCardinal -> 14.dp.toPx()
                                        isMajor -> 9.dp.toPx()
                                        else -> 5.dp.toPx()
                                    }
                                    val tickWidth = when {
                                        isCardinal -> 3.dp.toPx()
                                        isMajor -> 2.dp.toPx()
                                        else -> 1.dp.toPx()
                                    }
                                    val tickColor = when {
                                        angle == 0 -> Color(0xFFEF4444) // North in Red
                                        isCardinal -> Color(0xFFF8FAFC)
                                        isMajor -> Color(0xFF94A3B8)
                                        else -> Color(0xFF475569)
                                    }

                                    val startDist = radius - 10.dp.toPx() - tickLength
                                    val endDist = radius - 10.dp.toPx()

                                    val startX = (center.x + startDist * sin(angleRad)).toFloat()
                                    val startY = (center.y - startDist * cos(angleRad)).toFloat()
                                    val endX = (center.x + endDist * sin(angleRad)).toFloat()
                                    val endY = (center.y - endDist * cos(angleRad)).toFloat()

                                    drawLine(
                                        color = tickColor,
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = tickWidth
                                    )
                                }

                                // Dial Cardinal Letters (N, S, E, W)
                                val textPaint = android.graphics.Paint().apply {
                                    textSize = 13.sp.toPx()
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }

                                drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas

                                    // North
                                    textPaint.color = android.graphics.Color.parseColor("#EF4444")
                                    nativeCanvas.drawText("N", center.x, center.y - radius + 36.dp.toPx(), textPaint)

                                    // South
                                    textPaint.color = android.graphics.Color.WHITE
                                    nativeCanvas.drawText("S", center.x, center.y + radius - 24.dp.toPx(), textPaint)

                                    // East
                                    textPaint.color = android.graphics.Color.WHITE
                                    nativeCanvas.drawText("E", center.x + radius - 24.dp.toPx(), center.y + 5.dp.toPx(), textPaint)

                                    // West
                                    textPaint.color = android.graphics.Color.WHITE
                                    nativeCanvas.drawText("W", center.x - radius + 24.dp.toPx(), center.y + 5.dp.toPx(), textPaint)
                                }

                                // 3. Qibla Needle & Kaaba Icon (pointing at qiblaAngle relative to True North)
                                rotate(qiblaAngle.toFloat(), center) {
                                    val qiblaLineLength = radius * 0.74f

                                    // Needle line
                                    drawLine(
                                        color = if (isAligned) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        start = center,
                                        end = Offset(center.x, center.y - qiblaLineLength),
                                        strokeWidth = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Needle arrow head
                                    val path = Path().apply {
                                        moveTo(center.x, center.y - qiblaLineLength - 6.dp.toPx())
                                        lineTo(center.x - 9.dp.toPx(), center.y - qiblaLineLength + 14.dp.toPx())
                                        lineTo(center.x + 9.dp.toPx(), center.y - qiblaLineLength + 14.dp.toPx())
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (isAligned) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        style = Fill
                                    )

                                    // Kaaba Representation Cube at 55% radius
                                    val cubeSize = 24.dp.toPx()
                                    val cubeY = center.y - qiblaLineLength * 0.58f

                                    // Kaaba Drop Shadow
                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        topLeft = Offset(center.x - cubeSize / 2f + 2.dp.toPx(), cubeY - cubeSize / 2f + 2.dp.toPx()),
                                        size = Size(cubeSize, cubeSize)
                                    )
                                    // Kaaba Black Body
                                    drawRect(
                                        color = Color(0xFF0F1015),
                                        topLeft = Offset(center.x - cubeSize / 2f, cubeY - cubeSize / 2f),
                                        size = Size(cubeSize, cubeSize)
                                    )
                                    // Golden Kiswah Trim
                                    drawRect(
                                        color = Color(0xFFF59E0B),
                                        topLeft = Offset(center.x - cubeSize / 2f, cubeY - cubeSize * 0.18f),
                                        size = Size(cubeSize, cubeSize * 0.18f)
                                    )
                                }
                            }

                            // 4. Center Accelerometer Tilt Bubble Level
                            val maxTiltOffset = radius * 0.28f
                            val bubbleOffsetX = (compassState.roll / 45f).coerceIn(-1f, 1f) * maxTiltOffset
                            val bubbleOffsetY = (compassState.pitch / 45f).coerceIn(-1f, 1f) * maxTiltOffset

                            // Center target ring for levelness
                            drawCircle(
                                color = Color(0xFF2E3162),
                                radius = 22.dp.toPx(),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            // Spirit bubble
                            drawCircle(
                                color = if (compassState.isLevel) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFFF59E0B).copy(alpha = 0.6f),
                                radius = 8.dp.toPx(),
                                center = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY),
                                style = Fill
                            )

                            // 5. Static Phone Forward Sight / Target Reticle (Top Marker)
                            val targetPath = Path().apply {
                                moveTo(center.x, 6.dp.toPx())
                                lineTo(center.x - 10.dp.toPx(), 20.dp.toPx())
                                lineTo(center.x + 10.dp.toPx(), 20.dp.toPx())
                                close()
                            }
                            drawPath(
                                path = targetPath,
                                color = if (isAligned) Color(0xFF10B981) else Color(0xFFEF4444),
                                style = Fill
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tilt spirit level status label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (compassState.isLevel) Icons.Default.ScreenRotation else Icons.Default.ScreenLockRotation,
                            contentDescription = "Tilt Level",
                            tint = if (compassState.isLevel) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (compassState.isLevel) t("spirit_level_flat") else t("spirit_level_tilt"),
                            fontSize = 11.sp,
                            color = if (compassState.isLevel) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Alignment State Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAligned) Color(0xFF0F3A2E) else Color(0xFF222448)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isAligned) Color(0xFF10B981) else Color(0xFF2E3162))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isAligned) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Aligned",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = t("facing_kaaba"),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = t("facing_desc"),
                                    fontSize = 12.sp,
                                    color = Color(0xFFF8FAFC),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                val turnDir = if (signedAngleDiff > 0) t("right") else t("left")
                                val turnIcon = if (signedAngleDiff > 0) Icons.Default.RotateRight else Icons.Default.RotateLeft

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = turnIcon,
                                        contentDescription = "Turn Direction",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = t("rotate_to")
                                            .replace("{dir}", turnDir)
                                            .replace("{angle}", String.format("%.1f", absAngleDiff)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = t("rotate_instruction"),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t("azimuth"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "${String.format("%.1f", qiblaAngle)}° ${QiblaHelper.getCardinalDirection(qiblaAngle.toFloat())}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(26.dp).background(Color(0xFF2E3162)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t("heading"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "${String.format("%.1f", displayHeading)}° ${QiblaHelper.getCardinalDirection(displayHeading)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(26.dp).background(Color(0xFF2E3162)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t("distance"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "${String.format("%,.0f", distanceKm)} ${t("km")}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }
                    }
                }
            }
        }

        // 2. Hardware Missing Warning or Simulation Option
        if (!compassState.hasMagnetometer) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF382319)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Sensor Alert",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = t("no_magnetometer_warn"),
                            fontSize = 12.sp,
                            color = Color(0xFFF8FAFC),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 3. Interactive Touch Simulator Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qibla_compass_simulator_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161832)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3162))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Calibration Settings",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = t("simulator_title"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = t("simulator_desc"),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("manual_drag_toggle"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Switch(
                            checked = useManualDrag,
                            onCheckedChange = { useManualDrag = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFF59E0B),
                                checkedTrackColor = Color(0xFFF59E0B).copy(alpha = 0.4f),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF222448)
                            ),
                            modifier = Modifier.testTag("switch_manual_compass_drag")
                        )
                    }

                    AnimatedVisibility(
                        visible = useManualDrag,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = t("manual_drag_desc"),
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = manualHeading,
                                onValueChange = { manualHeading = it },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF59E0B),
                                    activeTrackColor = Color(0xFFF59E0B),
                                    inactiveTrackColor = Color(0xFF222448)
                                ),
                                modifier = Modifier.testTag("slider_manual_compass_heading")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalibrationGuideDialog(
    appLang: String,
    onDismiss: () -> Unit
) {
    val t = { key: String -> Translation.get(key, appLang) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = t("calibrate_dialog_title"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = t("calibrate_dialog_desc"),
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = "Figure 8 motion",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text("حسناً / OK", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1E2140),
        shape = RoundedCornerShape(20.dp)
    )
}
