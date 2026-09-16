package br.com.facbentes.zapdeck.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.facbentes.zapdeck.R

data class IconOption(
    val id: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val colorsDesc: String,
    val drawableResId: Int
)

val ICON_OPTIONS = listOf(
    IconOption(
        id = 1,
        title = "Opção 1: Card Pulse",
        subtitle = "Escaneamento Ativo & Feixe de Luz",
        description = "Cartão de visita flutuante estilizado com feixe de luz neon de escaneamento em tempo real. Design limpo, sem bordas ou molduras externas.",
        colorsDesc = "Degradê suave do Azul Royal Profundo (#0D47A1) ao Azul Elétrico e Ciano (#00B0FF)",
        drawableResId = R.drawable.icon_option_1
    ),
    IconOption(
        id = 2,
        title = "Opção 2: Geometric Flow",
        subtitle = "Deck Dinâmico & Conectividade",
        description = "Monograma de cartões em camadas sobrepostas com dinâmica de raio/zap minimalista em vidro fosco. Estética premium inspirada em fintechs.",
        colorsDesc = "Degradê fluido de Azul Safira, Azul Cobalto e reflexos turquesa",
        drawableResId = R.drawable.icon_option_2
    ),
    IconOption(
        id = 3,
        title = "Opção 3: Smart Card Lens",
        subtitle = "Visor Executivo & Alta Fidelidade",
        description = "Cartão inteligente central com marcadores de enquadramento/foco e símbolo de compartilhamento rápido em fundo degradê contínuo.",
        colorsDesc = "Degradê homogêneo do Azul Marinho Escuro ao Azul Cerúleo Vibrante (#0072FF -> #00C6FF)",
        drawableResId = R.drawable.icon_option_3
    )
)

@Composable
fun IconSelectionDialog(
    currentSelectedId: Int = 1,
    onDismiss: () -> Unit,
    onSelectOption: (IconOption) -> Unit
) {
    var selectedId by remember { mutableStateOf(currentSelectedId) }
    var showCreatorPhotoExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val linkedinUrl = "https://www.linkedin.com/in/fabio-bentes/"
    val openLinkedIn: () -> Unit = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedinUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header com degradê azul
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0D47A1), Color(0xFF0288D1))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎨 Novos Ícones ZapDeck",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Sem bordas, degradê azul fluido e acabamento premium",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showCreatorPhotoExpanded = true }
                                    .background(Color.White.copy(alpha = 0.22f), CircleShape)
                                    .border(0.8.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                    .padding(start = 5.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_creator_fabio),
                                    contentDescription = "Fábio A. C. Bentes - Criador (toque para ampliar)",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color.White, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Criado por Fábio A. C. Bentes",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { openLinkedIn() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_linkedin_custom),
                                        contentDescription = "LinkedIn de Fábio A. C. Bentes",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Lista rolável com as 3 opções
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ICON_OPTIONS.forEach { option ->
                        val isSelected = selectedId == option.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF0288D1) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedId = option.id },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF0F9FF) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Badge e Título
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = option.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSelected) Color(0xFF0369A1) else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = option.subtitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    if (isSelected) {
                                        Surface(
                                            color = Color(0xFF0288D1),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "SELECIONADO",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Visualização em alta resolução simulando o ícone do Android (Squircle)
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .shadow(12.dp, RoundedCornerShape(36.dp))
                                        .clip(RoundedCornerShape(36.dp))
                                        .background(Color(0xFF0D47A1))
                                ) {
                                    Image(
                                        painter = painterResource(id = option.drawableResId),
                                        contentDescription = option.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Detalhes do design
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🎨 ${option.colorsDesc}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Botão de Ação Direta
                                Button(
                                    onClick = {
                                        selectedId = option.id
                                        onSelectOption(option)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF0288D1) else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSelected) "Confirmar ${option.title}" else "Escolher Esta Opção",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Barra inferior de confirmação rápida
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Fechar")
                        }

                        Button(
                            onClick = {
                                val opt = ICON_OPTIONS.firstOrNull { it.id == selectedId } ?: ICON_OPTIONS.first()
                                onSelectOption(opt)
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Bater o Martelo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Foto Ampliada do Criador
    if (showCreatorPhotoExpanded) {
        Dialog(
            onDismissRequest = { showCreatorPhotoExpanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable { showCreatorPhotoExpanded = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clickable(enabled = false) { /* Impede fechamento ao clicar no conteúdo do card */ },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { showCreatorPhotoExpanded = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar visualização",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Foto do Criador Ampliada
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .shadow(12.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .border(2.5.dp, Color(0xFF0288D1), RoundedCornerShape(22.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_creator_fabio),
                                contentDescription = "Foto ampliada de Fábio A. C. Bentes",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Fábio A. C. Bentes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { openLinkedIn() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF0A66C2).copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_linkedin_custom),
                                    contentDescription = "Abrir LinkedIn de Fábio A. C. Bentes",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Criador e Desenvolvedor do ZapDeck",
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            onClick = { openLinkedIn() },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0F7FF),
                            border = BorderStroke(1.dp, Color(0xFF0A66C2).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_linkedin_custom),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "linkedin.com/in/fabio-bentes",
                                    color = Color(0xFF0A66C2),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showCreatorPhotoExpanded = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                        ) {
                            Text("Fechar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
