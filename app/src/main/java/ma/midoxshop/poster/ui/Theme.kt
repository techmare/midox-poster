package ma.midoxshop.poster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox

/** Warm off-white, near-black, one orange for the one thing to do now; teal = done / ready. */
object C {
    val ground = Color(0xFFF4F2EC)
    val card = Color(0xFFFFFFFF)
    val ink = Color(0xFF15171C)
    val muted = Color(0xFF5B5F68)
    val line = Color(0xFFE7E4DC)
    val photo = Color(0xFFE7E4DC)
    val accent = Color(0xFFEA580C)
    val accentText = Color(0xFFC2410C)
    val accentSoft = Color(0xFFFFEDD5)
    val accentInk = Color(0xFF9A3412)
    val glow = Color(0xFFFB923C)
    val good = Color(0xFF0F766E)
    val goodSoft = Color(0xFFCCFBF1)
    val dark = Color(0xFF15171C)
    val dark2 = Color(0xFF22252B)
    val dark3 = Color(0xFF2C2F36)
    val onDark = Color(0xFFC9CBD1)
}

@Composable
fun PosterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = C.accent, onPrimary = Color.White, background = C.ground, surface = C.card, onSurface = C.ink),
        content = content
    )
}

@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, color: Color = C.accent, textColor: Color = Color.White, height: Dp = 64.dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Text(text, color = textColor, fontSize = if (height >= 60.dp) 19.sp else 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    BigButton(text, modifier, color = C.card, textColor = C.ink, height = 48.dp, onClick = onClick)

@Composable
fun Pill(text: String, fg: Color = C.good, bg: Color = C.goodSoft) {
    Text(
        text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun Thumb(item: Item?, size: Dp = 64.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(C.photo), contentAlignment = Alignment.Center) {
        val pic = item?.pics?.firstOrNull()
        if (pic != null) AsyncImage(Midox.photo(pic), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        else Text("No photo", fontSize = 11.sp, color = C.muted, textAlign = TextAlign.Center)
    }
}

/** One product in a list: photo, name, a line under it, and something on the right. */
@Composable
fun ItemRow(item: Item, sub: String, subColor: Color = C.muted, onClick: (() -> Unit)? = null, right: @Composable RowScope.() -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(C.card)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 72.dp)
            .padding(12.dp)
    ) {
        Thumb(item, 56.dp)
        Column(Modifier.weight(1f)) {
            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 13.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        right()
    }
}

@Composable
fun Section(text: String) {
    Text(
        text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.muted, letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(52.dp, 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) C.accent else C.line)
            .clickable(role = Role.Switch) { onChange(!on) }
            .padding(4.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) { Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White)) }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("−" to -1, "+" to 1).forEachIndexed { i, (label, d) ->
            if (i == 1) Text("$value", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = C.ink, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(C.ground).clickable(role = Role.Button) { onChange(value + d) },
                contentAlignment = Alignment.Center
            ) { Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.ink) }
        }
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else C.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) C.ink else C.card)
            .border(1.dp, if (on) C.ink else C.line, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
