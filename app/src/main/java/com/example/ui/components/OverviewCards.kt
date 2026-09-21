package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.DashboardMetrics
import com.example.util.CurrencyUtils

@Composable
fun OverviewCards(
    metrics: DashboardMetrics,
    modifier: Modifier = Modifier
) {
    val remainingColor = when {
        metrics.remainingMonthlyBudget < 0 -> StatusRed
        metrics.budgetPercentUsed >= 80f -> StatusAmber
        else -> StatusGreen
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Total Spent This Month & Remaining Monthly Budget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Spent This Month",
                amountStr = CurrencyUtils.format(metrics.totalSpentThisMonth, metrics.currencySymbol),
                subtitle = if (metrics.monthlyBudget > 0) "of ${CurrencyUtils.formatCompact(metrics.monthlyBudget, metrics.currencySymbol)} budget" else "No monthly limit set",
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_total_spent")
            )

            MetricCard(
                title = "Remaining Budget",
                amountStr = if (metrics.monthlyBudget > 0) CurrencyUtils.format(metrics.remainingMonthlyBudget.coerceAtLeast(0.0), metrics.currencySymbol) else "—",
                subtitle = if (metrics.monthlyBudget <= 0) "Configure in Settings" else if (metrics.remainingMonthlyBudget < 0) "Over by ${CurrencyUtils.format(-metrics.remainingMonthlyBudget, metrics.currencySymbol)}" else "${(100f - metrics.budgetPercentUsed).coerceAtLeast(0f).toInt()}% left",
                icon = Icons.Default.TrendingUp,
                iconTint = remainingColor,
                iconBg = remainingColor.copy(alpha = 0.12f),
                amountColor = remainingColor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_remaining_budget")
            )
        }

        // Row 2: Today's Spend & Daily Average
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Today's Spend",
                amountStr = CurrencyUtils.format(metrics.todaySpend, metrics.currencySymbol),
                subtitle = "Logged today",
                icon = Icons.Default.CalendarToday,
                iconTint = Color(0xFF0284C7),
                iconBg = Color(0xFFE0F2FE),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_today_spend")
            )

            MetricCard(
                title = "Daily Average",
                amountStr = CurrencyUtils.format(metrics.dailyAverage, metrics.currencySymbol),
                subtitle = "Month to date",
                icon = Icons.Default.ShowChart,
                iconTint = Color(0xFF7C3AED),
                iconBg = Color(0xFFEDE9FE),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_daily_average")
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    amountStr: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    amountColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = amountStr,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = amountColor,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
