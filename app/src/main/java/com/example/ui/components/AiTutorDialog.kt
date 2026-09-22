package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.service.ChatMessage
import com.example.data.service.GeminiAiService
import com.example.data.service.MessageSender
import kotlinx.coroutines.launch

@Composable
fun AiTutorDialog(
    initialSubject: String = "수학",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("ai_tutor_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            AiTutorChatPanel(
                initialSubject = initialSubject,
                showCloseButton = true,
                onClose = onDismiss
            )
        }
    }
}

@Composable
fun AiTutorScreenContent(
    initialSubject: String = "수학",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_tutor_screen_content"),
        color = MaterialTheme.colorScheme.background
    ) {
        AiTutorChatPanel(
            initialSubject = initialSubject,
            showCloseButton = false,
            onClose = {}
        )
    }
}

@Composable
fun AiTutorChatPanel(
    initialSubject: String = "수학",
    showCloseButton: Boolean = false,
    onClose: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { GeminiAiService() }
    val listState = rememberLazyListState()

    var selectedSubject by remember { mutableStateOf(initialSubject) }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = MessageSender.AI_TUTOR,
                text = "반가워요! 1:1 AI 학습 튜터입니다 🧑‍🏫\n" +
                        "공부하다가 이해가 잘 안 되는 개념이나 문제 풀이 원리가 있다면 무엇이든 편하게 물어보세요. 단계별로 친절하게 도와드릴게요!"
            )
        )
    }

    val quickQuestions = listOf(
        "이차함수 꼭짓점 공식 쉽게 외우는 법",
        "관계대명사 who vs which 구분 팁",
        "광합성과 호흡의 차이점 한 줄 요약",
        "오늘 공부 집중이 잘 안 되는데 조언해 줘"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = if (showCloseButton) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) else RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI 1:1 맞춤 학습 튜터",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Gemini AI 실시간 질문 답변",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                if (showCloseButton) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Subject tag selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "질문 과목:", style = MaterialTheme.typography.labelMedium)
            listOf("수학", "영어", "과학", "국어", "학습고민").forEach { sub ->
                FilterChip(
                    selected = selectedSubject == sub,
                    onClick = { selectedSubject = sub },
                    label = { Text(sub, fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

        // Chat message list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }

            if (isGenerating) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 튜터가 맞춤 설명을 정리하고 있어요...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick recommendation prompt chips
        if (messages.size <= 2) {
            Text(
                text = "💡 이런 질문을 해보세요:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickQuestions.take(2).forEach { quickPrompt ->
                    AssistChip(
                        onClick = {
                            inputText = quickPrompt
                        },
                        label = { Text(quickPrompt, fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Input bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = if (showCloseButton) RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) else RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("개념이나 문제를 질문해 보세요...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_tutor_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            val userText = inputText.trim()
                            inputText = ""
                            messages.add(ChatMessage(sender = MessageSender.USER, text = userText))

                            coroutineScope.launch {
                                isGenerating = true
                                listState.animateScrollToItem(messages.size)
                                val answer = aiService.askTutor(
                                    userQuestion = userText,
                                    subjectContext = selectedSubject,
                                    recentHistory = messages
                                )
                                messages.add(ChatMessage(sender = MessageSender.AI_TUTOR, text = answer))
                                isGenerating = false
                                listState.animateScrollToItem(messages.size)
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isGenerating)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("ai_tutor_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "전송",
                        tint = if (inputText.isNotBlank() && !isGenerating) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
