package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun SignaturePad(
    strokes: SnapshotStateList<List<Offset>>,
    savedImagePath: String?,
    label: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Load old signature bitmap from disk if it exists
    val oldSigBitmap = remember(savedImagePath) {
        if (!savedImagePath.isNullOrEmpty()) {
            val file = File(savedImagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(savedImagePath).asImageBitmap()
            } else null
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Draw,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (strokes.isNotEmpty() || oldSigBitmap != null) {
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Hapus",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus TTD", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            // Draw previous signature if it exists and no new strokes are drawn
            if (oldSigBitmap != null && strokes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = oldSigBitmap,
                        contentDescription = "Tanda tangan tersimpan",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        alpha = 0.85f
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    ) {
                        Text(
                            text = "TTD Tersimpan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Draw current drawing canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                strokes.add(listOf(offset))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (strokes.isNotEmpty()) {
                                    val last = strokes.last()
                                    strokes[strokes.size - 1] = last + change.position
                                }
                            }
                        )
                    }
            ) {
                strokes.forEach { stroke ->
                    if (stroke.size > 1) {
                        val path = Path()
                        path.moveTo(stroke[0].x, stroke[0].y)
                        for (i in 1 until stroke.size) {
                            path.lineTo(stroke[i].x, stroke[i].y)
                        }
                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = Stroke(
                                width = 5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else if (stroke.size == 1) {
                        drawCircle(
                            color = Color.Black,
                            radius = 2.5f,
                            center = stroke[0]
                        )
                    }
                }
            }

            if (strokes.isEmpty() && oldSigBitmap == null) {
                Text(
                    text = "Gambarkan tanda tangan di sini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
