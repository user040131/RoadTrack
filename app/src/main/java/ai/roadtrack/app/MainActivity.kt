package ai.roadtrack.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Design system "RoadTrack" v3: dark glass. Muted accent, no pure primaries.
private val Bg = Color(0xFF0B0C10)
private val Accent = Color(0xFF7FC8A0)
private val OnAccent = Color(0xFF0B1A12)
private val Dim = Color(0xFFA8A8AE)
private val Haze = Color(0xFF3A4660)

data class Course(val name: String, val minutes: Int, val pins: Int, val songs: List<Song>)

// shortcut: hardcoded demo courses, replace with the course/pin server once it exists
private val demoCourses = listOf(
    Course("한강 야경 코스", 52, 14, listOf(Song("밤양갱", "비비", 0xFFD9B27A.toInt()), Song("Hype Boy", "NewJeans", 0xFF8DB8E8.toInt()))),
    Course("북악 스카이웨이", 48, 9, listOf(Song("주저하는 연인들을 위해", "잔나비", 0xFFD99494.toInt()), Song("Drive", "Miley Cyrus"))),
    Course("인천대교 왕복", 55, 6, listOf(Song("Drive", "Miley Cyrus"), Song("밤양갱", "비비", 0xFFD9B27A.toInt()))),
)

class MainActivity : ComponentActivity() {
    private lateinit var signals: VehicleSignals

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signals = VehicleSignals(applicationContext).also { it.start() }
        setContent {
            val car by signals.state.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, onPrimary = OnAccent, background = Bg)) {
                RoadTrackApp(car, onLight = signals::setAmbient)
            }
        }
    }

    override fun onDestroy() {
        signals.stop()
        super.onDestroy()
    }
}

/** Frosted glass: a faint top-lit fill and a hairline rim that is brighter at the top, like light catching an edge. */
private fun Modifier.glass(shape: Shape, tint: Color = Color.White, base: Color = Color.Transparent, rim: Color = Color.White) = this
    .clip(shape)
    .background(base)
    .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.06f))))
    .border(1.dp, Brush.verticalGradient(listOf(rim.copy(alpha = 0.36f), rim.copy(alpha = 0.06f))), shape)

/** Soft light behind the glass, tinted by the current cabin color so screen and cabin match. Static: no animation while driving. */
@Composable
private fun AmbientBackground(light: Color) {
    Box(
        Modifier.fillMaxSize()
            .background(Bg)
            .background(Brush.radialGradient(listOf(light.copy(alpha = 0.30f), Color.Transparent), center = Offset(300f, 120f), radius = 1100f))
            .background(Brush.radialGradient(listOf(Haze.copy(alpha = 0.45f), Color.Transparent), center = Offset(1700f, 760f), radius = 1000f)),
    )
}

@Composable
fun RoadTrackApp(car: CarSignals, onLight: (Int) -> Unit) {
    var course by remember { mutableStateOf<Course?>(null) }
    var mood by remember { mutableStateOf(Mood.DEFAULT) }
    var dismissed by remember { mutableStateOf<Mood?>(null) }
    var light by remember { mutableIntStateOf(Mood.DEFAULT.ambientArgb) }
    val suggested = suggestMood(car)

    Box(Modifier.fillMaxSize()) {
        AmbientBackground(Color(light))
        Box(Modifier.fillMaxSize().padding(40.dp)) {
            // Editing and browsing only while parked (Pleos driving distraction rules).
            if (car.parked) {
                ParkedScreen(course, onPick = { course = it })
            } else {
                DrivingScreen(course ?: demoCourses.first(), mood) { light = it; onLight(it) }
            }
            if (!car.parked && suggested != Mood.DEFAULT && suggested != mood && suggested != dismissed) {
                SuggestionBar(
                    suggested,
                    Modifier.align(Alignment.BottomCenter),
                    onAccept = { mood = suggested },
                    onDismiss = { dismissed = suggested },
                )
            }
        }
    }
}

@Composable
private fun ParkedScreen(selected: Course?, onPick: (Course) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("P · 오늘은 어디로 달릴까요?", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
        Text("'금요일 밤' 플레이리스트 52분에 맞춘 코스", color = Dim, fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            demoCourses.forEach { c ->
                val on = c == selected
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.weight(1f)
                        .glass(shape, tint = if (on) Accent else Color.White, rim = if (on) Accent else Color.White)
                        .clickable(role = Role.Button) { onPick(c) }
                        .padding(24.dp),
                ) {
                    Text(c.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text("${c.minutes}분 · 노래 핀 ${c.pins}개", color = Dim, fontSize = 20.sp)
                }
            }
        }
        Text(
            if (selected == null) "코스를 고르면 기어를 D로 바꿔 출발하세요" else "${selected.name} 선택됨 · 기어를 D로 바꾸면 주행 화면으로 바뀌어요",
            color = if (selected == null) Dim else Accent, fontSize = 20.sp,
        )
    }
}

@Composable
private fun DrivingScreen(course: Course, mood: Mood, onLight: (Int) -> Unit) {
    val ctx = LocalContext.current
    var index by remember(course) { mutableIntStateOf(0) }
    val song = course.songs[index % course.songs.size]
    val light = lightFor(song, mood)
    // Recolor the cabin once whenever the song or the mood changes.
    LaunchedEffect(light) { onLight(light) }
    // Glanceable: few items, big text, no animation while driving.
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(
            Modifier.glass(RoundedCornerShape(32.dp)).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${course.name} · ${mood.label}", color = Dim, fontSize = 22.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.size(20.dp).background(Color(light), CircleShape))
                Text(song.title, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
            }
            Text(song.artist, color = Dim, fontSize = 26.sp)
            Text("다음 노래 핀까지 1.2km", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            BigButton("이 곡 재생") {
                if (!playSong(ctx, song)) Toast.makeText(ctx, "재생할 음악 앱이 없어요", Toast.LENGTH_SHORT).show()
            }
            BigButton("다음 곡", primary = false) { index++ }
        }
    }
}

/** Primary: soft accent with a top highlight. Secondary: plain glass. */
@Composable
private fun BigButton(label: String, primary: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val surface = if (primary) {
        Modifier.clip(shape)
            .background(Accent)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
            .border(1.dp, Color.White.copy(alpha = 0.30f), shape)
    } else {
        Modifier.glass(shape)
    }
    Box(
        Modifier.height(96.dp).widthIn(min = 240.dp).then(surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (primary) OnAccent else Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SuggestionBar(mood: Mood, modifier: Modifier, onAccept: () -> Unit, onDismiss: () -> Unit) {
    Row(
        // Darker base than other glass: it floats over driving content and must stay readable.
        modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp), base = Bg.copy(alpha = 0.82f)).padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(Modifier.size(20.dp).background(Color(mood.ambientArgb), CircleShape))
        Text("${mood.label} 플레이리스트로 바꿀까요?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        TextButton(onDismiss) { Text("괜찮아요", fontSize = 26.sp, color = Dim) }
        BigButton("바꾸기", onClick = onAccept)
    }
}
