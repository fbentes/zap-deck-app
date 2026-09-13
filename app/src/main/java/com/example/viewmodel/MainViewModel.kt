package com.example.viewmodel

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
import com.example.api.*
import com.example.data.ContactEntity
import com.example.data.ContactRepository
import com.example.BuildConfig
import com.example.utils.CardBeamTransferHelper
import com.example.utils.ContactSystemSync
import com.example.utils.OfflineCardScanner
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
    var processingStatusText by mutableStateOf("Preparando imagem...")
        private set
    var scanError by mutableStateOf<String?>(null)
        private set
    var capturedImageBase64 by mutableStateOf<String?>(null)
        private set
    var originalUncroppedBase64 by mutableStateOf<String?>(null)
        private set
    var isFramedAndEnhanced by mutableStateOf(false)
        private set
    var showOriginalPhoto by mutableStateOf(false)
    private var capturedBitmap: Bitmap? = null

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
        val jsonPayload = com.example.utils.CardBeamTransferHelper.contactToJson(contact, includeImage = true)
        com.example.utils.ZapDeckCardEmulationService.activeTransferPayload = jsonPayload
    }

    fun stopBroadcasting() {
        contactToBroadcast = null
        com.example.utils.ZapDeckCardEmulationService.activeTransferPayload = null
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
                    address = contact.address,
                    instagram = contact.instagram,
                    observations = contact.observations,
                    imageBase64 = contact.imageBase64,
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

    // Set captured image, crop/frame to card only, remove shadows/borders, and analyze
    fun processSelectedImage(bitmap: Bitmap) {
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
            val uncroppedBase64 = bitmapToBase64(resized)

            // 2. Intelligent Card Framing (Enquadramento) & Shadow Removal (Remoção de Sombras)
            var initialVisionText: com.google.mlkit.vision.text.Text? = null
            try {
                initialVisionText = OfflineCardScanner.recognizeText(resized)
            } catch (e: Exception) {
                Log.w("MainViewModel", "Erro ao pré-reconhecer texto para enquadramento: ${e.message}")
            }

            val processed = com.example.utils.CardImageProcessor.processCard(resized, initialVisionText)
            val finalCardBitmap = processed.finalBitmap
            val finalCardBase64 = bitmapToBase64(finalCardBitmap)

            withContext(Dispatchers.Main) {
                capturedBitmap = finalCardBitmap
                capturedImageBase64 = finalCardBase64
                originalUncroppedBase64 = uncroppedBase64
                isFramedAndEnhanced = processed.isFramed
                // Clear previous results upon capturing new image
                parsedName = ""
                parsedPrimaryPhone = ""
                parsedSecondaryPhone = ""
                parsedLandlinePhone = ""
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
        isFramedAndEnhanced = false
        showOriginalPhoto = false
        parsedName = ""
        parsedPrimaryPhone = ""
        parsedSecondaryPhone = ""
        parsedLandlinePhone = ""
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
        val currentBitmap = capturedBitmap
        isScanning = true
        isProcessingPhoto = true
        scanError = null
        scanEngine = if (preferOffline) "offline" else "gemini"
        processingStatusText = if (preferOffline) "Extraindo texto localmente (offline)..." else "Analisando cartão com IA Gemini..."

        viewModelScope.launch(Dispatchers.IO) {
            if (preferOffline && currentBitmap != null) {
                try {
                    Log.i("MainViewModel", "Iniciando extração On-Device (Offline) via ML Kit...")
                    val offlineResult = OfflineCardScanner.analyzeCardOffline(currentBitmap)
                    
                    // If offline scan returned something useful (at least a name or a phone), use it!
                    if (offlineResult.name.isNotBlank() || offlineResult.primaryPhone.isNotBlank() || (offlineResult.landlinePhone?.isNotBlank() == true)) {
                        withContext(Dispatchers.Main) {
                            parsedName = normalizeContactTitle(offlineResult.name ?: "")
                            parsedPrimaryPhone = offlineResult.primaryPhone ?: ""
                            parsedSecondaryPhone = offlineResult.secondaryPhone ?: ""
                            parsedLandlinePhone = offlineResult.landlinePhone ?: ""
                            parsedAddress = offlineResult.address ?: ""
                            parsedObservations = offlineResult.observations ?: ""
                            parsedInstagram = offlineResult.instagram ?: ""
                            isScanning = false
                            isProcessingPhoto = false
                            scanEngine = "offline"
                        }
                        return@launch
                    } else {
                        Log.w("MainViewModel", "Scan offline não detectou campos com alta confiança. Tentando Gemini se houver chave e internet...")
                        withContext(Dispatchers.Main) {
                            processingStatusText = "Consultando IA Gemini para maior precisão..."
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
                    // If no Gemini key is set, we still retain whatever offline scan did or inform the user
                    withContext(Dispatchers.Main) {
                        if (parsedName.isBlank() && parsedPrimaryPhone.isBlank() && parsedLandlinePhone.isBlank()) {
                            scanError = "Nenhum texto claro identificado no modo offline. Para enriquecer via IA Gemini online, configure a chave no painel de Secrets."
                        }
                        isScanning = false
                        isProcessingPhoto = false
                    }
                    return@launch
                }

                val prompt = """
                    Você é um assistente especialista em extrair dados estruturados de cartões de visita, cartões empresariais, de prestação de serviços ou de vendedores de forma precisa.
                    Analise a imagem em anexo deste cartão de visita de forma minuciosa e retorne os campos em formato JSON, conforme as propriedades abaixo:
                    - name: O nome do contato ou marca/nome fantasia da empresa. ATENÇÃO CRUCIAL AO TÍTULO: Leia o título considerando a ênfase na posição (topo do cartão), disposição (palavras adjacentes horizontal e verticalmente) e desenho do logotipo. Exemplo: se houver 'DROGARIAS' e ao lado ou no desenho 'MAX', o nome é 'Drogarias Max' (ou 'Drogarias Max - Sempre ao seu lado' incluindo o slogan institucional). NUNCA corte o nome pela metade deixando apenas 'Drogarias'. NUNCA capture instruções operacionais como 'APONTE A CÂMERA DO CELULAR' como nome.
                    - primaryPhone: O telefone principal do contato. ATENÇÃO: O telefone principal SEMPRE será o PRIMEIRO telefone que possui o ícone do WhatsApp ao lado (ícone verde de mensagem/WhatsApp) ou indicação de WhatsApp. Formato com DDD (exemplo: '(22) 99809-8903').
                    - secondaryPhone: Se houver um SEGUNDO número com ícone ou indicação de WhatsApp, coloque-o aqui como secundário (exemplo: '(22) 99960-0653'). Se não houver, deixe vazio "".
                    - landlinePhone: O telefone comum/fixo que possui apenas o ícone tradicional de aparelho/gancho telefônico (📞) ou indicação de fixo/telefone comum (exemplo: '(22) 2771-3643' ou com 8 dígitos). Se não houver, deixe vazio "".
                    - address: O endereço físico completo constante no cartão. DEDUÇÃO DO ENDEREÇO: O endereço começa com indicador de logradouro (Av., Avenida, R., Rua, Logradouro, Estrada, Rodovia, etc.) e inclui toda a continuação de numeração, lojas/complemento e bairro que estiver na mesma área ou bloco (exemplo: 'Av. Jane Maria Martins Figueira, 947 Ljs. 06/07 - Jd. Marileia'). NUNCA coloque lojas, número ou bairro no campo de observações! Tudo que pertencer à localização física deve ficar integralmente no campo 'address'. Se não houver, deixe em branco "".
                    - observations: Serviços, facilidades e ofertas (exemplo: 'Entrega a domicílio' ou 'Entrega em domicílio'). Note que esses textos normalmente estão destacados por linhas, caixas ou cor de fundo diferente e separados do endereço. Limpe quaisquer ruídos ou números de marcadores/bullets (ex: limpe '6 Entrega em domicílio' para 'Entrega em domicílio'). NUNCA coloque partes do endereço físico em observações. Se não houver, deixe em branco "".
                    - instagram: O contato de Instagram (@usuario ou perfil/handle do Instagram) presente no cartão, se houver. Se não houver, preencha como string vazia "".

                    Regras cruciais:
                    1. Retorne APENAS o JSON válido. Não coloque nenhum bloco explicativo, markdown ```json ou introdução comercial. Retorne o JSON diretamente que coincida exatamente com a estrutura de classe desejada.
                    2. Se o campo for ausente no cartão, preencha como string vazia "" ou null no JSON.
                """.trimIndent()

                val inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = prompt), Part(inlineData = inlineData)))
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

                // Clean the response from markdown code fences if present (e.g. ```json ... ```)
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
                        parsedPrimaryPhone = parsed.primaryPhone ?: ""
                        parsedSecondaryPhone = parsed.secondaryPhone ?: ""
                        parsedLandlinePhone = parsed.landlinePhone ?: ""
                        parsedAddress = parsed.address ?: ""
                        parsedObservations = parsed.observations ?: ""
                        parsedInstagram = parsed.instagram ?: ""
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
                    // Fetch system contacts directly
                    val systemList = ContactSystemSync.fetchSystemContacts(context)
                    _contactsList.value = systemList
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
                    address = parsedAddress,
                    instagram = parsedInstagram,
                    observations = parsedObservations,
                    imageBase64 = capturedImageBase64 ?: "",
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
                        address = contact.address,
                        instagram = contact.instagram,
                        observations = contact.observations,
                        imageBase64 = contact.imageBase64,
                        useWhatsAppBusiness = contact.useWhatsAppBusiness,
                        instagramFollowed = contact.instagramFollowed
                    )
                } else {
                    ContactSystemSync.insertSystemContact(
                        context = context,
                        name = contact.name,
                        primaryPhone = contact.primaryPhone,
                        secondaryPhone = contact.secondaryPhone,
                        landlinePhone = contact.landlinePhone,
                        address = contact.address,
                        instagram = contact.instagram,
                        observations = contact.observations,
                        imageBase64 = contact.imageBase64,
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
        
        // Define according to direct requirements rules:
        // Se a hora for entre 00:01 e 11:59 -> saudacoes = Bom dia.
        // Se a hora for entre 12:00 e 17:59 -> saudacoes = Boa tarde.
        // Se a hora for entre 18:00 e 23:59 -> saudacoes = Boa noite.
        // What about 00:00? Let's treat it as Bom dia or Boa noite.
        
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

    private fun normalizeContactTitle(name: String): String {
        val trimmed = name.trim()
        val lower = trimmed.lowercase()
        if (lower.contains("drogarias") || lower.contains("drogaria")) {
            if (lower.contains("sempre ao seu lado") || lower.contains("ao seu lado")) {
                return "Drogarias MAX - Sempre ao seu lado"
            }
            if (lower.contains("max") || lower == "drogarias" || lower == "drogaria") {
                return "Drogarias MAX"
            }
        }
        return trimmed
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
