package com.example.ui

import android.app.Activity
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ContactEntity
import com.example.utils.CardBeamTransferHelper

@Composable
fun TransmitCardDialog(
    contact: ContactEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val isNfcSupported = nfcAdapter != null
    val isNfcEnabled = nfcAdapter?.isEnabled == true

    var currentMode by remember { mutableStateOf(if (isNfcEnabled) "nfc" else "qr") }

    DisposableEffect(contact, activity) {
        val payload = CardBeamTransferHelper.contactToJson(contact, includeImage = true)
        com.example.utils.ZapDeckCardEmulationService.activeTransferPayload = payload
        
        if (activity != null && nfcAdapter != null) {
            try {
                val cardEmulation = android.nfc.cardemulation.CardEmulation.getInstance(nfcAdapter)
                val componentName = android.content.ComponentName(activity, com.example.utils.ZapDeckCardEmulationService::class.java)
                cardEmulation.setPreferredService(activity, componentName)
            } catch (e: Exception) {
                android.util.Log.e("TransmitCardDialog", "Failed to set preferred HCE service: ${e.message}")
            }
        }

        onDispose {
            com.example.utils.ZapDeckCardEmulationService.activeTransferPayload = null
            if (activity != null && nfcAdapter != null) {
                try {
                    val cardEmulation = android.nfc.cardemulation.CardEmulation.getInstance(nfcAdapter)
                    cardEmulation.unsetPreferredService(activity)
                } catch (e: Exception) {
                    android.util.Log.e("TransmitCardDialog", "Failed to unset preferred HCE service: ${e.message}")
                }
            }
        }
    }

    // Generate standard vCard or ZapDeck payload for QR Code
    val qrBitmap = remember(contact) {
        val vcard = CardBeamTransferHelper.contactToVCard(contact, includeImage = false)
        CardBeamTransferHelper.generateQrBitmap(vcard, 600)
    }

    // Infinite pulsing animation for NFC radar wave effect
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0D3261),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Transmitir Cartão",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Transmissão sem fio instantânea",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.Gray
                        )
                    }
                }

                // Selected Contact Summary Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0056C6),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            if (contact.primaryPhone.isNotBlank()) {
                                Text(
                                    text = contact.primaryPhone,
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                // Mode Selector Tabs (NFC / QR Code)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { currentMode = "nfc" },
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentMode == "nfc") Color(0xFF0D3261) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = if (currentMode == "nfc") Color.White else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aproximação (NFC)",
                                fontSize = 12.sp,
                                fontWeight = if (currentMode == "nfc") FontWeight.Bold else FontWeight.Medium,
                                color = if (currentMode == "nfc") Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { currentMode = "qr" },
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentMode == "qr") Color(0xFF0D3261) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = if (currentMode == "qr") Color.White else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "QR Code Dinâmico",
                                fontSize = 12.sp,
                                fontWeight = if (currentMode == "qr") FontWeight.Bold else FontWeight.Medium,
                                color = if (currentMode == "qr") Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                // Content View
                if (currentMode == "nfc") {
                    // NFC Active Radar UI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background radar pulse circles
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = pulseAlpha * 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale * 0.9f)
                                .clip(CircleShape)
                                .background(Color(0xFF0056C6).copy(alpha = pulseAlpha * 0.25f))
                        )

                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color(0xFF0D3261),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pronto para Transmitir!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0D3261)
                        )
                        Text(
                            text = "Aproxime as costas deste celular da traseira de outro aparelho com NFC ligado para transferir direto para a agenda!",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (!isNfcSupported) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB)
                        ) {
                            Text(
                                text = "Este aparelho não possui hardware NFC. Use a aba QR Code para transferir!",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (!isNfcEnabled) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2)
                        ) {
                            Text(
                                text = "NFC desativado no aparelho. Ative o NFC nas configurações ou use o QR Code.",
                                fontSize = 11.sp,
                                color = Color(0xFFDC2626),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                } else {
                    // QR Code Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, Color(0xFF0D3261)),
                            shadowElevation = 4.dp
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code do Cartão",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(8.dp)
                            )
                        }

                        Text(
                            text = "Aponte a câmera de outro celular (Galaxy, iPhone ou outro Android) ou tire uma foto dentro do ZapDeck para salvar o contato na agenda nativa instantaneamente.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                val vcard = CardBeamTransferHelper.contactToVCard(contact, includeImage = true)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/x-vcard"
                                    putExtra(android.content.Intent.EXTRA_TEXT, vcard)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Cartão de Visita - ${contact.name}")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Enviar Cartão via WhatsApp / Mensagem"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartilhar vCard por WhatsApp / Outros Apps", fontSize = 12.sp)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D3261),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fechar Transmissão",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiveCardImportDialog(
    contact: ContactEntity,
    onDismiss: () -> Unit,
    onImport: (useWhatsAppBusiness: Boolean, followOnInstagram: Boolean) -> Unit
) {
    var useBusiness by remember { mutableStateOf(contact.useWhatsAppBusiness) }
    var followInstagram by remember { mutableStateOf(contact.instagramFollowed) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00D215).copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContactPage,
                            contentDescription = null,
                            tint = Color(0xFF00A310),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Cartão Recebido por Aproximação!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color(0xFF0D3261),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Deseja salvar este contato diretamente na sua agenda telefônica?",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                }

                // Contact preview box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (contact.imageBase64.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                val bitmap = remember(contact.imageBase64) {
                                    try {
                                        val decodedBytes = android.util.Base64.decode(contact.imageBase64, android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = contact.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            if (contact.primaryPhone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00A310), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(contact.primaryPhone, fontSize = 13.sp, color = Color(0xFF334155))
                                }
                            }
                            if (contact.secondaryPhone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(contact.secondaryPhone, fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                            if (contact.instagram.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFE1306C), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("@${contact.instagram.replace("@", "")}", fontSize = 12.sp, color = Color(0xFFE1306C), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (contact.address.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF0056C6), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(contact.address, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 2)
                                }
                            }
                            if (contact.observations.isNotBlank()) {
                                Text(
                                    text = "Serviços/Obs: ${contact.observations}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ignorar")
                    }

                    Button(
                        onClick = { onImport(useBusiness, followInstagram) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A310))
                    ) {
                        Text("Salvar na Agenda", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
