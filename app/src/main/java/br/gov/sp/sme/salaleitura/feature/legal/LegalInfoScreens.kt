@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun TermsPrivacyScreen(onBack: () -> Unit) {
    val urls = LocalUriHandler.current
    Scaffold(topBar = {
        TopAppBar(title = { Text("Termos de uso e privacidade") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Voltar") }
        })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Como usar o Sala de Leitura", style = MaterialTheme.typography.headlineSmall)
            Text("Informações sobre as funções disponíveis, o funcionamento offline e os cuidados com dados pessoais. Este texto não substitui a avaliação da unidade responsável sobre a LGPD.")
            LegalInfoContent.termsSections.forEach { (heading, explanation) ->
                Text(heading, style = MaterialTheme.typography.titleMedium)
                Text(explanation, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
            Text("Privacidade e tratamento de dados", style = MaterialTheme.typography.headlineSmall)
            LegalInfoContent.privacySections.forEach { (heading, explanation) ->
                Text(heading, style = MaterialTheme.typography.titleMedium)
                Text(explanation, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
            Text("Referências", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { urls.openUri(LegalInfoContent.legalReferenceUrl) }) {
                Text("Lei Geral de Proteção de Dados — texto oficial")
            }
            TextButton(onClick = { urls.openUri(LegalInfoContent.authorityReferenceUrl) }) {
                Text("Orientação da ANPD sobre crianças e adolescentes")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val urls = LocalUriHandler.current
    Scaffold(topBar = {
        TopAppBar(title = { Text("Sobre") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Sala de Leitura", style = MaterialTheme.typography.headlineMedium)
            Text("Criado por ${LegalInfoContent.creator}", style = MaterialTheme.typography.titleLarge)
            Text(LegalInfoContent.about, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            Text("Instagram: ${LegalInfoContent.instagramHandle}", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { urls.openUri(LegalInfoContent.instagramUrl) }, modifier = Modifier.fillMaxWidth()) {
                Text("Conhecer o Código em Aula no Instagram")
            }
            Text("O link abre o Instagram ou navegador somente quando você toca no botão. Nenhum cadastro ou histórico de leitura é compartilhado pelo aplicativo nessa ação.")
        }
    }
}
