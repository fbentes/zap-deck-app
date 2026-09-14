package com.example

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ContactRepository
import com.example.ui.screen.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.CardBeamTransferHelper
import com.example.utils.NfcReaderHelper
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNfcIntent(intent)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current.applicationContext
                val database = AppDatabase.getDatabase(context)
                val repository = ContactRepository(database.contactDao())
                val vm: MainViewModel = viewModel(
                    factory = MainViewModelFactory(application, repository)
                )
                mainViewModel = vm

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable Reader Mode so if another phone approaches with ZapDeck open, it reads its card instantly
        NfcReaderHelper.enableReaderMode(this) { receivedContact ->
            mainViewModel?.handleReceivedContact(receivedContact)
        }
    }

    override fun onPause() {
        super.onPause()
        NfcReaderHelper.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val ndefMessage = rawMessages[0] as? NdefMessage
                val record = ndefMessage?.records?.firstOrNull()
                if (record != null) {
                    val payload = String(record.payload, StandardCharsets.UTF_8)
                    val contact = CardBeamTransferHelper.jsonToContact(payload)
                        ?: CardBeamTransferHelper.vCardToContact(payload)
                    mainViewModel?.handleReceivedContact(contact)
                }
            }
        }
    }
}
