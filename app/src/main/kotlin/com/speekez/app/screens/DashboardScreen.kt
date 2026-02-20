package com.speekez.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.app.components.EmptyState
import com.speekez.app.components.WeeklyTrendChart
import com.speekez.data.dailyStatsDao
import com.speekez.data.transcriptionDao
import com.speekez.data.entity.DailyStats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.math.roundToInt

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val dailyStatsDao = remember { context.dailyStatsDao() }
    val transcriptionDao = remember { context.transcriptionDao() }

    val allStatsFlow = remember { dailyStatsDao.getAllStats() }
    val allStats by allStatsFlow.collectAsState(initial = null)

    val overallAvgWpmFlow = remember { transcriptionDao.getOverallAvgWpm() }
    val overallAvgWpm by overallAvgWpmFlow.collectAsState(initial = 0f)

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
        EmptyState(
            icon = Icons.Default.BarChart,
            message = "Start dictating to see your stats",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                HeroTimeSaved(totalWordsAllTime ?: 0)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val statsList = allStats!!
                    StatCard(
                        label = "Avg WPM",
                        value = "%.1f".format(Locale.getDefault(), overallAvgWpm ?: 0f),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        label = "Total Words",
                        value = (totalWordsAllTime ?: 0).toString(),
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
fun HeroTimeSaved(wordCount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = calculateTimeSaved(wordCount),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Total Time Saved",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun TodayStatsCard(stats: DailyStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Stats",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(label = "Recordings", value = (stats?.recordingCount ?: 0).toString())
                StatColumn(label = "Words", value = (stats?.wordCount ?: 0).toString())
                StatColumn(label = "Time Saved", value = calculateTimeSaved(stats?.wordCount ?: 0))
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun WeeklyChartCard(weeklyStats: List<DailyStats>, monday: LocalDate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            WeeklyTrendChart(
                weeklyStats = weeklyStats,
                monday = monday,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun calculateTimeSaved(wordCount: Int): String {
    val totalMinutes = wordCount / 75.0
    val totalMinutesRounded = totalMinutes.roundToInt()
    val hours = totalMinutesRounded / 60
    val minutes = totalMinutesRounded % 60

    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
