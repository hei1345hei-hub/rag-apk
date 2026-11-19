package com.kyant.backdrop.catalog.interview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("login") } // "login" or "register"

    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("123456") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scrollState = rememberScrollState()

    // Auto dismiss error/toast
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000)
            errorMessage = null
        }
    }
    LaunchedEffect(successToast) {
        if (successToast != null) {
            delay(2500)
            successToast = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = statusBarTop + 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar with Close/Back & Title
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                        .border(1.dp, BorderColor, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("✕", style = TextStyle(color = TextInk, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }

                // Quick Theme Toggle on Login Screen
                val isDark = isAppInDarkTheme()
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { ThemeState.toggle() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        if (isDark) "🌙 夜间" else "☀️ 日间",
                        style = TextStyle(
                            color = if (isDark) TagAmber else TagIndigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 2. Brand Emblem & Headings
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131B23))
                    .border(2.5.dp, Color(0xFFE59819), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText("☕", style = TextStyle(fontSize = 24.sp))
                    BasicText(
                        "JAVA",
                        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            BasicText(
                "Java 面试系统",
                style = TextStyle(color = TextInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                "基于 RAG 的智能问答与全真模拟面试平台 · 移动端",
                style = TextStyle(color = TextMuted, fontSize = 12.sp)
            )

            Spacer(Modifier.height(24.dp))

            // 3. Tab Switch (登录 / 注册)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (mode == "login") PrimaryRed else Color.Transparent)
                            .clickable {
                                mode = "login"
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "账号登录",
                            style = TextStyle(
                                color = if (mode == "login") Color.White else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (mode == "login") FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (mode == "register") PrimaryRed else Color.Transparent)
                            .clickable {
                                mode = "register"
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "注册新账号",
                            style = TextStyle(
                                color = if (mode == "register") Color.White else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (mode == "register") FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 4. Quick Demo Accounts (Only for login mode)
            AnimatedVisibility(visible = mode == "login") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    BasicText(
                        "快捷填入演示账号（免打字点击即填）：",
                        style = TextStyle(color = TextDim, fontSize = 11.sp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    username = "admin"
                                    password = "123456"
                                    successToast = "已填入教研主管 (admin)"
                                }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "👑 教研主管 (admin)",
                                style = TextStyle(color = TagRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    username = "zhangsan"
                                    password = "123456"
                                    successToast = "已填入备考学员 (zhangsan)"
                                }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "🎓 备考学员 (zhangsan)",
                                style = TextStyle(color = TagIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 5. Input Fields Card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Username
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText("登录账号", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (username.isEmpty()) {
                            BasicText("请输入账号 (如 admin)", style = TextStyle(color = TextDim, fontSize = 13.sp))
                        }
                        BasicTextField(
                            value = username,
                            onValueChange = { username = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Password
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("登录密码", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        BasicText(
                            if (isPasswordVisible) "隐藏 🙈" else "显示 👁️",
                            style = TextStyle(color = TagIndigo, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.clickable { isPasswordVisible = !isPasswordVisible }
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (password.isEmpty()) {
                            BasicText("请输入密码 (至少 6 位)", style = TextStyle(color = TextDim, fontSize = 13.sp))
                        }
                        BasicTextField(
                            value = password,
                            onValueChange = { password = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 14.sp),
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Register Specific Fields
                if (mode == "register") {
                    // Confirm Password
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText("确认密码", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(InputBoxBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (confirmPassword.isEmpty()) {
                                BasicText("请再次输入密码", style = TextStyle(color = TextDim, fontSize = 13.sp))
                            }
                            BasicTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                textStyle = TextStyle(color = TextInk, fontSize = 14.sp),
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Nickname
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText("用户昵称 (选填)", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(InputBoxBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (nickname.isEmpty()) {
                                BasicText("例如: Java冲刺学员", style = TextStyle(color = TextDim, fontSize = 13.sp))
                            }
                            BasicTextField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                textStyle = TextStyle(color = TextInk, fontSize = 14.sp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Email
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText("电子邮箱 (选填)", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(InputBoxBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (email.isEmpty()) {
                                BasicText("用于接收测评报告与提醒", style = TextStyle(color = TextDim, fontSize = 13.sp))
                            }
                            BasicTextField(
                                value = email,
                                onValueChange = { email = it },
                                textStyle = TextStyle(color = TextInk, fontSize = 14.sp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Error or Success Banner
            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TagRedBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicText("⚠️ $errorMessage", style = TextStyle(color = TagRed, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
            }
            if (successToast != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TagTealBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicText("✅ $successToast", style = TextStyle(color = TagTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
            }

            Spacer(Modifier.height(20.dp))

            // 6. Submit Button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLoading) PrimaryRed.copy(alpha = 0.6f) else PrimaryRed)
                    .clickable(enabled = !isLoading) {
                        if (username.isBlank()) {
                            errorMessage = "请输入登录账号"
                            return@clickable
                        }
                        if (password.length < 6) {
                            errorMessage = "密码长度至少需要 6 位"
                            return@clickable
                        }
                        if (mode == "register") {
                            if (confirmPassword != password) {
                                errorMessage = "两次输入的密码不一致"
                                return@clickable
                            }
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                val (ok, msg) = InterviewApi.register(
                                    username = username.trim(),
                                    pass = password,
                                    nickname = nickname.trim(),
                                    email = email.trim()
                                )
                                isLoading = false
                                if (ok) {
                                    successToast = "注册成功！请直接登录"
                                    mode = "login"
                                } else {
                                    errorMessage = msg
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                val ok = InterviewApi.login(username.trim(), password)
                                isLoading = false
                                if (ok) {
                                    successToast = "登录成功！欢迎回来"
                                    onLoginSuccess()
                                } else {
                                    errorMessage = "账号或密码错误，请重试"
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    if (isLoading) (if (mode == "login") "正在登录..." else "正在注册...")
                    else (if (mode == "login") "登 录 系 统" else "立即注册账号"),
                    style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(14.dp))

            // 7. Guest Experience Link
            Box(
                Modifier
                    .clickable {
                        InterviewApi.currentUser = UserInfo(0, "guest", "未登录学员", "GUEST", "")
                        InterviewApi.isLoggedIn = false
                        onDismiss()
                    }
                    .padding(8.dp)
            ) {
                BasicText(
                    "暂不登录，以访客模式体验系统 ›",
                    style = TextStyle(color = TagIndigo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}
