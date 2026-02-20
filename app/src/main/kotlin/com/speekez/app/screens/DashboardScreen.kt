package com.speekez.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.data.dailyStatsDao
import com.speekez.data.entity.DailyStats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val dailyStatsDao = remember { context.dailyStatsDao() }

    val allStatsFlow = remember { dailyStatsDao.getAllStats() }
    val allStats by allStatsFlow.collectAsState(initial = null)

    val totalWordsFlow = remember { dailyStatsDao.getTotalWordCount() }
    val totalWordsAllTime by totalWordsFlow.collectAsState(initial = 0)

    val todayDate = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val todayStats = allStats?.find { it.date == todayDate }

    val monday = remember { LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val sunday = remember { monday.plusDays(6) }

    val weeklyStatsFlow = remember(monday, sunday) {
        dailyStatsDao.getWeeklyStats(
            monday.format(DateTimeFormatter.ISO_LOCAL_DATE),
            sunday.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
    val weeklyStats by weeklyStatsFlow.collectAsState(initial = emptyList())

    if (allStats == null) {
        // Still loading from database
        Box(modifier = Modifier.fillMaxSize())
    } else if (allStats!!.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No data yet", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                HeroWordCounter(totalWordsAllTime ?: 0)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val statsList = allStats!!
                    val avgWpm = if (statsList.isNotEmpty()) statsList.map { it.avgWpm }.average().toFloat() else 0f
                    StatCard(
                        label = "Avg WPM",
                        value = "%.1f".format(Locale.getDefault(), avgWpm),
                        modifier = Modifier.weight(1f)
                    )

                    val totalSeconds = statsList.sumOf { it.timeSavedSeconds }
                    StatCard(
                        label = "Time Saved",
                        value = formatTimeSaved(totalSeconds),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                TodayStatsCard(todayStats)
            }

            item {
                WeeklyChartCard(weeklyStats, monday)
            }
        }
    }
}

@Composable
fun HeroWordCounter(count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4AA)
        )
        Text(
            text = "Words Transcribed",
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TodayStatsCard(stats: DailyStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Stats",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(label = "Recordings", value = (stats?.recordingCount ?: 0).toString())
                StatColumn(label = "Words", value = (stats?.wordCount ?: 0).toString())
                StatColumn(label = "Time Saved", value = formatTimeSaved(stats?.timeSavedSeconds ?: 0))
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun WeeklyChartCard(weeklyStats: List<DailyStats>, monday: LocalDate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val maxWords = (weeklyStats.maxOfOrNull { it.wordCount } ?: 0).coerceAtLeast(1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(barHeightFraction.coerceAtLeast(0.05f))
                                .width(20.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF00D4AA), Color(0xFF00D4AA).copy(alpha = 0.3f))
                                    ),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = monday.plusDays(i.toLong()).dayOfWeek.name.take(1),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

fun formatTimeSaved(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
