package br.com.facbentes.zapdeck.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.facbentes.zapdeck.data.ContactEntity
import br.com.facbentes.zapdeck.ui.component.ConfirmDeleteContactDialog
import br.com.facbentes.zapdeck.ui.component.ContactCard
import br.com.facbentes.zapdeck.ui.component.ContactQuickActionsDialog
import br.com.facbentes.zapdeck.ui.component.SwipeableContactCard
import br.com.facbentes.zapdeck.ui.theme.Slate400
import br.com.facbentes.zapdeck.ui.theme.Slate700
import br.com.facbentes.zapdeck.ui.theme.Slate900
import br.com.facbentes.zapdeck.ui.theme.ZapDeckBlueGradient
import br.com.facbentes.zapdeck.ui.theme.ZapDeckPrimary
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DashboardLayout(
    contacts: List<ContactEntity>,
    onContactSelected: (ContactEntity) -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    scanBackSide: Boolean,
    onScanBackSideChanged: (Boolean) -> Unit,
    showCardList: Boolean,
    onShowCardListChanged: (Boolean) -> Unit
) {
    var quickActionsContact by remember { mutableStateOf<ContactEntity?>(null) }
    var deleteConfirmContact by remember { mutableStateOf<ContactEntity?>(null) }

    if (!showCardList) {
        // Welcome Screen (Start Screen)
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF031B33), // Deep Navy
                            Color(0xFF0D3261), // Royal Slate Blue
                            Color(0xFF16529E)  // Active Vibrant Blue
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "ZapDeck Digitalizador",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Digitalize cartões físicos instantaneamente (Offline ou IA)",
                        fontSize = 13.5.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }

                // 3D-oriented layered cascading physical business cards simulation area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Card 1: Back (Carlos Lima)
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .height(135.dp)
                            .graphicsLayer(
                                rotationZ = -12f,
                                translationX = -30f,
                                translationY = -25f
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CARLOS LIMA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF00D215))
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                            Text("Reformas Residenciais e Pinturas", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("(11) 98765-4321", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                Text("São Paulo - SP", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }

                    // Card 2: Middle (Clínica Vida)
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .height(135.dp)
                            .graphicsLayer(
                                rotationZ = 8f,
                                translationX = 25f,
                                translationY = 5f
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CLÍNICA VIDA", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0056C6))
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFE1306C), modifier = Modifier.size(16.dp))
                            }
                            Text("Dra. Beatriz Sousa | Pediatra", fontSize = 10.sp, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("(21) 2211-1313", fontSize = 9.sp, color = Color.DarkGray)
                                Text("Rio de Janeiro - RJ", fontSize = 9.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    // Card 3: Front (Ana Silva)
                    val scannerColor = Color(0xFF00E5FF)
                    Card(
                        modifier = Modifier
                            .width(270.dp)
                            .height(145.dp)
                            .graphicsLayer(
                                rotationZ = -2f,
                                translationY = 32f
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.5.dp, scannerColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ANA SILVA", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                }
                                Text("UX Designer & Branding", fontSize = 11.sp, color = Color(0xFF64748B))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("contato@anasilva.design", fontSize = 10.sp, color = Color(0xFF334155))
                                    Text("Portfólio online", fontSize = 9.sp, color = Color(0xFF64748B))
                                }
                            }

                            // Scanner focus Corner lines to simulate scanner
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val lineSize = 16.dp.toPx()
                                val strokeW = 3.dp.toPx()
                                
                                drawLine(scannerColor, Offset(0f, 0f), Offset(lineSize, 0f), strokeWidth = strokeW)
                                drawLine(scannerColor, Offset(0f, 0f), Offset(0f, lineSize), strokeWidth = strokeW)
                                
                                drawLine(scannerColor, Offset(w, 0f), Offset(w - lineSize, 0f), strokeWidth = strokeW)
                                drawLine(scannerColor, Offset(w, 0f), Offset(0f, lineSize), strokeWidth = strokeW)
                                
                                drawLine(scannerColor, Offset(0f, h), Offset(lineSize, h), strokeWidth = strokeW)
                                drawLine(scannerColor, Offset(0f, h), Offset(0f, h - lineSize), strokeWidth = strokeW)
                                
                                drawLine(scannerColor, Offset(w, h), Offset(w - lineSize, h), strokeWidth = strokeW)
                                drawLine(scannerColor, Offset(w, h), Offset(w, h - lineSize), strokeWidth = strokeW)
                            }
                        }
                    }
                }

                // Interactive Primary Call to Actions Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Surface(
                        onClick = { onScanBackSideChanged(!scanBackSide) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (scanBackSide) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (scanBackSide) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = scanBackSide,
                                onCheckedChange = onScanBackSideChanged,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF00E5FF),
                                    checkmarkColor = Color(0xFF031B33),
                                    uncheckedColor = Color.White.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Digitalizar verso também (Frente e Verso)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (scanBackSide) "Captura a frente e em seguida o verso antes de digitalizar" else "Digitaliza somente a frente do cartão",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                            Icon(
                                imageVector = if (scanBackSide) Icons.Default.Flip else Icons.Default.CropPortrait,
                                contentDescription = null,
                                tint = if (scanBackSide) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        IconButton(
                            onClick = onPickGallery,
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Escolher da Galeria",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Button(
                            onClick = { onTakePhoto() },
                            modifier = Modifier.size(76.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0D3261)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Tirar foto",
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Box(modifier = Modifier.size(50.dp))
                    }

                    ElevatedButton(
                        onClick = { onShowCardListChanged(true) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "Listar Cartões/Contatos",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ver Meus Contatos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    } else {
        // List of Cards Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = ZapDeckBlueGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Buscar contatos por nome, serviço...", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.85f),
                        focusedLeadingIconColor = ZapDeckPrimary,
                        unfocusedLeadingIconColor = Slate700,
                        focusedTrailingIconColor = ZapDeckPrimary,
                        unfocusedTrailingIconColor = Slate700
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate700) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = Slate700)
                            }
                        }
                    }
                )

                BackHandler(enabled = showCardList) {
                    onShowCardListChanged(false)
                }

                if (contacts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.size(90.dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBox,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Nenhum resultado encontrado" else "Nenhum cartão salvo ainda",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Tente buscar com outros termos." else "Use o botão na tela inicial para capturar seu primeiro cartão!",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💡 Arraste ➔ para editar, ⬅ para excluir, ou segure para opções",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            SwipeableContactCard(
                                contact = contact,
                                onClick = { onContactSelected(contact) },
                                onLongClick = { quickActionsContact = contact },
                                onSwipeEdit = { onEditContact(contact) },
                                onSwipeDelete = { deleteConfirmContact = contact }
                            )
                        }
                    }
                }
            }

            // Quick Actions Dialog on Long-press
            quickActionsContact?.let { contact ->
                ContactQuickActionsDialog(
                    contact = contact,
                    onDismiss = { quickActionsContact = null },
                    onEdit = {
                        quickActionsContact = null
                        onEditContact(contact)
                    },
                    onDelete = {
                        quickActionsContact = null
                        deleteConfirmContact = contact
                    }
                )
            }

            // Confirmation Dialog on Swipe-to-delete or delete selection
            deleteConfirmContact?.let { contact ->
                ConfirmDeleteContactDialog(
                    contact = contact,
                    onDismiss = { deleteConfirmContact = null },
                    onConfirmDelete = {
                        val toDelete = contact
                        deleteConfirmContact = null
                        onDeleteContact(toDelete)
                    }
                )
            }
        }
    }
}
