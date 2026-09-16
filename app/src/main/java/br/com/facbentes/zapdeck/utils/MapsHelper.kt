package br.com.facbentes.zapdeck.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MapsHelper {
    /**
     * Abre um endereço físico diretamente no aplicativo Google Maps,
     * ou no navegador web como fallback seguro.
     */
    fun openAddressInMaps(context: Context, address: String) {
        val trimmed = address.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, "Endereço não informado", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Tenta abrir especificamente no aplicativo Google Maps
            val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(trimmed))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                return
            }
        } catch (e: Exception) {
            // Continua para o fallback
        }

        // Fallback: abrir via URL padrão do Google Maps (funciona com qualquer navegador ou app de mapas instalado)
        try {
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(trimmed))
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            context.startActivity(browserIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Não foi possível abrir o mapa", Toast.LENGTH_SHORT).show()
        }
    }
}
