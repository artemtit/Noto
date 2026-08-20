package com.noto.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.noto.app.MainActivity
import com.noto.app.NotoApplication
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Task
import java.time.LocalDate

class NotoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as NotoApplication).container
        val today = LocalDate.now().toString()
        val flow = container.taskRepository.observeByDate(today)

        provideContent {
            val tasks by flow.collectAsState(initial = emptyList())
            GlanceTheme {
                WidgetContent(tasks = tasks)
            }
        }
    }

    @Composable
    private fun WidgetContent(tasks: List<Task>) {
        val openAppAction = actionStartActivity<MainActivity>()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .cornerRadius(16.dp)
                .padding(12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().clickable(openAppAction),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = titleText(),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onBackground,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = "${tasks.size}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                )
            }
            Spacer(GlanceModifier.height(6.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(openAppAction),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emptyText(),
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                    items(tasks) { t -> TaskItem(t, openAppAction) }
                }
            }
        }
    }

    @Composable
    private fun TaskItem(task: Task, openAction: Action) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable(openAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(18.dp)
                    .cornerRadius(2.dp)
                    .background(priorityColor(task.priority)),
            ) {}
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = task.title,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onBackground,
                    ),
                )
                task.dueTime?.let {
                    Text(
                        text = it.toString().substring(0, 5),
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }

    private fun titleText(): String =
        if (java.util.Locale.getDefault().language == "ru") "Сегодня" else "Today"

    private fun emptyText(): String =
        if (java.util.Locale.getDefault().language == "ru") "Ничего на сегодня" else "Nothing today"

    private fun priorityColor(p: Priority): androidx.compose.ui.graphics.Color = when (p) {
        Priority.HIGH -> androidx.compose.ui.graphics.Color(0xFFEF4444)
        Priority.MEDIUM -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
        Priority.LOW -> androidx.compose.ui.graphics.Color(0xFF10B981)
    }
}
