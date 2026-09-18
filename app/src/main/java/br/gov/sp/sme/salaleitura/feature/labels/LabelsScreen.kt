@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.labels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
@Composable fun LabelsScreen(onBack:()->Unit){var code by remember{mutableStateOf("")};var type by remember{mutableStateOf("COPY")};val bitmap=remember(code,type){if(code.isBlank())null else LabelGenerator.qrBitmap(if(type=="COPY")LabelGenerator.copyPayload(code.trim()) else LabelGenerator.personPayload(code.trim()),480)};Scaffold(topBar={TopAppBar(title={Text("Etiquetas / QR")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(type=="COPY",{type="COPY"},label={Text("Exemplar")});FilterChip(type=="PERSON",{type="PERSON"},label={Text("Pessoa")})};OutlinedTextField(code,{code=it},label={Text("Código interno")},modifier=Modifier.fillMaxWidth());bitmap?.let{Image(it.asImageBitmap(),contentDescription="QR Code",modifier=Modifier.fillMaxWidth().aspectRatio(1f));Text(if(type=="COPY")LabelGenerator.copyPayload(code.trim()) else LabelGenerator.personPayload(code.trim()))}}}}
