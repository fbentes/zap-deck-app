package com.example.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.ProcessingCardAnimation
import com.example.ui.ReceiveCardImportDialog
import com.example.ui.component.BackSideCapturePromptDialog
import com.example.ui.component.ContactDetailsDialog
import com.example.ui.component.EditContactDialog
import com.example.ui.component.SettingsDialog
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.utils.ApkShareHelper
import com.example.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCardList by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] ?: false
        if (readGranted && writeGranted) {
            viewModel.refreshContactsList()
            Toast.makeText(context, "Permissão de contatos concedida!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de contatos negada. Usando banco de dados local.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasRead || !hasWrite) {
            contactsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                )
            )
        } else {
            viewModel.refreshContactsList()
        }
    }

    LaunchedEffect(showCardList) {
        if (showCardList) {
            viewModel.refreshContactsList()
        }
    }

    LaunchedEffect(viewModel.isScanning) {
        if (!viewModel.isScanning && viewModel.capturedImageBase64 != null && viewModel.scanError == null) {
            val name = viewModel.parsedName
            val phone = viewModel.parsedPrimaryPhone
            val cleanPhone = phone.replace("\\D".toRegex(), "")
            if (name.isNotBlank() || cleanPhone.isNotBlank()) {
                val existing = contacts.find { lc ->
                    val cleanLcPrimary = lc.primaryPhone.replace("\\D".toRegex(), "")
                    val cleanLcSecondary = lc.secondaryPhone.replace("\\D".toRegex(), "")
                    (cleanPhone.isNotEmpty() && (cleanLcPrimary == cleanPhone || cleanLcSecondary == cleanPhone || cleanLcPrimary.endsWith(cleanPhone) || cleanPhone.endsWith(cleanLcPrimary))) ||
                    (name.isNotBlank() && lc.name.trim().equals(name.trim(), ignoreCase = true))
                }
                if (existing != null) {
                    val merged = existing.copy(
                        name = if (viewModel.parsedName.isNotBlank()) viewModel.parsedName else existing.name,
                        primaryPhone = if (viewModel.parsedPrimaryPhone.isNotBlank()) viewModel.parsedPrimaryPhone else existing.primaryPhone,
                        secondaryPhone = if (viewModel.parsedSecondaryPhone.isNotBlank()) viewModel.parsedSecondaryPhone else existing.secondaryPhone,
                        address = if (viewModel.parsedAddress.isNotBlank()) viewModel.parsedAddress else existing.address,
                        observations = if (viewModel.parsedObservations.isNotBlank()) viewModel.parsedObservations else existing.observations,
                        instagram = if (viewModel.parsedInstagram.isNotBlank()) viewModel.parsedInstagram else existing.instagram,
                        imageBase64 = viewModel.capturedImageBase64 ?: existing.imageBase64
                    )
                    viewModel.selectedContact = merged
                    viewModel.editModeActive = true
                    viewModel.clearScannedState()
                    Toast.makeText(context, "Contato já existe (${existing.name}). Abrindo edição para evitar redundância.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val tempFile = remember { File(context.cacheDir, "camera_capture.jpg") }
    val tempUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            if (bitmap != null) {
                viewModel.processSelectedImage(bitmap)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.processSelectedImage(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao abrir imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(tempUri)
        } else {
            Toast.makeText(context, "Permissão da Câmera é necessária para fotografar cartões", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            cameraLauncher.launch(tempUri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (showCardList && viewModel.capturedImageBase64 == null) {
                        IconButton(onClick = { showCardList = false }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Voltar para tela inicial",
                                tint = if (showCardList) Color.White else Slate900
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon_zc_1780517771865),
                            contentDescription = "ZapDeck Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Text(
                            text = "ZapDeck",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (showCardList) Color.White else Slate900
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            ApkShareHelper.shareInstalledApk(context)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar Aplicativo ZapDeck (APK)",
                            tint = if (showCardList) Color.White.copy(alpha = 0.95f) else Slate700
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar meu nome",
                            tint = if (showCardList) Color.White.copy(alpha = 0.95f) else Slate700
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showCardList) Color(0xFF031B33) else Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (viewModel.capturedImageBase64 != null && !viewModel.awaitingBackSideCapture) {
                ScanReviewLayout(
                    viewModel = viewModel,
                    onBack = { viewModel.clearScannedState() },
                    onSaved = { showCardList = true }
                )
            } else {
                val filteredContacts = remember(contacts, searchQuery) {
                    if (searchQuery.isBlank()) contacts else {
                        contacts.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.observations.contains(searchQuery, ignoreCase = true) ||
                                    it.primaryPhone.contains(searchQuery)
                        }
                    }
                }

                DashboardLayout(
                    contacts = filteredContacts,
                    onContactSelected = { viewModel.selectedContact = it },
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    onTakePhoto = { triggerCamera() },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    scanBackSide = viewModel.scanBackSideEnabled,
                    onScanBackSideChanged = { viewModel.scanBackSideEnabled = it },
                    showCardList = showCardList,
                    onShowCardListChanged = { showCardList = it }
                )
            }

            if (viewModel.awaitingBackSideCapture) {
                BackSideCapturePromptDialog(
                    frontImageBase64 = viewModel.capturedImageBase64,
                    onCaptureBackCamera = { triggerCamera() },
                    onCaptureBackGallery = { galleryLauncher.launch("image/*") },
                    onSkipBack = { viewModel.skipBackSideAndProcessOnlyFront() },
                    onCancel = { viewModel.cancelBackSideCapture() }
                )
            }

            if (viewModel.isProcessingPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 28.dp, vertical = 32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            ProcessingCardAnimation(
                                modifier = Modifier.size(110.dp)
                            )
                            Text(
                                text = "Processando foto do cartão, aguarde...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Slate900,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    currentName = viewModel.userName,
                    onDismiss = { showSettingsDialog = false },
                    onSave = {
                        viewModel.updateUserName(it)
                        showSettingsDialog = false
                        Toast.makeText(context, "Nome salvo com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (viewModel.showBeamReceiveDialog && viewModel.receivedBeamContact != null) {
                ReceiveCardImportDialog(
                    contact = viewModel.receivedBeamContact!!,
                    onDismiss = {
                        viewModel.showBeamReceiveDialog = false
                        viewModel.receivedBeamContact = null
                    },
                    onImport = { useBusiness, followInstagram ->
                        viewModel.importReceivedContactToAgenda(useBusiness, followInstagram)
                        Toast.makeText(context, "Cartão salvo com sucesso na sua agenda!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            viewModel.selectedContact?.let { contact ->
                if (viewModel.editModeActive) {
                    EditContactDialog(
                        contact = contact,
                        onDismiss = { 
                            viewModel.editModeActive = false 
                            viewModel.selectedContact = null
                        },
                        onSave = { updated ->
                            viewModel.saveEditedContact(updated)
                            viewModel.editModeActive = false
                            viewModel.selectedContact = null
                            Toast.makeText(context, "Contato atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    ContactDetailsDialog(
                        contact = contact,
                        userName = viewModel.userName,
                        getGreeting = { viewModel.getGreetingMessage() },
                        getTemplateMsg = { viewModel.getWhatsAppMessage() },
                        onDismiss = { viewModel.selectedContact = null },
                        onEdit = { viewModel.editModeActive = true },
                        onDelete = {
                            viewModel.deleteContact(contact)
                            viewModel.selectedContact = null
                            Toast.makeText(context, "Contato excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateContact = { updated ->
                            viewModel.saveEditedContact(updated)
                        }
                    )
                }
            }
        }
    }
}
