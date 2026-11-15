package ai.roadtrack.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val Bg = Color(0xFF0E1116)
private val Card = Color(0xFF1A1F27)
private val Accent = Color(0xFF4FD1C5)
private val Pin = Color(0xFFF687B3)
private val Dim = Color(0xFF9AA4B2)

data class Course(val name: String, val minutes: Int, val pins: Int, val songs: List<Song>)

// shortcut: hardcoded demo courses, replace with the course/pin server once it exists
private val demoCourses = listOf(
    Course("한강 야경 코스", 52, 14, listOf(Song("밤양갱", "비비"), Song("Hype Boy", "NewJeans"))),
    Course("북악 스카이웨이", 48, 9, listOf(Song("주저하는 연인들을 위해", "잔나비"))),
    Course("인천대교 왕복", 55, 6, listOf(Song("Drive", "Miley Cyrus"))),
)

class MainActivity : ComponentActivity() {
    private lateinit var signals: VehicleSignals

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signals = VehicleSignals(applicationContext).also { it.start() }
        setContent {
            val car by signals.state.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg)) {
                RoadTrackApp(car, onMood = { signals.setAmbient(it.ambientArgb) })
            }
        }
    }

    override fun onDestroy() {
        signals.stop()
        super.onDestroy()
    }
}

@Composable
fun RoadTrackApp(car: CarSignals, onMood: (Mood) -> Unit) {
    var course by remember { mutableStateOf<Course?>(null) }
    var mood by remember { mutableStateOf(Mood.DEFAULT) }
    var dismissed by remember { mutableStateOf<Mood?>(null) }
    val suggested = suggestMood(car)

    Box(Modifier.fillMaxSize().background(Bg).padding(32.dp)) {
        // Editing and browsing only while parked (Pleos driving distraction rules).
        if (car.parked) {
            ParkedScreen(course, onPick = { course = it })
        } else {
            DrivingScreen(course ?: demoCourses.first(), mood)
        }
        if (!car.parked && suggested != Mood.DEFAULT && suggested != mood && suggested != dismissed) {
            SuggestionBar(
                suggested,
                Modifier.align(Alignment.BottomCenter),
                onAccept = { mood = suggested; onMood(suggested) },
                onDismiss = { dismissed = suggested },
            )
        }
    }
}

@Composable
private fun ParkedScreen(selected: Course?, onPick: (Course) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("P · 오늘은 어디로 달릴까요?", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("'금요일 밤' 플레이리스트 52분에 맞춘 코스", color = Dim, fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            demoCourses.forEach { c ->
                val on = c == selected
                Column(
                    Modifier.weight(1f)
                        .background(if (on) Accent.copy(alpha = 0.2f) else Card, RoundedCornerShape(16.dp))
                        .clickable { onPick(c) }
                        .padding(24.dp),
                ) {
                    Text(c.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${c.minutes}분 · 노래 핀 ${c.pins}개", color = Dim, fontSize = 18.sp)
                }
            }
        }
        Text(
            if (selected == null) "코스를 고르면 기어를 D로 바꿔 출발하세요" else "${selected.name} 선택됨 · 기어를 D로 바꾸면 주행 화면으로 바뀌어요",
            color = Accent, fontSize = 20.sp,
        )
    }
}

@Composable
private fun DrivingScreen(course: Course, mood: Mood) {
    val ctx = LocalContext.current
    var index by remember(course) { mutableIntStateOf(0) }
    val song = course.songs[index % course.songs.size]
    // Glanceable: few items, big text, no animation while driving.
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("${course.name} · ${mood.label}", color = Dim, fontSize = 22.sp)
        Text(song.title, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text(song.artist, color = Dim, fontSize = 28.sp)
        Text("다음 노래 핀까지 1.2km", color = Pin, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            BigButton("▶ 이 곡 재생") {
                if (!playSong(ctx, song)) Toast.makeText(ctx, "재생할 음악 앱이 없어요", Toast.LENGTH_SHORT).show()
            }
            BigButton("다음 곡") { index++ }
        }
    }
}

@Composable
private fun BigButton(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.height(96.dp).widthIn(min = 280.dp), shape = RoundedCornerShape(20.dp)) {
        Text(label, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SuggestionBar(mood: Mood, modifier: Modifier, onAccept: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier.fillMaxWidth().background(Card, RoundedCornerShape(20.dp)).padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("${mood.label} 플레이리스트로 바꿀까요?", color = Color.White, fontSize = 28.sp, modifier = Modifier.weight(1f))
        BigButton("바꾸기", onAccept)
        TextButton(onDismiss) { Text("괜찮아요", fontSize = 24.sp, color = Dim) }
    }
}
