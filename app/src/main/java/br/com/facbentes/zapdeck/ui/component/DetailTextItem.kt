package br.com.facbentes.zapdeck.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.facbentes.zapdeck.ui.theme.Slate700
import br.com.facbentes.zapdeck.ui.theme.Slate900
import br.com.facbentes.zapdeck.ui.theme.ZapDeckPrimary

@Composable
fun DetailTextItem(
    icon: ImageVector? = null,
    painter: Painter? = null,
    label: String,
    value: String,
    iconColor: Color = Slate700,
    onClick: (() -> Unit)? = null,
    actionHint: String? = null
) {
    val isClickable = onClick != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick?.invoke() }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isClickable) ZapDeckPrimary else iconColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label, 
                fontWeight = FontWeight.Bold, 
                fontSize = 13.sp, 
                color = Slate900
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = value, 
                    fontSize = 14.5.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    color = if (isClickable) ZapDeckPrimary else Slate900,
                    textDecoration = if (isClickable) TextDecoration.Underline else TextDecoration.None,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isClickable) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Abrir no Google Maps",
                        tint = ZapDeckPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (actionHint != null && isClickable) {
                Text(
                    text = actionHint,
                    fontSize = 11.sp,
                    color = ZapDeckPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

