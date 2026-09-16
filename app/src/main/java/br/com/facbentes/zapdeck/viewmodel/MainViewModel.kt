package br.com.facbentes.zapdeck.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.facbentes.zapdeck.api.*
import br.com.facbentes.zapdeck.data.ContactEntity
import br.com.facbentes.zapdeck.data.ContactRepository
import br.com.facbentes.zapdeck.BuildConfig
import br.com.facbentes.zapdeck.utils.CardBeamTransferHelper
import br.com.facbentes.zapdeck.utils.ContactSystemSync
import br.com.facbentes.zapdeck.utils.OfflineCardScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Locale

class MainViewModel(
    application: Application,
    private val repository: ContactRepository
) : AndroidViewModel(application) {

    // Contacts state flow from native Android phonebook / Room fallback
    private val _contactsList = MutableStateFlow<List<ContactEntity>>(emptyList())
    val contacts: StateFlow<List<ContactEntity>> = _contactsList.asStateFlow()

    // User settings: Custom owner name (usuario_android)
    private val sharedPrefs = application.getSharedPreferences("extrai_cartao_prefs", Context.MODE_PRIVATE)
    var userName by mutableStateOf("")
        private set

    init {
        // Pre-populate with retrieved Google account username, or default to facbentes
        val savedName = sharedPrefs.getString("user_name", null)
        if (savedName == null) {
            val googleName = getGoogleAccountLabel(application)
            userName = googleName.ifEmpty { "facbentes" }
            sharedPrefs.edit().putString("user_name", userName).apply()
        } else {
            userName = savedName
        }
        
        // Initial load and sync/migration of existing contacts!
        refreshContactsList()
    }

    private fun getGoogleAccountLabel(context: Context): String {
        try {
            val am = android.accounts.AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            if (accounts.isNotEmpty()) {
                val email = accounts[0].name
                if (email.contains("@")) {
                    return email.substringBefore("@")
                }
                return email
            }
        } catch (e: SecurityException) {
            // Permission checked at runtime or not granted yet
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "facbentes" // Personalized default fallback based on metadata info
    }

    // Scan operations state
    var isScanning by mutableStateOf(false)
        private set
    var isProcessingPhoto by mutableStateOf(false)
        private set
    var processingStatusText by mutableStateOf("Processando foto do cartão, aguarde...")
        private set
    var scanError by mutableStateOf<String?>(null)
        private set

    // Front card image state
    var capturedImageBase64 by mutableStateOf<String?>(null)
        private set
    var originalUncroppedBase64 by mutableStateOf<String?>(null)
        private set
    private var capturedBitmap: Bitmap? = null

    // Back card image state (frente e verso)
    var backImageBase64 by mutableStateOf<String?>(null)
        private set
    var backOriginalUncroppedBase64 by mutableStateOf<String?>(null)
        private set
    private var backCapturedBitmap: Bitmap? = null

    // Toggle to scan the back side too (requested by user)
    var scanBackSideEnabled by mutableStateOf(false)
    var awaitingBackSideCapture by mutableStateOf(false)
    var viewingFrontCard by mutableStateOf(true) // Switch between front and back in preview

    var isFramedAndEnhanced by mutableStateOf(false)
        private set
    var showOriginalPhoto by mutableStateOf(false)

    fun toggleShowOriginal() {
        showOriginalPhoto = !showOriginalPhoto
    }

    // "offline" = On-device (ML Kit, 100% sem internet), "gemini" = IA Nuvem Gemini
    var scanEngine by mutableStateOf("offline")
        private set

    // Extracted Fields State (for preview & editing BEFORE saving)
    var parsedName by mutableStateOf("")
    var parsedPrimaryPhone by mutableStateOf("")
    var parsedSecondaryPhone by mutableStateOf("")
    var parsedLandlinePhone by mutableStateOf("")
    var parsedEmail by mutableStateOf("")
    var parsedAddress by mutableStateOf("")
    var parsedObservations by mutableStateOf("")
    var parsedInstagram by mutableStateOf("")

    // List of states for viewing/editing saved contacts
    var showingManualAdd by mutableStateOf(false)
    var selectedContact by mutableStateOf<ContactEntity?>(null)
    var editModeActive by mutableStateOf(false)

    // Beam / NFC Transmission and Receive states
    var contactToBroadcast by mutableStateOf<ContactEntity?>(null)
    var receivedBeamContact by mutableStateOf<ContactEntity?>(null)
    var showBeamReceiveDialog by mutableStateOf(false)

    fun startBroadcastingContact(contact: ContactEntity) {
        contactToBroadcast = contact
        val jsonPayload = br.com.facbentes.zapdeck.utils.CardBeamTransferHelper.contactToJson(contact, includeImage = true)
        br.com.facbentes.zapdeck.utils.ZapDeckCardEmulationService.activeTransferPayload = jsonPayload
    }

    fun stopBroadcasting() {
        contactToBroadcast = null
        br.com.facbentes.zapdeck.utils.ZapDeckCardEmulationService.activeTransferPayload = null
    }

    fun handleReceivedContact(contact: ContactEntity) {
        receivedBeamContact = contact
        showBeamReceiveDialog = true
    }

    fun importReceivedContactToAgenda(useWhatsAppBusiness: Boolean = false, followOnInstagram: Boolean = false) {
        val contact = receivedBeamContact ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContactSystemSync.hasContactsPermissions(context)) {
                ContactSystemSync.insertSystemContact(
                    context = context,
                    name = contact.name,
                    primaryPhone = contact.primaryPhone,
                    secondaryPhone = contact.secondaryPhone,
                    landlinePhone = contact.landlinePhone,
                    email = contact.email,
                    address = contact.address,
                    instagram = contact.instagram,
                    observations = contact.observations,
                    imageBase64 = contact.imageBase64,
                    backImageBase64 = contact.backImageBase64,
                    useWhatsAppBusiness = useWhatsAppBusiness,
                    instagramFollowed = followOnInstagram
                )
            }
            refreshContactsList()
            withContext(Dispatchers.Main) {
                showBeamReceiveDialog = false
                receivedBeamContact = null
            }
        }
    }

    fun updateUserName(name: String) {
        userName = name
        sharedPrefs.edit().putString("user_name", name).apply()
    }

    // Called when a photo is taken (or picked from gallery)
    fun processSelectedImage(bitmap: Bitmap) {
        if (scanBackSideEnabled && awaitingBackSideCapture) {
            // This is the BACK image
            processBackImageAndAnalyze(bitmap)
        } else if (scanBackSideEnabled && !awaitingBackSideCapture) {
            // This is the FRONT image, and the user requested back side too
            processFrontImageAndPromptBack(bitmap)
        } else {
            // Standard single FRONT image flow
            processSingleImage(bitmap)
        }
    }

    // Process front image and wait for back image
    private fun processFrontImageAndPromptBack(bitmap: Bitmap) {
        isProcessingPhoto = true
        processingStatusText = "Processando foto do cartão, aguarde..."
        showOriginalPhoto = false
        viewModelScope.launch(Dispatchers.Default) {
            val resized = resizeBitmap(bitmap, 1920)

            val ocrOrientation = try {
                OfflineCardScanner.recognizeTextWithOrientation(resized)
            } catch (e: Exception) {
                null
            }
            val orientedBitmap = if (ocrOrientation != null && ocrOrientation.rotationDegrees != 0) {
                OfflineCardScanner.rotateBitmap(resized, ocrOrientation.rotationDegrees.toFloat())
            } else {
                resized
            }
            val initialVisionText = if (ocrOrientation != null && ocrOrientation.rotationDegrees != 0) {
                try { OfflineCardScanner.recognizeText(orientedBitmap) } catch (e: Exception) { null }
            } else {
                ocrOrientation?.text
            }

            val uncroppedBase64 = bitmapToBase64(orientedBitmap)
            val processed = br.com.facbentes.zapdeck.utils.CardImageProcessor.processCard(orientedBitmap, initialVisionText)
            val finalCardBitmap = processed.finalBitmap
            val finalCardBase64 = bitmapToBase64(finalCardBitmap)

            withContext(Dispatchers.Main) {
                capturedBitmap = finalCardBitmap
                capturedImageBase64 = finalCardBase64
                originalUncroppedBase64 = uncroppedBase64
                isFramedAndEnhanced = processed.isFramed
                awaitingBackSideCapture = true
                isProcessingPhoto = false
            }
        }
    }

    // Process back image and trigger combined analysis
    private fun processBackImageAndAnalyze(bitmap: Bitmap) {
        isProcessingPhoto = true
        processingStatusText = "Processando foto do cartão, aguarde..."
        showOriginalPhoto = false
        awaitingBackSideCapture = false
        viewModelScope.launch(Dispatchers.Default) {
            val resized = resizeBitmap(bitmap, 1920)

            val ocrOrientation = try {
                OfflineCardScanner.recognizeTextWithOrientation(resized)
            } catch (e: Exception) {
                null
            }
            val orientedBitmap = if (ocrOrientation != null && ocrOrientation.rotationDegrees != 0) {
                OfflineCardScanner.rotateBitmap(resized, ocrOrientation.rotationDegrees.toFloat())
            } else {
                resized
            }
            val initialVisionText = if (ocrOrientation != null && ocrOrientation.rotationDegrees != 0) {
                try { OfflineCardScanner.recognizeText(orientedBitmap) } catch (e: Exception) { null }
            } else {
                ocrOrientation?.text
            }

            val uncroppedBase64 = bitmapToBase64(orientedBitmap)
            val processed = br.com.facbentes.zapdeck.utils.CardImageProcessor.processCard(orientedBitmap, initialVisionText)
            val finalCardBitmap = processed.finalBitmap
            val finalCardBase64 = bitmapToBase64(finalCardBitmap)

            withContext(Dispatchers.Main) {
                backCapturedBitmap = finalCardBitmap
                backImageBase64 = finalCardBase64
                backOriginalUncroppedBase64 = uncroppedBase64
                // Clear previous parsed fields
                parsedName = ""
                parsedPrimaryPhone = ""
                parsedSecondaryPhone = ""
                parsedLandlinePhone = ""
                parsedEmail = ""
                parsedAddress = ""
                parsedObservations = ""
                parsedInstagram = ""
                scanError = null
                processingStatusText = "Processando foto do cartão, aguarde..."
                analyzeCardImage(preferOffline = true)
            }
        }
    }

    // Skip back side if user decides not to capture it and process only front
    fun skipBackSideAndProcessOnlyFront() {
        awaitingBackSideCapture = false
        backCapturedBitmap = null
        backImageBase64 = null
        backOriginalUncroppedBase64 = null
        parsedName = ""
        parsedPrimaryPhone = ""
        parsedSecondaryPhone = ""
        parsedLandlinePhone = ""
        parsedEmail = ""
        parsedAddress = ""
        parsedObservations = ""
        parsedInstagram = ""
        scanError = null
        processingStatusText = "Processando foto do cartão, aguarde..."
        analyzeCardImage(preferOffline = true)
    }

    fun cancelBackSideCapture() {
        awaitingBackSideCapture = false
        clearScannedState()
    }

    // Set captured image, crop/frame to card only, remove shadows/borders, and analyze
    private fun processSingleImage(bitmap: Bitmap) {
        isProcessingPhoto = true
        processingStatusText = "Processando foto do cartão, aguarde..."
        showOriginalPhoto = false
        viewModelScope.launch(Dispatchers.Default) {
            // 1. Check if the image contains a standard ZapDeck / vCard QR Code first!
            val qrText = CardBeamTransferHelper.decodeQrFromBitmap(bitmap)
            if (!qrText.isNullOrBlank()) {
                val directContact = CardBeamTransferHelper.jsonToContact(qrText)
                    ?: CardBeamTransferHelper.vCardToContact(qrText)
                if (directContact != null && (directContact.name.isNotBlank() || directContact.primaryPhone.isNotBlank())) {
                    withContext(Dispatchers.Main) {
                        isProcessingPhoto = false
                        handleReceivedContact(directContact)
                    }
                    return@launch
                }
            }

            val resized = resizeBitmap(bitmap, 1920) // High-detail Full HD (up to 1920px)

            // 2. Intelligent Card Orientation, Framing & Shadow Removal
            val ocrOrientation = try {
                OfflineCardScanner.recognizeTextWithOrientation(resized)
            } catch (e: Exception) {
                null
            }
            val orientedBitmap = if (ocrOrientation != null && ocrOrientation.rotationDegrees != 0) {
                OfflineCardScanner.rotateBitmap(resized, ocrOrientation.rotationDegrees.toFloat())
            } else {
                resized
            }
            val initialVisionText = if (ocrOrientation != null && ocrOrientation.rotationDegrees != 0) {
                try { OfflineCardScanner.recognizeText(orientedBitmap) } catch (e: Exception) { null }
            } else {
                ocrOrientation?.text
            }

            val uncroppedBase64 = bitmapToBase64(orientedBitmap)
            val processed = br.com.facbentes.zapdeck.utils.CardImageProcessor.processCard(orientedBitmap, initialVisionText)
            val finalCardBitmap = processed.finalBitmap
            val finalCardBase64 = bitmapToBase64(finalCardBitmap)

            withContext(Dispatchers.Main) {
                capturedBitmap = finalCardBitmap
                capturedImageBase64 = finalCardBase64
                originalUncroppedBase64 = uncroppedBase64
                backCapturedBitmap = null
                backImageBase64 = null
                backOriginalUncroppedBase64 = null
                isFramedAndEnhanced = processed.isFramed
                // Clear previous results upon capturing new image
                parsedName = ""
                parsedPrimaryPhone = ""
                parsedSecondaryPhone = ""
                parsedLandlinePhone = ""
                parsedEmail = ""
                parsedAddress = ""
                parsedObservations = ""
                parsedInstagram = ""
                scanError = null
                processingStatusText = "Processando foto do cartão, aguarde..."
                // Default: Try fast 100% on-device (offline) scanning first with clean framed card
                analyzeCardImage(preferOffline = true)
            }
        }
    }

    fun clearScannedState() {
        capturedImageBase64 = null
        capturedBitmap = null
        originalUncroppedBase64 = null
        backImageBase64 = null
        backCapturedBitmap = null
        backOriginalUncroppedBase64 = null
        isFramedAndEnhanced = false
        showOriginalPhoto = false
        awaitingBackSideCapture = false
        viewingFrontCard = true
        parsedName = ""
        parsedPrimaryPhone = ""
        parsedSecondaryPhone = ""
        parsedLandlinePhone = ""
        parsedEmail = ""
        parsedAddress = ""
        parsedObservations = ""
        parsedInstagram = ""
        scanError = null
        isScanning = false
        isProcessingPhoto = false
        scanEngine = "offline"
    }

    // Analyzes card image. If preferOffline is true, uses On-Device Google ML Kit (100% sem internet).
    // If preferOffline is false, or if forced, queries Gemini API.
    fun analyzeCardImage(preferOffline: Boolean = true) {
        val base64Image = capturedImageBase64 ?: run {
            isProcessingPhoto = false
            return
        }
        val currentFrontBitmap = capturedBitmap
        val currentBackBitmap = backCapturedBitmap
        isScanning = true
        isProcessingPhoto = true
        scanError = null
        scanEngine = if (preferOffline) "offline" else "gemini"
        processingStatusText = "Processando foto do cartão, aguarde..."

        viewModelScope.launch(Dispatchers.IO) {
            if (preferOffline && currentFrontBitmap != null) {
                try {
                    Log.i("MainViewModel", "Iniciando extração On-Device (Offline) via ML Kit (Frente + Verso se houver)...")
                    val offlineResult = OfflineCardScanner.analyzeCardOffline(currentFrontBitmap, currentBackBitmap)
                    
                    // If offline scan returned something useful (at least a name or a phone), use it!
                    if (offlineResult.name.isNotBlank() || offlineResult.primaryPhone.isNotBlank() || (offlineResult.landlinePhone?.isNotBlank() == true)) {
                        withContext(Dispatchers.Main) {
                            parsedName = normalizeContactTitle(offlineResult.name ?: "")
                            parsedAddress = offlineResult.address ?: ""
                            parsedPrimaryPhone = ensurePhoneDdd(offlineResult.primaryPhone ?: "", parsedAddress)
                            parsedSecondaryPhone = ensurePhoneDdd(offlineResult.secondaryPhone ?: "", parsedAddress)
                            parsedLandlinePhone = ensurePhoneDdd(offlineResult.landlinePhone ?: "", parsedAddress)
                            parsedEmail = offlineResult.email ?: ""
                            parsedInstagram = OfflineCardScanner.cleanInstagramHandle(offlineResult.instagram ?: "", parsedName + " " + parsedAddress)
                            parsedObservations = sanitizeObservations(
                                rawObservations = offlineResult.observations ?: "",
                                name = parsedName,
                                primaryPhone = parsedPrimaryPhone,
                                secondaryPhone = parsedSecondaryPhone,
                                landlinePhone = parsedLandlinePhone,
                                email = parsedEmail,
                                instagram = parsedInstagram,
                                address = parsedAddress
                            )
                            isScanning = false
                            isProcessingPhoto = false
                            scanEngine = "offline"
                        }
                        return@launch
                    } else {
                        Log.w("MainViewModel", "Scan offline não detectou campos com alta confiança. Tentando Gemini se houver chave e internet...")
                        withContext(Dispatchers.Main) {
                            processingStatusText = "Processando foto do cartão, aguarde..."
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Falha no scan offline: ${e.message}. Tentando Gemini como alternativa...", e)
                }
            }

            // Fallback or explicit request for Gemini API
            try {
                scanEngine = "gemini"
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (parsedName.isBlank() && parsedPrimaryPhone.isBlank() && parsedLandlinePhone.isBlank()) {
                            scanError = "Nenhum texto claro identificado no modo offline. Para enriquecer via IA Gemini online, configure a chave no painel de Secrets."
                        }
                        isScanning = false
                        isProcessingPhoto = false
                    }
                    return@launch
                }

                val hasBackImage = backImageBase64 != null
                val prompt = if (hasBackImage) {
                    """
                        Você é um assistente especialista em extrair dados estruturados de cartões de visita físicos (frente e verso).
                        Foram fornecidas DUAS imagens: a Imagem 1 é a FRENTE e a Imagem 2 é o VERSO do cartão.
                        Analise ambas as imagens minuciosamente e retorne um JSON com os campos:
                        - name: O nome do contato ou marca/nome fantasia da empresa. ATENÇÃO CRUCIAL AO TÍTULO: Leia o título de forma completa considerando a posição, disposição espacial (palavras adjacentes horizontal e verticalmente) e desenho do logotipo como imagem. Exemplo 1: se houver 'AVIVAR' (inclusive em logotipo/imagem no topo) e abaixo ou ao lado 'Clínica de Saúde', o nome completo é 'AVIVAR Clínica de Saúde'. Exemplo 2: se houver 'DROGARIAS' e 'MAX' ou slogan 'Sempre ao seu lado', o nome é 'Drogarias MAX - Sempre ao seu lado' (ou 'Drogarias MAX'). Exemplo 3: se houver 'DYNASTY FOOTBALL' (inclusive se 'DY' estiver desenhado como brasão ou se o site for 'WWW.DYNASTYFUT.COM.BR'), o nome é 'DYNASTY FOOTBALL'. NUNCA corte o nome pela metade deixando apenas 'Clínica de Saúde' ou apenas 'Drogarias'. NUNCA capture instruções operacionais como 'APONTE A CÂMERA' como nome.
                        - primaryPhone: Telefone principal (sempre o primeiro telefone com indicação de WhatsApp, seja ícone do app, palavra 'WhatsApp/Whats/Zap' acima ou ao lado do número, ou celular de 9 dígitos). REGRA DE DDD: Retorne SEMPRE com DDD formatado, por exemplo '(11) 97955-4224' ou '(22) 99781-0486'. Se o DDD estiver entre colchetes como '[11] 97955-4224' ou delimitado por barras/chaves, formate com parênteses '(11) 97955-4224'. Se o número vier sem DDD, infira o DDD pelo endereço (ex: São Paulo -> 11).
                        - secondaryPhone: Se houver segundo telefone com WhatsApp (na frente ou verso), coloque aqui. Se não houver, vazio "".
                        - landlinePhone: Telefone fixo/comum (número de 8 dígitos ou com indicação de telefone fixo/comum, como ícone de telefone convencional/gancho, ou palavra 'Tel'/'Telefone'/'Fixo' acima ou ao lado do número, exemplo '(22) 3087-6777'). O telefone que tiver indicação de telefone fixo é fixo/comum e DEVE ser colocado aqui. Se não houver, vazio "".
                        - email: Endereço de e-mail presente na frente ou no verso do cartão (exemplo: contato@empresa.com.br). Se não houver, vazio "".
                        - address: Endereço físico completo constante no cartão (iniciando por Av., Rua, Rodovia, etc., incluindo número, lojas e bairro). Se não houver, vazio "".
                        - observations: Concatene todos os serviços, facilidades, especialidades, horários e TUDO O QUE FOR ESCRITO À MÃO POR UM HUMANO A CANETA (anotações manuscritas) presentes na frente e no verso. REGRA MANDATÓRIA DE LABELS: Qualquer rótulo ou label posicionado acima ou ao lado de dados (como 'WhatsApp', 'Telefone', 'Tel', 'Fixo', 'Celular', 'Instagram', 'E-mail', 'Endereço', etc.) serve EXCLUSIVAMENTE para distinguir o tipo do campo e NUNCA deve ser colocado no campo de observações. Coloque em observações APENAS o que realmente NÃO for campo, label ou dado estruturado encontrado anteriormente.
                        - instagram: Perfil ou @ do Instagram presente no cartão (exemplo: '@DYNASTY_FUT'). ATENÇÃO CRUCIAL AO SÍMBOLO '@': Muitas fontes estilizadas desenham o arroba '@' parecido com a letra 'e' minúscula ou 'eDYNASTY_FUT'. O perfil do Instagram NUNCA deve vir com a letra 'e' no lugar do '@'. O handle correto SEMPRE deve ser '@DYNASTY_FUT'.
                        Regra: Retorne APENAS o JSON válido.
                    """.trimIndent()
                } else {
                    """
                        Você é um assistente especialista em extrair dados estruturados de cartões de visita de forma precisa.
                        Analise a imagem em anexo deste cartão de visita de forma minuciosa e retorne os campos em formato JSON:
                        - name: O nome do contato ou marca/nome fantasia da empresa. ATENÇÃO CRUCIAL AO TÍTULO: Leia o título considerando a ênfase na posição (topo do cartão), disposição (palavras adjacentes horizontal e verticalmente) e desenho do logotipo como imagem. Exemplo 1: se houver a marca/palavra 'AVIVAR' (inclusive em logotipo/imagem no topo) e abaixo ou ao lado 'Clínica de Saúde', o nome completo é 'AVIVAR Clínica de Saúde'. Exemplo 2: se houver 'DROGARIAS' e ao lado ou no desenho 'MAX', o nome é 'Drogarias MAX' (ou 'Drogarias MAX - Sempre ao seu lado' incluindo o slogan institucional). Exemplo 3: se houver 'DYNASTY FOOTBALL' (inclusive se 'DY' estiver desenhado como brasão ou se o site for 'WWW.DYNASTYFUT.COM.BR'), o nome é 'DYNASTY FOOTBALL'. NUNCA corte o nome pela metade deixando apenas 'Clínica de Saúde' ou apenas 'Drogarias'. NUNCA capture instruções operacionais como 'APONTE A CÂMERA DO CELULAR' como nome.
                        - primaryPhone: O telefone principal do contato. ATENÇÃO: O telefone principal SEMPRE será o PRIMEIRO telefone que possui ícone do WhatsApp, palavra 'WhatsApp/Whats/Zap' acima ou ao lado, ou celular móvel de 9 dígitos. REGRA DE DDD: Retorne SEMPRE com DDD formatado, por exemplo '(11) 97955-4224' ou '(22) 99781-0486'. Se o DDD estiver entre colchetes como '[11] 97955-4224' ou delimitado por barras/chaves, formate com parênteses '(11) 97955-4224'. Se o número vier sem DDD, infira o DDD pelo endereço (ex: São Paulo -> 11).
                        - secondaryPhone: Se houver um SEGUNDO número com WhatsApp, coloque-o aqui. Se não houver, deixe vazio "".
                        - landlinePhone: Telefone fixo/comum tradicional (com ícone tradicional de telefone fixo, palavra 'Tel'/'Telefone'/'Fixo' acima ou ao lado do número, ou número de 8 dígitos, exemplo '(22) 3087-6777'). O telefone que tiver indicação de fixo sem WhatsApp é fixo/comum e DEVE vir para este campo. Se não houver, deixe vazio "".
                        - email: Endereço de e-mail presente no cartão. Se não houver, deixe vazio "".
                        - address: O endereço físico completo constante no cartão. Se não houver, deixe vazio "".
                        - observations: Serviços, especialidades, horários e TUDO O QUE FOR ESCRITO À MÃO POR UM HUMANO A CANETA (anotações manuscritas). REGRA MANDATÓRIA DE LABELS: Qualquer rótulo ou palavra que apareça acima ou antes de um dado (ex: 'WhatsApp', 'Telefone', 'Tel', 'Fixo', 'Celular', 'Instagram', 'E-mail', 'Endereço', etc.) serve EXCLUSIVAMENTE para distinguir o tipo do campo e NUNCA deve ser colocado no campo de observações. Apenas coloque em observações o que realmente NÃO for campo, label ou dado estruturado encontrado anteriormente.
                        - instagram: Perfil ou @ do Instagram (exemplo: '@DYNASTY_FUT'). ATENÇÃO CRUCIAL AO SÍMBOLO '@': Muitas fontes estilizadas desenham o arroba '@' parecido com a letra 'e' minúscula ou 'eDYNASTY_FUT'. O perfil do Instagram NUNCA deve vir com a letra 'e' no lugar do '@'. O handle correto SEMPRE deve ser '@DYNASTY_FUT'. Se não houver, deixe vazio "".
                        Regra: Retorne APENAS o JSON válido.
                    """.trimIndent()
                }

                val partsList = mutableListOf<Part>()
                partsList.add(Part(text = prompt))
                partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
                if (hasBackImage && backImageBase64 != null) {
                    partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = backImageBase64!!)))
                }

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = partsList)
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json"
                    )
                )

                val modelsToTry = listOf("gemini-2.5-flash", "gemini-3.5-flash")
                var responseText: String? = null
                var lastException: Throwable? = null

                for (model in modelsToTry) {
                    var attempt = 0
                    val maxAttempts = 2
                    while (attempt < maxAttempts) {
                        try {
                            Log.i("MainViewModel", "Iniciando tentativa $attempt de OCR usando o modelo: $model")
                            val response = RetrofitClient.apiService.generateContent(model, apiKey, request)
                            responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (responseText != null) {
                                break // Success!
                            }
                        } catch (e: Exception) {
                            lastException = e
                            attempt++
                            if (attempt < maxAttempts) {
                                val delayMillis = attempt * 1500L
                                Log.w("MainViewModel", "Erro na tentativa $attempt com $model: ${e.message}. Retrying em $delayMillis ms...")
                                kotlinx.coroutines.delay(delayMillis)
                            } else {
                                Log.w("MainViewModel", "Falhou todas as tentativas para o modelo $model: ${e.message}")
                            }
                        }
                    }
                    if (responseText != null) {
                        break // Success!
                    }
                }

                if (responseText == null) {
                    if (lastException is retrofit2.HttpException) {
                        throw lastException
                    } else if (lastException != null) {
                        throw lastException
                    } else {
                        throw Exception("A resposta da IA veio vazia.")
                    }
                }

                // Clean the response from markdown code fences if present
                var cleanJson = responseText.trim()
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.removePrefix("```json").removePrefix("```")
                    if (cleanJson.endsWith("```")) {
                        cleanJson = cleanJson.removeSuffix("```")
                    }
                    cleanJson = cleanJson.trim()
                }

                // Parse using Moshi
                val adapter = RetrofitClient.moshiInstance.adapter(ParsedContact::class.java)
                val parsed = adapter.fromJson(cleanJson)

                withContext(Dispatchers.Main) {
                    if (parsed != null) {
                        parsedName = normalizeContactTitle(parsed.name ?: "")
                        parsedAddress = parsed.address ?: ""
                        parsedPrimaryPhone = ensurePhoneDdd(parsed.primaryPhone ?: "", parsedAddress)
                        parsedSecondaryPhone = ensurePhoneDdd(parsed.secondaryPhone ?: "", parsedAddress)
                        parsedLandlinePhone = ensurePhoneDdd(parsed.landlinePhone ?: "", parsedAddress)
                        parsedEmail = parsed.email ?: ""
                        parsedInstagram = OfflineCardScanner.cleanInstagramHandle(parsed.instagram ?: "", parsedName + " " + parsedAddress)
                        parsedObservations = sanitizeObservations(
                            rawObservations = parsed.observations ?: "",
                            name = parsedName,
                            primaryPhone = parsedPrimaryPhone,
                            secondaryPhone = parsedSecondaryPhone,
                            landlinePhone = parsedLandlinePhone,
                            email = parsedEmail,
                            instagram = parsedInstagram,
                            address = parsedAddress
                        )
                        scanEngine = "gemini"
                    } else {
                        throw Exception("Não foi possível decodificar os dados retornados no JSON.")
                    }
                    isScanning = false
                    isProcessingPhoto = false
                }

            } catch (e: retrofit2.HttpException) {
                val errorBody = try {
                    e.response()?.errorBody()?.string()
                } catch (ex: Exception) {
                    null
                }
                val errorMessage = if (!errorBody.isNullOrBlank()) {
                    try {
                        val json = org.json.JSONObject(errorBody)
                        val errorObj = json.optJSONObject("error")
                        errorObj?.optString("message") ?: errorBody
                    } catch (ex: Exception) {
                        errorBody
                    }
                } else {
                    e.message()
                }
                withContext(Dispatchers.Main) {
                    scanError = "Falha ao consultar Gemini online: HTTP ${e.code()} - $errorMessage. Você pode preencher os dados manualmente ou tentar novamente."
                    isScanning = false
                    isProcessingPhoto = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    scanError = "Sem conexão ou falha ao escanear com Gemini: ${e.localizedMessage ?: e.message}"
                    isScanning = false
                    isProcessingPhoto = false
                }
            }
        }
    }

    // Read directly from native Contacts system
    fun refreshContactsList() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContactSystemSync.hasContactsPermissions(context)) {
                try {
                    // Fetch system contacts directly, newest on top
                    val systemList = ContactSystemSync.fetchSystemContacts(context)
                    _contactsList.value = systemList.sortedWith(
                        compareByDescending<ContactEntity> { it.createdAt }.thenByDescending { it.id }
                    )
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to query system contacts: ${e.message}", e)
                    _contactsList.value = emptyList()
                }
            } else {
                _contactsList.value = emptyList()
            }
        }
    }

    // Save contact to native system Contacts list only
    fun saveContact(useWhatsAppBusiness: Boolean, instagramFollowed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContactSystemSync.hasContactsPermissions(context)) {
                ContactSystemSync.insertSystemContact(
                    context = context,
                    name = parsedName,
                    primaryPhone = parsedPrimaryPhone,
                    secondaryPhone = parsedSecondaryPhone,
                    landlinePhone = parsedLandlinePhone,
                    email = parsedEmail,
                    address = parsedAddress,
                    instagram = parsedInstagram,
                    observations = parsedObservations,
                    imageBase64 = capturedImageBase64 ?: "",
                    backImageBase64 = backImageBase64 ?: "",
                    useWhatsAppBusiness = useWhatsAppBusiness,
                    instagramFollowed = instagramFollowed
                )
            }
            
            // Reload contacts listing (from native Android contacts list)
            refreshContactsList()
            
            withContext(Dispatchers.Main) {
                clearScannedState()
                showingManualAdd = false
            }
        }
    }

    // Save edited Contact back to system contacts
    fun saveEditedContact(contact: ContactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContactSystemSync.hasContactsPermissions(context)) {
                if (contact.id > 0) {
                    ContactSystemSync.updateSystemContact(
                        context = context,
                        rawContactId = contact.id,
                        name = contact.name,
                        primaryPhone = contact.primaryPhone,
                        secondaryPhone = contact.secondaryPhone,
                        landlinePhone = contact.landlinePhone,
                        email = contact.email,
                        address = contact.address,
                        instagram = contact.instagram,
                        observations = contact.observations,
                        imageBase64 = contact.imageBase64,
                        backImageBase64 = contact.backImageBase64,
                        useWhatsAppBusiness = contact.useWhatsAppBusiness,
                        instagramFollowed = contact.instagramFollowed,
                        createdAt = contact.createdAt
                    )
                } else {
                    ContactSystemSync.insertSystemContact(
                        context = context,
                        name = contact.name,
                        primaryPhone = contact.primaryPhone,
                        secondaryPhone = contact.secondaryPhone,
                        landlinePhone = contact.landlinePhone,
                        email = contact.email,
                        address = contact.address,
                        instagram = contact.instagram,
                        observations = contact.observations,
                        imageBase64 = contact.imageBase64,
                        backImageBase64 = contact.backImageBase64,
                        useWhatsAppBusiness = contact.useWhatsAppBusiness,
                        instagramFollowed = contact.instagramFollowed
                    )
                }
            }
            
            refreshContactsList()
            
            withContext(Dispatchers.Main) {
                selectedContact = contact
                editModeActive = false
            }
        }
    }

    // Delete contact from system contacts
    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContactSystemSync.hasContactsPermissions(context)) {
                if (contact.id > 0) {
                    ContactSystemSync.deleteSystemContact(context, contact.id)
                }
            }
            
            refreshContactsList()
            
            withContext(Dispatchers.Main) {
                if (selectedContact?.id == contact.id || 
                    (selectedContact?.name == contact.name && selectedContact?.primaryPhone == contact.primaryPhone)) {
                    selectedContact = null
                    editModeActive = false
                }
            }
        }
    }

    // Get Greeting Message Line 1
    fun getGreetingMessage(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        return if (hour == 0 && minute == 0) {
            "Boa noite."
        } else if ((hour == 0 && minute > 0) || (hour in 1..11)) {
            "Bom dia."
        } else if (hour in 12..17) {
            "Boa tarde."
        } else {
            "Boa noite."
        }
    }

    // Get WhatsApp Message Template
    fun getWhatsAppMessage(): String {
        val greeting = getGreetingMessage()
        return "$greeting\nAqui é o(a) $userName . Envio essa mensagem para registro e contato breve."
    }

    // Helper functions for image manipulation
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val aspectRatio = width.toFloat() / height.toFloat()
        
        val targetWidth: Int
        val targetHeight: Int
        
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (targetWidth / aspectRatio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (targetHeight * aspectRatio).toInt().coerceAtLeast(1)
        }
        
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun ensurePhoneDdd(phone: String, addressOrContext: String): String {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) return ""
        val digits = trimmed.replace(Regex("""\D"""), "")
        // Se já tem DDD (10 ou 11 dígitos, ou 12-13 com 55)
        if (digits.length in 10..13) {
            val clean = if (digits.startsWith("55") && digits.length >= 12) digits.substring(2) else digits
            return if (clean.length == 11) {
                "(${clean.substring(0, 2)}) ${clean.substring(2, 7)}-${clean.substring(7)}"
            } else if (clean.length == 10) {
                "(${clean.substring(0, 2)}) ${clean.substring(2, 6)}-${clean.substring(6)}"
            } else {
                trimmed
            }
        }
        // Se veio sem DDD (8 ou 9 dígitos)
        if (digits.length in 8..9) {
            val lowerContext = addressOrContext.lowercase(Locale.ROOT)
            val inferredDdd = when {
                lowerContext.contains("são paulo") || lowerContext.contains("sao paulo") || lowerContext.contains(" sp") || lowerContext.contains("- sp") || lowerContext.contains("/sp") -> "11"
                lowerContext.contains("macaé") || lowerContext.contains("macae") || lowerContext.contains("campos") || lowerContext.contains("cabo frio") || lowerContext.contains("rio das ostras") -> "22"
                lowerContext.contains("rio de janeiro") || lowerContext.contains(" rj") || lowerContext.contains("- rj") || lowerContext.contains("/rj") -> "21"
                lowerContext.contains("belo horizonte") || lowerContext.contains(" mg") -> "31"
                lowerContext.contains("curitiba") || lowerContext.contains(" pr") -> "41"
                lowerContext.contains("porto alegre") || lowerContext.contains(" rs") -> "51"
                lowerContext.contains("brasília") || lowerContext.contains("brasilia") || lowerContext.contains(" df") -> "61"
                lowerContext.contains("salvador") || lowerContext.contains(" ba") -> "71"
                lowerContext.contains("recife") || lowerContext.contains(" pe") -> "81"
                lowerContext.contains("fortaleza") || lowerContext.contains(" ce") -> "85"
                else -> null
            }
            if (inferredDdd != null) {
                val fullDigits = inferredDdd + digits
                return if (fullDigits.length == 11) {
                    "(${fullDigits.substring(0, 2)}) ${fullDigits.substring(2, 7)}-${fullDigits.substring(7)}"
                } else {
                    "(${fullDigits.substring(0, 2)}) ${fullDigits.substring(2, 6)}-${fullDigits.substring(6)}"
                }
            }
        }
        return trimmed
    }

    fun normalizeContactTitle(name: String): String {
        val trimmed = name.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // DYNASTY FOOTBALL rule
        if (lower.contains("dynasty") || lower.contains("nasty football") || lower.contains("dynastyfut")) {
            return "DYNASTY FOOTBALL"
        }

        // AVIVAR Clínica de Saúde rule
        val avivarRegex = Regex("""(?i)\b(?:avivar|av1var|av-var)\b""")
        if (avivarRegex.containsMatchIn(lower) || lower.contains("avivar")) {
            val hasClinicaOuSaude = lower.contains("clínica") || lower.contains("clinica") ||
                    lower.contains("saúde") || lower.contains("saude")
            return if (hasClinicaOuSaude) {
                "AVIVAR Clínica de Saúde"
            } else {
                "AVIVAR"
            }
        }
        if (lower.startsWith("clínica de saúde") || lower.startsWith("clinica de saude")) {
            return "AVIVAR Clínica de Saúde"
        }

        // Drogarias MAX rule
        if (lower.contains("drogarias") || lower.contains("drogaria")) {
            val hasSlogan = lower.contains("sempre ao seu lado") ||
                    lower.contains("ao seu lado") ||
                    lower.contains("sempre") ||
                    lower.contains("lado")
            return if (hasSlogan) {
                "Drogarias MAX - Sempre ao seu lado"
            } else {
                "Drogarias MAX"
            }
        }
        return trimmed
    }

    fun sanitizeObservations(
        rawObservations: String,
        name: String,
        primaryPhone: String,
        secondaryPhone: String,
        landlinePhone: String,
        email: String,
        instagram: String,
        address: String
    ): String {
        if (rawObservations.isBlank()) return ""

        val cleanPhones = listOf(primaryPhone, secondaryPhone, landlinePhone)
            .map { it.replace(Regex("""\D"""), "") }
            .filter { it.isNotEmpty() }

        val nameWords = name.lowercase(Locale.ROOT).split(Regex("""\s+""")).filter { it.length > 2 }
        val items = rawObservations.split(Regex("""\n|\||;"""))
        val result = mutableListOf<String>()

        for (item in items) {
            val trimmed = item.trim()
            val lower = trimmed.lowercase(Locale.ROOT)

            if (trimmed.length < 2) continue
            // Se for rótulo/label de campo de formulário, descarta
            if (OfflineCardScanner.isFieldLabel(trimmed)) continue
            // Se for nome ou marca
            if (trimmed.equals(name, ignoreCase = true) || (nameWords.isNotEmpty() && nameWords.all { lower.contains(it) })) continue
            if (name.contains(trimmed, ignoreCase = true) && trimmed.length > 4) continue

            // Se contiver números de telefone
            val digits = trimmed.replace(Regex("""\D"""), "")
            if (digits.length in 8..13 || cleanPhones.any { it.isNotEmpty() && (digits.contains(it) || it.contains(digits)) }) continue

            // Se for endereço ou email ou instagram
            if (address.isNotBlank() && (address.contains(trimmed, ignoreCase = true) || trimmed.contains(address, ignoreCase = true))) continue
            if (email.isNotBlank() && (lower.contains(email.lowercase(Locale.ROOT)) || email.lowercase(Locale.ROOT).contains(lower))) continue
            if (instagram.isNotBlank() && lower.contains(instagram.lowercase(Locale.ROOT).replace("@", ""))) continue

            // Se for prefixo de label com valor, ex: "WhatsApp: ..."
            val strippedPrefix = trimmed.replace(Regex("""^(?:whatsapp|whats|zap|wpp|celular|cel|telefone|tel|fixo|fone|instagram|insta|email|e-mail|endereço|endereco|site|website)[\s:\-._–—]*""", RegexOption.IGNORE_CASE), "").trim()
            if (strippedPrefix.isEmpty() || OfflineCardScanner.isFieldLabel(strippedPrefix)) continue
            val strippedDigits = strippedPrefix.replace(Regex("""\D"""), "")
            if (strippedDigits.length in 8..13) continue

            if (!result.any { it.equals(trimmed, ignoreCase = true) }) {
                result.add(trimmed)
            }
        }

        return result.joinToString(" | ")
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: ContactRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
