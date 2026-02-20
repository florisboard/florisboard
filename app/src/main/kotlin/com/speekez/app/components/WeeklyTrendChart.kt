package com.speekez.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.data.entity.DailyStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeeklyTrendChart(
    weeklyStats: List<DailyStats>,
    monday: LocalDate,
    modifier: Modifier = Modifier
) {
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    val maxWords = (weeklyStats.maxOfOrNull { it.wordCount } ?: 0).coerceAtLeast(1)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0..6) {
                val date = monday.plusDays(i.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                val stats = weeklyStats.find { it.date == date }
                val wordCount = stats?.wordCount ?: 0
                val barHeightFraction = wordCount.toFloat() / maxWords

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedDayIndex = if (selectedDayIndex == i) null else i
                        },
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Tooltip
                    AnimatedVisibility(
                        visible = selectedDayIndex == i,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF1A1A2E),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = wordCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(barHeightFraction.coerceAtLeast(0.05f))
                                .width(24.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF00D4AA),
                                            Color(0xFF00D4AA).copy(alpha = 0.3f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..6) {
                Text(
                    text = monday.plusDays(i.toLong()).dayOfWeek.name.take(1),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
