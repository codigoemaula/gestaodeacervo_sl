package br.gov.sp.sme.salaleitura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.gov.sp.sme.salaleitura.ui.SalaLeituraApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SalaLeituraApp() }
    }
}
