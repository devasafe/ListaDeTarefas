package com.example.listadetarefas

import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientCallbackProvider
import androidx.fragment.app.FragmentActivity
import java.util.Locale

class MainActivity : FragmentActivity(), AmbientCallbackProvider, TextToSpeech.OnInitListener {

    private lateinit var audioManager: AudioManager
    private lateinit var audioHelper: AudioHelper
    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    private lateinit var listView: ListView
    private lateinit var btnLer: Button
    private lateinit var btnBluetooth: Button

    private var ttsReady = false
    private var selectedMessage: String? = null

    private val mensagens = listOf(
        "Alerta: Reunião em 10 minutos na sala 3.",
        "Notificação: Seu turno começa às 14h.",
        "Emergência: Evacuação do bloco B solicitada.",
        "Instrução: Use EPI ao entrar na área de produção.",
        "Lembrete: Treinamento de segurança amanhã às 9h."
    )

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesAdded(addedDevices)
            if (audioHelper.isBluetoothHeadsetConnected()) {
                runOnUiThread {
                    statusText.text = "Fone Bluetooth conectado"
                    Toast.makeText(this@MainActivity, "Bluetooth conectado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesRemoved(removedDevices)
            if (!audioHelper.isBluetoothHeadsetConnected()) {
                runOnUiThread {
                    statusText.text = "Bluetooth desconectado"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AmbientModeSupport.attach(this)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioHelper = AudioHelper(this)
        tts = TextToSpeech(this, this)

        statusText = findViewById(R.id.statusText)
        listView = findViewById(R.id.listView)
        btnLer = findViewById(R.id.btnLer)
        btnBluetooth = findViewById(R.id.btnBluetooth)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mensagens)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedMessage = mensagens[position]
            statusText.text = "Selecionado: ${position + 1}"
        }

        btnLer.setOnClickListener {
            val msg = selectedMessage
            if (msg == null) {
                Toast.makeText(this, "Selecione uma mensagem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!audioHelper.isSpeakerAvailable() && !audioHelper.isBluetoothHeadsetConnected()) {
                Toast.makeText(this, "Nenhuma saída de áudio disponível", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lerEmVozAlta(msg)
        }

        btnBluetooth.setOnClickListener {
            abrirConfiguracoesBluetooth()
        }

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        atualizarStatusAudio()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("pt", "BR")
            ttsReady = true
        }
    }

    private fun lerEmVozAlta(texto: String) {
        if (ttsReady) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "doma_msg")
        }
    }

    private fun abrirConfiguracoesBluetooth() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    private fun atualizarStatusAudio() {
        statusText.text = when {
            audioHelper.isBluetoothHeadsetConnected() -> "Bluetooth conectado"
            audioHelper.isSpeakerAvailable() -> "Alto-falante disponível"
            else -> "Sem saída de áudio"
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback =
        object : AmbientModeSupport.AmbientCallback() {}

    override fun onDestroy() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        tts.shutdown()
        super.onDestroy()
    }
}
