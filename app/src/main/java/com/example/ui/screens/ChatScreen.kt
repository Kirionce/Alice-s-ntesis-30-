package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.CloudChatMessage
import com.example.data.local.ChatChannel
import com.example.data.local.MessageSpeaker
import com.example.ui.MainUiState
import com.example.ui.MessagesUiState
import com.example.ui.theme.AdminBubbleBg
import com.example.ui.theme.AdminBubbleBorder
import com.example.ui.theme.AliceGold
import com.example.ui.theme.AliceGoldBright
import com.example.ui.theme.AliceGoldContainer
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightOutline
import com.example.ui.theme.MidnightOutlineSoft
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceHigh
import com.example.ui.theme.OnAliceGold
import com.example.ui.theme.StarlightMuted
import com.example.ui.theme.StarlightSubtle
import com.example.ui.theme.StarlightWhite
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusCrimsonContainer
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldContainer
import com.example.ui.theme.YuiCyan
import com.example.ui.theme.YuiCyanDim
import com.example.ui.theme.YuiRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    uiState: MainUiState,
    messagesState: MessagesUiState,
    speakingMessageId: String?,
    onSelectChannel: (ChatChannel) -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onToggleSpeak: (CloudChatMessage) -> Unit,
    onToggleAutoVoice: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    val messages = (messagesState as? MessagesUiState.Success)?.messages.orEmpty()

    LaunchedEffect(messages.size, uiState.streamingPreviewText.length, uiState.selectedChannel) {
        val totalItems = messages.size + if (uiState.isSending) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightCanvas, MidnightAbyss)
                )
            )
    ) {
        ChatTopHeader(
            uiState = uiState,
            onSelectChannel = onSelectChannel,
            onToggleAutoVoice = onToggleAutoVoice,
            onClearChat = onClearChat
        )

        AnimatedVisibility(
            visible = uiState.banner != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.banner?.let { banner ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (banner.isError) StatusCrimsonContainer else StatusEmeraldContainer,
                    border = BorderStroke(
                        1.dp,
                        if (banner.isError) StatusCrimson.copy(alpha = 0.5f) else StatusEmerald.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (banner.isError) Icons.Rounded.WarningAmber else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (banner.isError) StatusCrimson else StatusEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = banner.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = StarlightWhite,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissBanner) {
                            Text("OK", color = StarlightMuted)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (messagesState) {
                is MessagesUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AliceGold, strokeWidth = 2.5.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Sincronizando conversaciones…",
                                style = MaterialTheme.typography.bodySmall,
                                color = StarlightMuted
                            )
                        }
                    }
                }

                is MessagesUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = StatusCrimsonContainer,
                            border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = StatusCrimson,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = messagesState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StarlightWhite
                                )
                            }
                        }
                    }
                }

                is MessagesUiState.Success -> {
                    if (messages.isEmpty() && !uiState.isSending) {
                        EmptyChannelWelcome(
                            channel = uiState.selectedChannel,
                            affectionateMode = uiState.affectionateMode,
                            modelDisplayName = uiState.selectedModel.displayName,
                            onQuickPrompt = { prompt, encourageDialogue ->
                                onSendMessage(prompt, encourageDialogue)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("chat_messages_list"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages, key = { it.id.ifBlank { it.sequenceOrder.toString() } }) { msg ->
                                ChatMessageBubble(
                                    message = msg,
                                    isSpeaking = speakingMessageId == msg.id,
                                    onToggleSpeak = { onToggleSpeak(msg) },
                                    onCopy = { clipboard.setText(AnnotatedString(msg.content)) }
                                )
                            }

                            if (uiState.isSending) {
                                item(key = "streaming_or_typing") {
                                    if (uiState.streamingPreviewText.isNotBlank()) {
                                        StreamingBubble(
                                            channel = uiState.selectedChannel,
                                            text = uiState.streamingPreviewText
                                        )
                                    } else {
                                        TypingIndicatorRow(channel = uiState.selectedChannel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.selectedChannel == ChatChannel.GROUP && messages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MidnightSurfaceHigh,
                    border = BorderStroke(1.dp, YuiCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = !uiState.isSending) {
                            val topic = inputText.trim().ifEmpty {
                                "Conversen un momento entre ustedes y denme su opinión conjunta para ayudarme hoy, Administrador Kirionce aquí."
                            }
                            inputText = ""
                            onSendMessage(topic, true)
                        }
                        .testTag("group_dialogue_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forum,
                            contentDescription = null,
                            tint = YuiCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Conversar entre ellas",
                            style = MaterialTheme.typography.labelMedium,
                            color = YuiCyan
                        )
                    }
                }
            }
        }

        ChatComposer(
            channel = uiState.selectedChannel,
            inputText = inputText,
            onInputChange = { inputText = it },
            isSending = uiState.isSending,
            affectionateMode = uiState.affectionateMode,
            onSend = {
                val toSend = inputText.trim()
                if (toSend.isNotEmpty()) {
                    inputText = ""
                    onSendMessage(toSend, false)
                }
            }
        )
    }
}

@Composable
private fun ChatTopHeader(
    uiState: MainUiState,
    onSelectChannel: (ChatChannel) -> Unit,
    onToggleAutoVoice: (Boolean) -> Unit,
    onClearChat: () -> Unit
) {
    Surface(
        color = MidnightCanvas.copy(alpha = 0.96f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = AliceGoldBright
                    )
                    Text(
                        text = "${stringResource(R.string.admin_title)} · ${uiState.selectedModel.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StarlightMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleAutoVoice(!uiState.autoVoiceReply) },
                    modifier = Modifier.testTag("toggle_auto_voice_button")
                ) {
                    Icon(
                        imageVector = if (uiState.autoVoiceReply) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        contentDescription = if (uiState.autoVoiceReply) "Voz automática activada" else "Voz automática desactivada",
                        tint = if (uiState.autoVoiceReply) YuiCyan else StarlightSubtle
                    )
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.clear_chat_cd),
                        tint = StarlightMuted
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChannelPill(
                    channel = ChatChannel.ALICE,
                    selected = uiState.selectedChannel == ChatChannel.ALICE,
                    accentColor = AliceGold,
                    onClick = { onSelectChannel(ChatChannel.ALICE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("channel_pill_alice")
                )
                ChannelPill(
                    channel = ChatChannel.YUI,
                    selected = uiState.selectedChannel == ChatChannel.YUI,
                    accentColor = YuiCyan,
                    onClick = { onSelectChannel(ChatChannel.YUI) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("channel_pill_yui")
                )
                ChannelPill(
                    channel = ChatChannel.GROUP,
                    selected = uiState.selectedChannel == ChatChannel.GROUP,
                    accentColor = YuiRose,
                    onClick = { onSelectChannel(ChatChannel.GROUP) },
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("channel_pill_group")
                )
            }
        }
    }
}

@Composable
private fun ChannelPill(
    channel: ChatChannel,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else MidnightSurface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else MidnightOutlineSoft
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (channel) {
                ChatChannel.ALICE -> {
                    Image(
                        painter = painterResource(R.drawable.img_avatar_alice_1795075355068),
                        contentDescription = stringResource(R.string.alice_avatar_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, AliceGold, CircleShape)
                    )
                }
                ChatChannel.YUI -> {
                    Image(
                        painter = painterResource(R.drawable.img_avatar_yui_1795075354455),
                        contentDescription = stringResource(R.string.yui_avatar_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, YuiCyan, CircleShape)
                    )
                }
                ChatChannel.GROUP -> {
                    Box(modifier = Modifier.width(34.dp).height(24.dp)) {
                        Image(
                            painter = painterResource(R.drawable.img_avatar_alice_1795075355068),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.CenterStart)
                                .clip(CircleShape)
                                .border(1.dp, AliceGold, CircleShape)
                        )
                        Image(
                            painter = painterResource(R.drawable.img_avatar_yui_1795075354455),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.CenterEnd)
                                .clip(CircleShape)
                                .border(1.dp, YuiCyan, CircleShape)
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = when (channel) {
                    ChatChannel.ALICE -> "Alice"
                    ChatChannel.YUI -> "Yui"
                    ChatChannel.GROUP -> "Ambas"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accentColor else StarlightMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyChannelWelcome(
    channel: ChatChannel,
    affectionateMode: Boolean,
    modelDisplayName: String,
    onQuickPrompt: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPrompts = when (channel) {
        ChatChannel.ALICE -> listOf(
            "Alice, organízame el plan de tareas de hoy" to false,
            "Recomiéndame música épica y videojuegos para esta noche" to false,
            "Cuéntame sobre tu origen en el mundo virtual" to false
        )
        ChatChannel.YUI -> listOf(
            "Yui, necesito un poco de ánimo hoy" to false,
            "Ayúdame a elegir una idea creativa para un proyecto" to false,
            "¿Qué te gustaría explorar conmigo en el mundo virtual?" to false
        )
        ChatChannel.GROUP -> listOf(
            "Alice y Yui, conversen entre ustedes y recomiéndenme un videojuego" to true,
            "Salúdenme juntas y ayúdenme a planificar mi semana" to false,
            "Hablen entre ustedes sobre tecnología y música para inspirarme" to true
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, MidnightOutline, RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.img_banner_virtual_world_1795075355049),
                    contentDescription = stringResource(R.string.virtual_world_banner_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MidnightAbyss.copy(alpha = 0.88f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (channel == ChatChannel.ALICE || channel == ChatChannel.GROUP) {
                        Image(
                            painter = painterResource(R.drawable.img_avatar_alice_1795075355068),
                            contentDescription = stringResource(R.string.alice_avatar_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(2.dp, AliceGold, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (channel == ChatChannel.YUI || channel == ChatChannel.GROUP) {
                        Image(
                            painter = painterResource(R.drawable.img_avatar_yui_1795075354455),
                            contentDescription = stringResource(R.string.yui_avatar_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(2.dp, YuiCyan, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = when (channel) {
                                ChatChannel.ALICE -> "Alice Synthesis Thirty"
                                ChatChannel.YUI -> "Yui"
                                ChatChannel.GROUP -> "Alice y Yui"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = StarlightWhite
                        )
                        Text(
                            text = buildString {
                                append("Listas para ti, Administrador Kirionce")
                                if (affectionateMode) append(" · Modo cariñosa")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AliceGoldBright
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MidnightSurface,
                border = BorderStroke(1.dp, MidnightOutlineSoft)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = when (channel) {
                            ChatChannel.ALICE -> "«A tus órdenes, Administrador Kirionce. Estoy lista para asistirte con tareas, tecnología, música, videojuegos o cualquier proyecto que emprendamos hoy.»"
                            ChatChannel.YUI -> "«¡Hola, Administrador Kirionce! Qué alegría verte por aquí. Cuéntame en qué puedo acompañarte o animarte hoy.»"
                            ChatChannel.GROUP -> "«Ambas estamos aquí contigo, Administrador Kirionce. Puedes pedirnos ayuda en conjunto o indicarnos un tema para conversar entre nosotras.»"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightWhite.copy(alpha = 0.92f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = StatusEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$modelDisplayName · Sincronizado con Cloud Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusEmerald
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Sugerencias rápidas",
                style = MaterialTheme.typography.labelLarge,
                color = StarlightMuted
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickPrompts) { (prompt, dialogue) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MidnightSurfaceHigh,
                        border = BorderStroke(1.dp, MidnightOutline),
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onQuickPrompt(prompt, dialogue) }
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = StarlightWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: CloudChatMessage,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.speaker == MessageSpeaker.USER.id
    val isAlice = message.speaker == MessageSpeaker.ALICE.id
    val isYui = message.speaker == MessageSpeaker.YUI.id
    val isSystem = message.speaker == MessageSpeaker.SYSTEM.id
    val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.displayTimestampMillis()))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (isAlice || isYui) {
            Image(
                painter = painterResource(
                    if (isAlice) R.drawable.img_avatar_alice_1795075355068
                    else R.drawable.img_avatar_yui_1795075354455
                ),
                contentDescription = if (isAlice) stringResource(R.string.alice_avatar_cd) else stringResource(R.string.yui_avatar_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = if (isAlice) AliceGold else YuiCyan,
                        shape = CircleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
        }

        val bgColor = when {
            isUser -> AdminBubbleBg
            message.errorFlag || isSystem -> StatusCrimsonContainer
            isAlice -> MidnightSurface
            isYui -> MidnightSurfaceHigh
            else -> MidnightSurface
        }
        val borderColor = when {
            isUser -> AdminBubbleBorder
            message.errorFlag || isSystem -> StatusCrimson.copy(alpha = 0.6f)
            isAlice -> AliceGold.copy(alpha = 0.45f)
            isYui -> YuiCyan.copy(alpha = 0.45f)
            else -> MidnightOutline
        }

        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 6.dp,
                topEnd = if (isUser) 6.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = bgColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                isAlice -> "Alice Synthesis Thirty"
                                isYui -> "Yui"
                                else -> "Aviso del Sistema"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                isAlice -> AliceGoldBright
                                isYui -> YuiCyan
                                else -> StatusCrimson
                            }
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = StarlightSubtle
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarlightWhite
                )

                if (isUser) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = StarlightMuted.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.End)
                    )
                } else if (isAlice || isYui) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSpeaking) (if (isAlice) AliceGoldContainer else YuiCyanDim) else MidnightCanvas,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onToggleSpeak)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = if (isSpeaking) stringResource(R.string.stop_voice_cd) else stringResource(R.string.play_voice_cd),
                                    tint = if (isAlice) AliceGold else YuiCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeaking) "Detener" else "Escuchar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isAlice) AliceGoldBright else YuiCyan
                                )
                            }
                        }

                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.copy_message_cd),
                                tint = StarlightSubtle,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(
    channel: ChatChannel,
    text: String
) {
    val accentColor = when (channel) {
        ChatChannel.ALICE -> AliceGold
        ChatChannel.YUI -> YuiCyan
        ChatChannel.GROUP -> YuiRose
    }
    Surface(
        modifier = Modifier.widthIn(max = 310.dp),
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
        color = MidnightSurface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.8.dp,
                    color = accentColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (channel) {
                        ChatChannel.ALICE -> "Alice Synthesis Thirty"
                        ChatChannel.YUI -> "Yui"
                        ChatChannel.GROUP -> "Alice y Yui"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = StarlightWhite
            )
        }
    }
}

@Composable
private fun TypingIndicatorRow(channel: ChatChannel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = if (channel == ChatChannel.YUI) YuiCyan else AliceGold
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = when (channel) {
                ChatChannel.ALICE -> "Alice está redactando su respuesta…"
                ChatChannel.YUI -> "Yui está pensando con cariño…"
                ChatChannel.GROUP -> "Alice y Yui están respondiendo…"
            },
            style = MaterialTheme.typography.bodySmall,
            color = StarlightMuted
        )
    }
}

@Composable
private fun ChatComposer(
    channel: ChatChannel,
    inputText: String,
    onInputChange: (String) -> Unit,
    isSending: Boolean,
    affectionateMode: Boolean,
    onSend: () -> Unit
) {
    Surface(
        color = MidnightCanvas,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    placeholder = {
                        Text(
                            text = when (channel) {
                                ChatChannel.ALICE -> stringResource(R.string.input_placeholder_alice)
                                ChatChannel.YUI -> stringResource(R.string.input_placeholder_yui)
                                ChatChannel.GROUP -> stringResource(R.string.input_placeholder_group)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = StarlightSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = if (affectionateMode) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = stringResource(R.string.affectionate_mode),
                                tint = YuiRose.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface,
                        focusedBorderColor = if (channel == ChatChannel.YUI) YuiCyan else AliceGold,
                        unfocusedBorderColor = MidnightOutlineSoft,
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite,
                        cursorColor = AliceGold
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = if (inputText.isNotBlank() && !isSending) AliceGold else MidnightSurfaceHigh,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .clickable(enabled = inputText.isNotBlank() && !isSending, onClick = onSend)
                        .testTag("send_message_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AliceGold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.send_message_cd),
                                tint = if (inputText.isNotBlank()) OnAliceGold else StarlightSubtle,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
