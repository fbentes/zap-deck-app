package br.com.facbentes.zapdeck.ui.component

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.facbentes.zapdeck.utils.MapsHelper
import androidx.compose.ui.window.Dialog
import br.com.facbentes.zapdeck.R
import br.com.facbentes.zapdeck.data.ContactEntity
import br.com.facbentes.zapdeck.integration.messaging.MessagingIntegrationHelper
import br.com.facbentes.zapdeck.ui.TransmitCardDialog
import br.com.facbentes.zapdeck.ui.theme.Slate200
import br.com.facbentes.zapdeck.ui.theme.Slate700
import br.com.facbentes.zapdeck.ui.theme.Slate800
import br.com.facbentes.zapdeck.ui.theme.Slate900
import br.com.facbentes.zapdeck.ui.theme.ZapDeckPrimary

@Composable
fun ContactDetailsDialog(
    contact: ContactEntity,
    userName: String,
    getGreeting: () -> String,
    getTemplateMsg: () -> String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateContact: (ContactEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImageZoomed by remember { mutableStateOf(false) }

    var isWhatsAppRegisteredOnNormal by remember { mutableStateOf<Boolean?>(null) }
    var isWhatsAppRegisteredOnBusiness by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingWhatsApp by remember { mutableStateOf(false) }

    var isCheckingInstagramOnLoad by remember { mutableStateOf(false) }
    var instagramExists by remember { mutableStateOf(true) }
    var showTransmitDialog by remember { mutableStateOf(false) }

    val isNormalInstalled = remember(context) { MessagingIntegrationHelper.isAppInstalled(context, "com.whatsapp") }
    val isBusinessInstalled = remember(context) { MessagingIntegrationHelper.isAppInstalled(context, "com.whatsapp.w4b") }
    var viewingBackInDetails by remember { mutableStateOf(false) }

    LaunchedEffect(contact.primaryPhone) {
        if (contact.primaryPhone.isNotEmpty()) {
            isCheckingWhatsApp = true
            isWhatsAppRegisteredOnNormal = MessagingIntegrationHelper.checkIfWhatsAppRegistered(context, contact.primaryPhone, isBusiness = false)
            isWhatsAppRegisteredOnBusiness = MessagingIntegrationHelper.checkIfWhatsAppRegistered(context, contact.primaryPhone, isBusiness = true)
            isCheckingWhatsApp = false
        }
    }

    LaunchedEffect(contact.instagram) {
        if (contact.instagram.isNotEmpty()) {
            isCheckingInstagramOnLoad = true
            instagramExists = MessagingIntegrationHelper.checkIfInstagramUserExists(contact.instagram)
            isCheckingInstagramOnLoad = false
        }
    }

    if (showTransmitDialog) {
        TransmitCardDialog(
            contact = contact,
            onDismiss = { showTransmitDialog = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dados do Contato",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showTransmitDialog = true }, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Transmitir por NFC / QR Code",
                                tint = ZapDeckPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = "Editar", 
                                tint = Slate700, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Excluir", 
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar e sair da tela",
                                tint = Slate800,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                if (contact.imageBase64.isNotEmpty() || contact.backImageBase64.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Foto do Cartão (Toque para dar zoom):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        if (contact.imageBase64.isNotEmpty() && contact.backImageBase64.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = !viewingBackInDetails,
                                    onClick = { viewingBackInDetails = false },
                                    label = { Text("Frente", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = viewingBackInDetails,
                                    onClick = { viewingBackInDetails = true },
                                    label = { Text("Verso", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    val currentDisplayBase64 = if (viewingBackInDetails && contact.backImageBase64.isNotEmpty()) {
                        contact.backImageBase64
                    } else {
                        contact.imageBase64
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clickable { isImageZoomed = true },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(1.dp),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Base64Image(base64String = currentDisplayBase64, modifier = Modifier.fillMaxSize())
                    }
                }

                DetailTextItem(icon = Icons.Default.Person, label = "Nome:", value = contact.name)
                
                if (contact.primaryPhone.isNotEmpty()) {
                    DetailTextItem(
                        painter = painterResource(id = R.drawable.ic_whatsapp_custom),
                        label = "Telefone Principal (WhatsApp):",
                        value = contact.primaryPhone
                    )
                }

                if (contact.secondaryPhone.isNotEmpty()) {
                    DetailTextItem(
                        painter = painterResource(id = R.drawable.ic_whatsapp_custom),
                        label = "Telefone Secundário (WhatsApp):",
                        value = contact.secondaryPhone
                    )
                }

                if (contact.landlinePhone.isNotEmpty()) {
                    DetailTextItem(
                        icon = Icons.Default.Phone,
                        label = "Telefone Comum / Fixo:",
                        value = contact.landlinePhone
                    )
                }

                if (contact.email.isNotEmpty()) {
                    DetailTextItem(
                        icon = Icons.Default.Email,
                        label = "E-mail:",
                        value = contact.email
                    )
                }

                if (contact.address.isNotEmpty()) {
                    DetailTextItem(
                        icon = Icons.Default.Place, 
                        label = "Endereço:", 
                        value = contact.address,
                        onClick = {
                            MapsHelper.openAddressInMaps(context, contact.address)
                        },
                        actionHint = "Toque para abrir no Google Maps"
                    )
                }

                if (contact.instagram.isNotEmpty()) {
                    DetailTextItem(
                        icon = Icons.Default.Person, 
                        label = "Instagram:", 
                        value = contact.instagram,
                        iconColor = Color(0xFFE1306C)
                    )
                }

                if (contact.observations.isNotEmpty()) {
                    DetailTextItem(icon = Icons.Default.Info, label = "Serviços / Observações:", value = contact.observations)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Slate200)

                Text(
                    text = "Ações Rápidas de Integração",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (contact.primaryPhone.isNotEmpty()) {
                        if (isNormalInstalled) {
                            Button(
                                onClick = {
                                    val textMsg = getTemplateMsg()
                                    MessagingIntegrationHelper.openWhatsAppChat(context, contact.primaryPhone, textMsg, false)
                                },
                                modifier = Modifier.size(54.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                enabled = !isCheckingWhatsApp
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp_custom),
                                    contentDescription = "WhatsApp Padrão",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        if (isBusinessInstalled) {
                            Button(
                                onClick = {
                                    val textMsg = getTemplateMsg()
                                    MessagingIntegrationHelper.openWhatsAppChat(context, contact.primaryPhone, textMsg, true)
                                },
                                modifier = Modifier.size(54.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                enabled = !isCheckingWhatsApp
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp_business_custom),
                                    contentDescription = "WhatsApp Business",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        if (!isNormalInstalled && !isBusinessInstalled) {
                            Button(
                                onClick = {
                                    val textMsg = getTemplateMsg()
                                    MessagingIntegrationHelper.openWhatsAppChat(context, contact.primaryPhone, textMsg, contact.useWhatsAppBusiness)
                                },
                                modifier = Modifier.size(54.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp_custom),
                                    contentDescription = "WhatsApp",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                    }

                    if (contact.email.isNotEmpty()) {
                        Button(
                            onClick = {
                                try {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${contact.email}")
                                    }
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Nenhum aplicativo de e-mail encontrado.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ZapDeckPrimary),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Enviar E-mail",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (contact.instagram.isNotEmpty()) {
                        val isInstagramButtonEnabled = !isCheckingInstagramOnLoad && instagramExists
                        Button(
                            onClick = {
                                MessagingIntegrationHelper.openInstagramProfile(context, contact.instagram)
                                if (!contact.instagramFollowed) {
                                    onUpdateContact(contact.copy(instagramFollowed = true))
                                }
                            },
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (instagramExists) Color(0xFFE1306C) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (instagramExists) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            enabled = isInstagramButtonEnabled
                        ) {
                            Icon(
                                imageVector = if (!instagramExists) Icons.Default.Block else Icons.Default.Person,
                                contentDescription = "Instagram",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (isImageZoomed) {
        val currentZoomBase64 = if (viewingBackInDetails && contact.backImageBase64.isNotEmpty()) {
            contact.backImageBase64
        } else {
            contact.imageBase64
        }
        Dialog(onDismissRequest = { isImageZoomed = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clickable { isImageZoomed = false }
            ) {
                Base64Image(
                    base64String = currentZoomBase64,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
