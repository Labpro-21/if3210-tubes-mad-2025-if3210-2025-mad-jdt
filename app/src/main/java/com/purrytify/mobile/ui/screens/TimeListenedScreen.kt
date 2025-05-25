package com.purrytify.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.purrytify.mobile.viewmodel.TimeListenedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeListenedScreen(navController: NavController) {
    val viewModel: TimeListenedViewModel = viewModel()
    val totalMinutes by viewModel.totalMinutes.collectAsState()
    val dailyAverage by viewModel.dailyAverage.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    Brush.verticalGradient(
                                            colors =
                                                    listOf(
                                                            Color(0xFF1E3A8A),
                                                            Color(0xFF1E1B4B),
                                                            Color.Black
                                                    )
                                    )
                            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                        text = "Time listened",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                )
            }

            // Content
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                // Month
                Text(text = currentMonth, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = "You listened to music for",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = "$totalMinutes",
                            color = Color(0xFF1DB954),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "minutes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "this month.",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = "Daily average: $dailyAverage min",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Color(0xFF1DB954)) }
                } else {
                    DailyChart(viewModel = viewModel)
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun DailyChart(viewModel: TimeListenedViewModel) {
    val chartDataPoints = viewModel.getChartDataPoints()

    Column {
        // Chart title
        Text(
                text = "Daily Chart",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chart container
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2A2A2A))
                                .padding(16.dp)
        ) {
            if (chartDataPoints.isEmpty()) {
                // No data state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            text = "No listening data available",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                    )
                }
            } else {
                // Chart canvas
                Canvas(modifier = Modifier.fillMaxSize()) { drawChart(chartDataPoints) }

                // Y-axis label
                Text(
                        text = "minutes",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier =
                                Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp)
                )

                // X-axis label
                Text(
                        text = "day",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier =
                                Modifier.align(Alignment.BottomEnd)
                                        .padding(end = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawChart(dataPoints: List<Pair<Int, Float>>) {
    if (dataPoints.isEmpty()) return

    val chartWidth = size.width - 60f // Leave space for labels
    val chartHeight = size.height - 60f // Leave space for labels
    val chartLeft = 30f
    val chartTop = 30f

    // Draw axes
    drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(chartLeft, chartTop + chartHeight),
            end = Offset(chartLeft + chartWidth, chartTop + chartHeight),
            strokeWidth = 2f
    )

    drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(chartLeft, chartTop),
            end = Offset(chartLeft, chartTop + chartHeight),
            strokeWidth = 2f
    )

    // Draw chart data
    if (dataPoints.isNotEmpty()) {
        val maxDay = dataPoints.maxOfOrNull { it.first } ?: 31

        // Draw data points first
        dataPoints.forEach { (day, value) ->
            val x = chartLeft + (day.toFloat() / maxDay.toFloat()) * chartWidth
            val y = chartTop + chartHeight - (value * chartHeight)

            drawCircle(color = Color(0xFF1DB954), radius = 6f, center = Offset(x, y))
        }

        // Draw connecting line if we have multiple points
        if (dataPoints.size > 1) {
            val path = Path()

            dataPoints.forEachIndexed { index, (day, value) ->
                val x = chartLeft + (day.toFloat() / maxDay.toFloat()) * chartWidth
                val y = chartTop + chartHeight - (value * chartHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw the line
            drawPath(path = path, color = Color(0xFF1DB954), style = Stroke(width = 3f))
        }
    }
}
