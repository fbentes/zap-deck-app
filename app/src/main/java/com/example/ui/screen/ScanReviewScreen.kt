package com.example.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel
import com.example.R
import com.example.data.ContactEntity
import com.example.integration.messaging.MessagingIntegrationHelper
import com.example.ui.ProcessingCardAnimation
import com.example.ui.TransmitCardDialog
import com.example.ui.component.Base64Image
import com.example.ui.component.zapDeckTextFieldColors
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.ZapDeckPrimary
import kotlinx.coroutines.launch

@Composable
fun ScanReviewLayout(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val contactsList by viewModel.contacts.collectAsStateWithLifecycle()

    val cleanParsedPhone = remember(viewModel.parsedPrimaryPhone) {
        viewModel.parsedPrimaryPhone.replace("\\D".toRegex(), "")
    }
    val existingWhatsAppContact = remember(contactsList, cleanParsedPhone) {
        if (cleanParsedPhone.isEmpty()) null else {
            contactsList.find { lc ->
                val cleanLcPrimary = lc.primaryPhone.replace("\\D".toRegex(), "")
                val cleanLcSecondary = lc.secondaryPhone.replace("\\D".toRegex(), "")
                (cleanLcPrimary.isNotEmpty() && (cleanLcPrimary == cleanParsedPhone || cleanLcPrimary.endsWith(cleanParsedPhone) || cleanParsedPhone.endsWith(cleanLcPrimary))) ||
                (cleanLcSecondary.isNotEmpty() && (cleanLcSecondary == cleanParsedPhone || cleanLcSecondary.endsWith(cleanParsedPhone) || cleanParsedPhone.endsWith(cleanLcSecondary)))
            }
        }
    }

    val cleanParsedInstagram = remember(viewModel.parsedInstagram) {
        viewModel.parsedInstagram.replace("@", "").trim().lowercase()
    }
    val existingInstagramContact = remember(contactsList, cleanParsedInstagram) {
        if (cleanParsedInstagram.isEmpty()) null else {
            contactsList.find { lc ->
                val cleanLcInstagram = lc.instagram.replace("@", "").trim().lowercase()
                cleanLcInstagram == cleanParsedInstagram
            }
        }
    }

    val isWhatsAppInstalled = remember(context) { MessagingIntegrationHelper.isAppInstalled(context, "com.whatsapp") }
    val isWhatsAppBusinessInstalled = remember(context) { MessagingIntegrationHelper.isAppInstalled(context, "com.whatsapp.w4b") }

    val showNormal = isWhatsAppInstalled || (!isWhatsAppInstalled && !isWhatsAppBusinessInstalled)
    val showBusiness = isWhatsAppBusinessInstalled

    var sendViaNormal by remember { mutableStateOf(false) }
    var sendViaBusiness by remember { mutableStateOf(false) }
    var followOnInstagram by remember { mutableStateOf(false) }
    var customMessage by remember { mutableStateOf("") }

    LaunchedEffect(existingWhatsAppContact) {
        if (existingWhatsAppContact != null) {
            sendViaNormal = false
            sendViaBusiness = false
        }
    }

    LaunchedEffect(existingInstagramContact) {
        if (existingInstagramContact != null) {
            followOnInstagram = false
        }
    }

    var isCheckingWhatsAppNormal by remember { mutableStateOf(false) }
    var isCheckingWhatsAppBusiness by remember { mutableStateOf(false) }
    var whatsAppNormalExists by remember { mutableStateOf(false) }
    var whatsAppBusinessExists by remember { mutableStateOf(false) }
    var whatsAppChecked by remember { mutableStateOf(false) }

    var isCheckingInstagram by remember { mutableStateOf(false) }
    var instagramExists by remember { mutableStateOf(false) }
    var instagramChecked by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.parsedPrimaryPhone) {
        val phone = viewModel.parsedPrimaryPhone
        if (phone.isNotBlank()) {
            isCheckingWhatsAppNormal = true
            whatsAppNormalExists = MessagingIntegrationHelper.checkIfWhatsAppRegistered(context, phone, isBusiness = false)
            isCheckingWhatsAppNormal = false
            
            isCheckingWhatsAppBusiness = true
            whatsAppBusinessExists = MessagingIntegrationHelper.checkIfWhatsAppRegistered(context, phone, isBusiness = true)
            isCheckingWhatsAppBusiness = false
            
            whatsAppChecked = true
            
            sendViaNormal = false
            sendViaBusiness = false
        } else {
            whatsAppNormalExists = false
            whatsAppBusinessExists = false
            whatsAppChecked = false
            sendViaNormal = false
            sendViaBusiness = false
        }
    }

    LaunchedEffect(whatsAppNormalExists, whatsAppBusinessExists, existingWhatsAppContact) {
        if (existingWhatsAppContact != null) {
            sendViaNormal = false
            sendViaBusiness = false
        } else {
            if (!whatsAppNormalExists) {
                sendViaNormal = false
            }
            if (!whatsAppBusinessExists) {
                sendViaBusiness = false
            }
        }
    }

    LaunchedEffect(viewModel.parsedInstagram) {
        val handle = viewModel.parsedInstagram
        if (handle.isNotBlank()) {
            isCheckingInstagram = true
            instagramExists = MessagingIntegrationHelper.checkIfInstagramUserExists(handle)
            isCheckingInstagram = false
            instagramChecked = true
            
            followOnInstagram = false
            if (!instagramExists) {
                Toast.makeText(context, "O contato $handle do Instagram não existe!", Toast.LENGTH_LONG).show()
            }
        } else {
            instagramExists = false
            instagramChecked = false
            followOnInstagram = false
        }
    }

    val defaultMsg = viewModel.getWhatsAppMessage()
    LaunchedEffect(viewModel.parsedName, viewModel.userName) {
        if (customMessage.isEmpty() || customMessage.contains("()  .") || customMessage.contains("o(a)  .")) {
            customMessage = defaultMsg
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Slate800)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancelar e Voltar", color = Slate800, fontWeight = FontWeight.Bold)
            }

            if (viewModel.backImageBase64 != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = viewModel.viewingFrontCard,
                        onClick = { viewModel.viewingFrontCard = true },
                        label = { Text("Frente do Cartão", fontWeight = if (viewModel.viewingFrontCard) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZapDeckPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    FilterChip(
                        selected = !viewModel.viewingFrontCard,
                        onClick = { viewModel.viewingFrontCard = false },
                        label = { Text("Verso do Cartão", fontWeight = if (!viewModel.viewingFrontCard) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZapDeckPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val imageToDisplay = if (viewModel.viewingFrontCard) {
                        if (viewModel.showOriginalPhoto && viewModel.originalUncroppedBase64 != null) {
                            viewModel.originalUncroppedBase64
                        } else {
                            viewModel.capturedImageBase64
                        }
                    } else {
                        if (viewModel.showOriginalPhoto && viewModel.backOriginalUncroppedBase64 != null) {
                            viewModel.backOriginalUncroppedBase64
                        } else {
                            viewModel.backImageBase64
                        }
                    }
                    imageToDisplay?.let { base64 ->
                        Base64Image(
                            base64String = base64,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Subtitle badge & original toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ZapDeckPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = ZapDeckPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (viewModel.showOriginalPhoto) "Exibindo foto original (com fundo)" else "Cartão enquadrado e sem sombras",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZapDeckPrimary
                        )
                    }
                }

                val hasOriginal = if (viewModel.viewingFrontCard) {
                    viewModel.originalUncroppedBase64 != null
                } else {
                    viewModel.backOriginalUncroppedBase64 != null
                }
                if (hasOriginal) {
                    TextButton(
                        onClick = { viewModel.toggleShowOriginal() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.showOriginalPhoto) Icons.Default.AutoFixHigh else Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Slate700
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (viewModel.showOriginalPhoto) "Ver Otimizado (Sem Sombras)" else "Ver Original com Fundo",
                            fontSize = 12.sp,
                            color = Slate700
                        )
                    }
                }
            }

            if (viewModel.isScanning || viewModel.isProcessingPhoto) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProcessingCardAnimation(
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "Processando foto do cartão, aguarde...",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.scanEngine == "offline") Color(0xFFE8F5E9) else Color(0xFFEDE7F6)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (viewModel.scanEngine == "offline") Icons.Default.CheckCircle else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (viewModel.scanEngine == "offline") Color(0xFF2E7D32) else Color(0xFF673AB7),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (viewModel.scanEngine == "offline") "Leitura 100% Offline (ML Kit)" else "Leitura IA Online (Gemini)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.scanEngine == "offline") Color(0xFF1B5E20) else Color(0xFF512DA8)
                            )
                        }

                        TextButton(
                            onClick = {
                                if (viewModel.scanEngine == "offline") {
                                    viewModel.analyzeCardImage(preferOffline = false)
                                } else {
                                    viewModel.analyzeCardImage(preferOffline = true)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.scanEngine == "offline") "Tentar com Gemini" else "Repetir Offline",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            viewModel.scanError?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.analyzeCardImage(preferOffline = true) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Ler no Modo Offline (ML Kit)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Text(
                text = "Revisar dados estruturados",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Nome
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Nome:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedName,
                    onValueChange = { viewModel.parsedName = it },
                    placeholder = { Text("Nome do contato ou empresa", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate700) }
                )
            }

            // Telefone Principal
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Telefone principal (WhatsApp / Mensagens):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedPrimaryPhone,
                    onValueChange = { viewModel.parsedPrimaryPhone = it },
                    placeholder = { Text("DDD + Número WhatsApp", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_whatsapp_custom), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) },
                    supportingText = { Text("Definido prioritariamente como o telefone associado ao WhatsApp no cartão.") }
                )
            }

            // Telefone Secundário
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Telefone secundário (WhatsApp secundário):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedSecondaryPhone,
                    onValueChange = { viewModel.parsedSecondaryPhone = it },
                    placeholder = { Text("Segundo número de WhatsApp", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_whatsapp_custom), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) }
                )
            }

            // Telefone Comum / Fixo
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Telefone comum / fixo:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedLandlinePhone,
                    onValueChange = { viewModel.parsedLandlinePhone = it },
                    placeholder = { Text("Telefone fixo com DDD", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Slate700) },
                    supportingText = { Text("Identificado pelo ícone comum de telefone ou número de 8 dígitos.") }
                )
            }

            // E-mail
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "E-mail:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedEmail,
                    onValueChange = { viewModel.parsedEmail = it },
                    placeholder = { Text("contato@empresa.com.br", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate700) },
                    supportingText = { Text("Endereço de e-mail identificado no cartão.") }
                )
            }

            // Instagram
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Instagram (perfil, arroba ou link):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedInstagram,
                    onValueChange = { viewModel.parsedInstagram = it },
                    placeholder = { Text("@perfil ou link", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFE1306C)) },
                    supportingText = { Text("Perfil de Instagram extraído do cartão de visita.") }
                )
            }

            // Endereço
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Endereço:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedAddress,
                    onValueChange = { viewModel.parsedAddress = it },
                    placeholder = { Text("Endereço físico", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Slate700) }
                )
            }

            // Serviços / Observações
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Observações (Serviços e Soluções):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = viewModel.parsedObservations,
                    onValueChange = { viewModel.parsedObservations = it },
                    placeholder = { Text("Serviços ou soluções listadas no cartão", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = zapDeckTextFieldColors(),
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Slate700) },
                    supportingText = { Text("Compilado dinâmico das listagens ou especialidades do cartão.") }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Opções de Envio do WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900
                    )

                    if (existingWhatsAppContact != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Aviso contato whatsapp existente",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "whatsapp: O ${viewModel.parsedPrimaryPhone} já existe como ${existingWhatsAppContact.name} !",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (isCheckingWhatsAppNormal || isCheckingWhatsAppBusiness) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(text = "Validando se número existe no WhatsApp...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (viewModel.parsedPrimaryPhone.isNotBlank() && whatsAppChecked && !whatsAppNormalExists && !whatsAppBusinessExists) {
                        Text(
                            text = "Aviso: O número fornecido não possui formato válido de celular ou não foi encontrado no WhatsApp.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showNormal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendViaNormal,
                                onCheckedChange = { sendViaNormal = it },
                                enabled = whatsAppNormalExists && existingWhatsAppContact == null
                            )
                            Column {
                                Text(
                                    text = "Enviar via WhatsApp Padrão",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (whatsAppNormalExists && existingWhatsAppContact == null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (whatsAppNormalExists && existingWhatsAppContact == null) Slate900 else Slate400
                                )
                                if (viewModel.parsedPrimaryPhone.isNotBlank() && whatsAppChecked && !whatsAppNormalExists) {
                                    Text("Apenas disponível para formato de celular garantido", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    if (showBusiness) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendViaBusiness,
                                onCheckedChange = { sendViaBusiness = it },
                                enabled = whatsAppBusinessExists && existingWhatsAppContact == null
                            )
                            Column {
                                Text(
                                    text = "Enviar via WhatsApp Business",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (whatsAppBusinessExists && existingWhatsAppContact == null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (whatsAppBusinessExists && existingWhatsAppContact == null) Slate900 else Slate400
                                )
                                if (viewModel.parsedPrimaryPhone.isNotBlank() && whatsAppChecked && !whatsAppBusinessExists) {
                                    Text("Apenas disponível para formato de celular garantido", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    if (!isWhatsAppInstalled && !isWhatsAppBusinessInstalled) {
                        Text(
                            text = "(Nenhum WhatsApp detectado localmente no aparelho. Testando com verificação por formato)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val isMessageEnabled = (sendViaNormal || sendViaBusiness) && existingWhatsAppContact == null

                    Text(
                        text = "Mensagem para Enviar (Habilitado se WhatsApp selecionado):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (isMessageEnabled) Slate900 else Slate400,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isMessageEnabled,
                        maxLines = 8,
                        minLines = 3,
                        colors = zapDeckTextFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isMessageEnabled) Slate900 else Slate400
                        ),
                        shape = RoundedCornerShape(8.dp),
                        supportingText = { Text("Lembrete: O template utiliza seu nome de usuário (${viewModel.userName}).", fontSize = 10.sp) }
                    )
                }
            }

            // Instagram Checkbox (Optional)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (existingInstagramContact != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Aviso contato instagram existente",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "instagram: O instagram ${viewModel.parsedInstagram} já está sendo seguido !",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = followOnInstagram,
                            onCheckedChange = { followOnInstagram = it },
                            enabled = instagramExists && !isCheckingInstagram && viewModel.parsedInstagram.isNotBlank() && existingInstagramContact == null
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Abrir o Instagram ao salvar para seguir o contato",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (instagramExists && viewModel.parsedInstagram.isNotBlank() && existingInstagramContact == null) Slate900 else Slate400
                            )
                            if (isCheckingInstagram) {
                                Text(
                                    text = "Verificando se o perfil existe no Instagram...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (viewModel.parsedInstagram.isBlank()) {
                                Text(
                                    text = "Nenhum perfil de Instagram detectado no cartão.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (instagramChecked && !instagramExists) {
                                Text(
                                    text = "O contato ${viewModel.parsedInstagram} do Instagram não existe!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (instagramChecked && instagramExists && existingInstagramContact == null) {
                                Text(
                                    text = "Perfil verificado com sucesso no Instagram!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate700
                    ),
                    border = BorderStroke(1.dp, Slate400)
                ) {
                    Text("Descartar", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (viewModel.parsedName.isBlank()) {
                            Toast.makeText(context, "Por favor insira um nome antes de salvar", Toast.LENGTH_SHORT).show()
                        } else {
                            val phone = viewModel.parsedPrimaryPhone
                            val msgToSubmit = customMessage
                            val shouldOpenWhatsApp = sendViaNormal || sendViaBusiness
                            val isBusiness = sendViaBusiness
                            val instagramHandle = viewModel.parsedInstagram

                            viewModel.saveContact(isBusiness, followOnInstagram)
                            Toast.makeText(context, "Contato salvo com sucesso!", Toast.LENGTH_SHORT).show()
                            onSaved()

                            if (shouldOpenWhatsApp) {
                                if (phone.isNotBlank()) {
                                    MessagingIntegrationHelper.openWhatsAppChat(context, phone, msgToSubmit, isBusiness)
                                } else {
                                    Toast.makeText(context, "Contato salvo, mas sem telefone para envio do WhatsApp.", Toast.LENGTH_LONG).show()
                                }
                            }

                            if (followOnInstagram && instagramHandle.isNotBlank()) {
                                scope.launch {
                                    val exists = MessagingIntegrationHelper.checkIfInstagramUserExists(instagramHandle)
                                    if (exists) {
                                        MessagingIntegrationHelper.openInstagramProfile(context, instagramHandle)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "O contato $instagramHandle do Instagram não existe!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Salvar Registro")
                }
            }

            // Quick Share / Proximity Transfer for the freshly captured card
            var showDirectTransmitDialog by remember { mutableStateOf(false) }
            val previewCardEntity = remember(
                viewModel.parsedName,
                viewModel.parsedPrimaryPhone,
                viewModel.parsedSecondaryPhone,
                viewModel.parsedAddress,
                viewModel.parsedInstagram,
                viewModel.parsedObservations
            ) {
                ContactEntity(
                    id = 0,
                    name = viewModel.parsedName.ifBlank { "Novo Contato" },
                    primaryPhone = viewModel.parsedPrimaryPhone,
                    secondaryPhone = viewModel.parsedSecondaryPhone,
                    address = viewModel.parsedAddress,
                    instagram = viewModel.parsedInstagram,
                    observations = viewModel.parsedObservations,
                    imageBase64 = viewModel.capturedImageBase64 ?: ""
                )
            }

            if (showDirectTransmitDialog) {
                TransmitCardDialog(
                    contact = previewCardEntity,
                    onDismiss = { showDirectTransmitDialog = false }
                )
            }

            OutlinedButton(
                onClick = {
                    viewModel.startBroadcastingContact(previewCardEntity)
                    showDirectTransmitDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transmitir por Aproximação (NFC / QR)", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
