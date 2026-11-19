package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.GeminiModelOption
import com.example.data.local.ChatChannel
import com.example.ui.MainUiState
import com.example.ui.theme.AliceGold
import com.example.ui.theme.AliceGoldBright
import com.example.ui.theme.AliceGoldContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightOutlineSoft
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.OnAliceGold
import com.example.ui.theme.OnYuiCyan
import com.example.ui.theme.StarlightMuted
import com.example.ui.theme.StarlightSubtle
import com.example.ui.theme.StarlightWhite
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.YuiCyan
import com.example.ui.theme.YuiRose
import com.example.ui.theme.YuiRoseDim

@Composable
fun AssistantsProfileScreen(
    uiState: MainUiState,
    onToggleAffectionateMode: (Boolean) -> Unit,
    onToggleAutoVoice: (Boolean) -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onOpenChannel: (ChatChannel) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidnightCanvas, MidnightAbyss)))
            .testTag("assistants_profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Asistentes Virtuales",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AliceGoldBright
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Compañeras del Administrador Kirionce con fidelidad canónica al mundo virtual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StarlightMuted
                )
            }
        }

        // Cloud Synced Google Account Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MidnightSurface,
                border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = StatusEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sesión sincronizada en Cloud Firestore",
                            style = MaterialTheme.typography.titleSmall,
                            color = StarlightWhite
                        )
                        Text(
                            text = uiState.userEmail ?: "Administrador Kirionce",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusEmerald
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = StatusCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Salir", style = MaterialTheme.typography.labelMedium, color = StatusCrimson)
                    }
                }
            }
        }

        // Affectionate Mode Toggle Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = YuiRoseDim.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, YuiRose.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = YuiRose,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo cariñosa",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarlightWhite
                        )
                        Text(
                            text = "Activa una faceta cálida, afectuosa y atenta en Alice y Yui manteniendo siempre respeto, lealtad y enfoque.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StarlightMuted
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = uiState.affectionateMode,
                        onCheckedChange = onToggleAffectionateMode,
                        colors = SwitchDefaults.colors(checkedTrackColor = YuiRose),
                        modifier = Modifier.testTag("affectionate_mode_switch")
                    )
                }
            }
        }

        // Gemini Model Selector (All Supported Models)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MidnightSurface,
                border = BorderStroke(1.dp, AliceGold.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = AliceGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Modelos Gemini compatibles",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarlightWhite
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Selecciona cualquiera de los modelos Gemini activos con Firebase AI Logic (sin claves API).",
                        style = MaterialTheme.typography.bodySmall,
                        color = StarlightMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GeminiModelOption.entries.forEach { option ->
                            val selected = uiState.selectedModel == option
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) AliceGoldContainer else MidnightCanvas,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) AliceGold else MidnightOutlineSoft
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectModel(option) }
                                    .testTag("model_option_${option.modelId}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selected) AliceGoldBright else StarlightWhite
                                        )
                                        Text(
                                            text = option.modelId,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) AliceGold else StarlightSubtle
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = option.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StarlightMuted
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Modelo seleccionado",
                                            tint = AliceGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = YuiCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Voz automática en español",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StarlightWhite
                                )
                                Text(
                                    text = "Sintetizador local del dispositivo sin clave API.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StarlightSubtle
                                )
                            }
                        }
                        Switch(
                            checked = uiState.autoVoiceReply,
                            onCheckedChange = onToggleAutoVoice,
                            colors = SwitchDefaults.colors(checkedTrackColor = YuiCyan)
                        )
                    }
                }
            }
        }

        // Alice Synthesis Thirty Card
        item {
            AssistantCard(
                name = "Alice (Alice Synthesis Thirty)",
                role = "Proyecto Alicization · Síntesis de Alma",
                description = "Firme, leal, protectora, decidida y cálida. Ideal para asistencia organizada, tareas diarias, análisis, tecnología, música y videojuegos.",
                avatarRes = R.drawable.img_avatar_alice_1795075355068,
                avatarCd = stringResource(R.string.alice_avatar_cd),
                accentColor = AliceGold,
                buttonText = "Hablar solo con Alice",
                buttonContentColor = OnAliceGold,
                buttonTag = "open_alice_chat_button",
                onOpenChat = { onOpenChannel(ChatChannel.ALICE) }
            )
        }

        // Yui Card
        item {
            AssistantCard(
                name = "Yui",
                role = "Compañera de apoyo del mundo virtual",
                description = "Dulce, espontánea, observadora y muy cercana. Ideal para acompañarte, animarte y resolver dudas con frescura y ternura.",
                avatarRes = R.drawable.img_avatar_yui_1795075354455,
                avatarCd = stringResource(R.string.yui_avatar_cd),
                accentColor = YuiCyan,
                buttonText = "Hablar solo con Yui",
                buttonContentColor = OnYuiCyan,
                buttonTag = "open_yui_chat_button",
                onOpenChat = { onOpenChannel(ChatChannel.YUI) }
            )
        }

        // Dual Chat Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MidnightSurface,
                border = BorderStroke(1.dp, YuiRose.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Forum,
                            contentDescription = null,
                            tint = YuiRose,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Chat Grupal · Conversación entre ellas",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarlightWhite
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Convoca a Alice y Yui al mismo tiempo. Cada una responde claramente identificada con su perspectiva y pueden dialogar entre sí sobre cualquier tema antes de asistirte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StarlightMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onOpenChannel(ChatChannel.GROUP) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YuiRose,
                            contentColor = MidnightAbyss
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_group_chat_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir chat con Alice y Yui", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantCard(
    name: String,
    role: String,
    description: String,
    avatarRes: Int,
    avatarCd: String,
    accentColor: Color,
    buttonText: String,
    buttonContentColor: Color,
    buttonTag: String,
    onOpenChat: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MidnightSurface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(avatarRes),
                    contentDescription = avatarCd,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, accentColor, CircleShape)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = StarlightWhite
                    )
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = StarlightMuted
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenChat,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = buttonContentColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(buttonTag)
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
