package br.com.facbentes.zapdeck.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.facbentes.zapdeck.R
import br.com.facbentes.zapdeck.data.ContactEntity
import br.com.facbentes.zapdeck.integration.messaging.MessagingIntegrationHelper
import br.com.facbentes.zapdeck.utils.MapsHelper
import br.com.facbentes.zapdeck.ui.theme.Slate200
import br.com.facbentes.zapdeck.ui.theme.Slate400
import br.com.facbentes.zapdeck.ui.theme.Slate700
import br.com.facbentes.zapdeck.ui.theme.Slate800
import br.com.facbentes.zapdeck.ui.theme.Slate900
import br.com.facbentes.zapdeck.ui.theme.ZapDeckPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactDialog(
    contact: ContactEntity,
    onDismiss: () -> Unit,
    onSave: (ContactEntity) -> Unit
) {
    var name by remember { mutableStateOf(contact.name) }
    var primaryPhone by remember { mutableStateOf(contact.primaryPhone) }
    var secondaryPhone by remember { mutableStateOf(contact.secondaryPhone) }
    var landlinePhone by remember { mutableStateOf(contact.landlinePhone) }
    var email by remember { mutableStateOf(contact.email) }
    var instagram by remember { mutableStateOf(contact.instagram) }
    var address by remember { mutableStateOf(contact.address) }
    var observations by remember { mutableStateOf(contact.observations) }
    var useWhatsAppBusiness by remember { mutableStateOf(contact.useWhatsAppBusiness) }
    var instagramFollowed by remember { mutableStateOf(contact.instagramFollowed) }

    val editContext = LocalContext.current
    var whatsAppNormalExists by remember { mutableStateOf(false) }
    var whatsAppBusinessExists by remember { mutableStateOf(false) }

    val normalInstalled = remember(editContext) { MessagingIntegrationHelper.isAppInstalled(editContext, "com.whatsapp") }
    val businessInstalled = remember(editContext) { MessagingIntegrationHelper.isAppInstalled(editContext, "com.whatsapp.w4b") }

    LaunchedEffect(primaryPhone) {
        if (primaryPhone.isNotBlank()) {
            whatsAppNormalExists = MessagingIntegrationHelper.checkIfWhatsAppRegistered(editContext, primaryPhone, isBusiness = false)
            whatsAppBusinessExists = MessagingIntegrationHelper.checkIfWhatsAppRegistered(editContext, primaryPhone, isBusiness = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editar Contato",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar e sair da tela",
                                tint = Slate800,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(
                                        contact.copy(
                                            name = name,
                                            primaryPhone = primaryPhone,
                                            secondaryPhone = secondaryPhone,
                                            landlinePhone = landlinePhone,
                                            email = email,
                                            instagram = instagram,
                                            address = address,
                                            observations = observations,
                                            useWhatsAppBusiness = useWhatsAppBusiness,
                                            instagramFollowed = instagramFollowed
                                        )
                                    )
                                }
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ZapDeckPrimary,
                                contentColor = Color.White,
                                disabledContainerColor = ZapDeckPrimary.copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            ),
                            enabled = name.isNotBlank(),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Salvar contato",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Nome
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Nome:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Nome do contato", color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = zapDeckTextFieldColors(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate700) }
                    )
                }

                // Telefone Principal
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Telefone Principal (WhatsApp):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = primaryPhone,
                        onValueChange = { primaryPhone = it },
                        placeholder = { Text("DDD + Número WhatsApp", color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = zapDeckTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_whatsapp_custom), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) }
                    )
                }

                // Telefone Secundário
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Telefone Secundário (WhatsApp):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        placeholder = { Text("Segundo número WhatsApp", color = Slate400) },
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
                        text = "Telefone Comum / Fixo:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = landlinePhone,
                        onValueChange = { landlinePhone = it },
                        placeholder = { Text("Telefone fixo com DDD", color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = zapDeckTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Slate700) }
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
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("contato@empresa.com.br", color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = zapDeckTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate700) }
                    )
                }

                // Instagram
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Instagram:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = instagram,
                        onValueChange = { instagram = it },
                        placeholder = { Text("@perfil ou link", color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = zapDeckTextFieldColors(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFE1306C)) }
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
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("Endereço físico", color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = zapDeckTextFieldColors(),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Slate700) },
                        trailingIcon = {
                            if (address.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        MapsHelper.openAddressInMaps(editContext, address)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Abrir endereço no Google Maps",
                                        tint = Color(0xFF0056C6)
                                    )
                                }
                            }
                        }
                    )
                }

                // Serviços / Observações
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Serviços / Observações:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        placeholder = { Text("Serviços prestados ou observações", color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = zapDeckTextFieldColors(),
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Slate700) }
                    )
                }

                if (instagram.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = instagramFollowed,
                            onCheckedChange = { instagramFollowed = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ZapDeckPrimary,
                                uncheckedColor = Slate400,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Já sigo este contato no Instagram",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                    }
                }

                if (primaryPhone.isNotBlank() || instagram.isNotBlank()) {
                    Text(
                        text = "Ações Rápidas de Integração",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (primaryPhone.isNotBlank()) {
                            if (normalInstalled) {
                                Button(
                                    onClick = {
                                        MessagingIntegrationHelper.openWhatsAppChat(editContext, primaryPhone, "", false)
                                    },
                                    modifier = Modifier.size(54.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = whatsAppNormalExists
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_whatsapp_custom),
                                        contentDescription = "WhatsApp Padrão",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            if (businessInstalled) {
                                Button(
                                    onClick = {
                                        MessagingIntegrationHelper.openWhatsAppChat(editContext, primaryPhone, "", true)
                                    },
                                    modifier = Modifier.size(54.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = whatsAppBusinessExists
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_whatsapp_business_custom),
                                        contentDescription = "WhatsApp Business",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }
                        }

                        if (instagram.isNotBlank()) {
                            Button(
                                onClick = {
                                    MessagingIntegrationHelper.openInstagramProfile(editContext, instagram)
                                },
                                modifier = Modifier.size(54.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (instagramFollowed) Icons.Default.OpenInNew else Icons.Default.Person,
                                    contentDescription = "Instagram",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
