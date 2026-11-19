package com.kyant.backdrop.catalog.interview

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs

@Composable
fun InterviewApp() {
    val coroutineScope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme()
    val isLightTheme = !isDark
    val contentColor = if (isLightTheme) Color(0xFF56646F) else Color(0xFFCBD5E1)
    val backdrop = rememberLayerBackdrop()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    fun switchTab(targetIndex: Int) {
        val target = targetIndex.coerceIn(0, 4)
        if (selectedTabIndex == target && pagerState.currentPage == target) return
        selectedTabIndex = target
        scrollJob?.cancel()
        if (pagerState.currentPage != target) {
            isProgrammaticScroll = true
            scrollJob = coroutineScope.launch {
                try {
                    if (kotlin.math.abs(pagerState.currentPage - target) <= 1) {
                        pagerState.animateScrollToPage(target)
                    } else {
                        pagerState.scrollToPage(target)
                    }
                } finally {
                    isProgrammaticScroll = false
                }
            }
        }
    }

    // Sync bottom tab when user manually swipes the pager across pages
    LaunchedEffect(pagerState) {
        snapshotFlow {
            if (pagerState.isScrollInProgress) {
                pagerState.targetPage
            } else {
                pagerState.currentPage
            }
        }.collect { target ->
            if (!isProgrammaticScroll && selectedTabIndex != target) {
                selectedTabIndex = target
            }
        }
    }

    val homeIconPainter = rememberVectorPainter(HomeIcon)
    val chatIconPainter = rememberVectorPainter(ChatIcon)
    val interviewIconPainter = rememberVectorPainter(InterviewIcon)
    val questionIconPainter = rememberVectorPainter(QuestionIcon)
    val userIconPainter = rememberVectorPainter(UserIcon)

    val tabPainters = listOf(
        homeIconPainter,
        chatIconPainter,
        interviewIconPainter,
        questionIconPainter,
        userIconPainter
    )
    val tabNames = listOf("首页", "AI问答", "模拟面试", "题库精选", "个人中心")

    Box(
        Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // 1. Native Compose Screen Content with horizontal swipeable pages (connected to backdrop graphics layer)
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
                beyondViewportPageCount = 4
            ) { page ->
                when (page) {
                    0 -> DashboardScreen(onNavigateToTab = { switchTab(it) })
                    1 -> ChatScreen()
                    2 -> InterviewScreen()
                    3 -> QuestionScreen()
                    4 -> UserScreen(onNavigateToTab = { switchTab(it) })
                }
            }
        }

        // 2. Official Kyant LiquidBottomTabs docked at the bottom
        LiquidBottomTabs(
            selectedTabIndex = { selectedTabIndex },
            onTabSelected = { switchTab(it) },
            backdrop = backdrop,
            tabsCount = 5,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val iconColorFilter = ColorFilter.tint(contentColor)
            repeat(5) { index ->
                LiquidBottomTab({ switchTab(index) }) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .paint(
                                painter = tabPainters[index],
                                colorFilter = iconColorFilter
                            )
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        tabNames[index],
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // 3. Login / Register Fullscreen Modal with smooth slide & fade animation
        androidx.compose.animation.AnimatedVisibility(
            visible = InterviewApi.showLoginModal,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            LoginScreen(
                onDismiss = { InterviewApi.showLoginModal = false },
                onLoginSuccess = { InterviewApi.showLoginModal = false }
            )
        }

        // 4. Knowledge Base & Document Parsing Fullscreen Screen with smooth slide & fade animation
        androidx.compose.animation.AnimatedVisibility(
            visible = InterviewApi.showKnowledgeScreen,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            KnowledgeScreen(
                onDismiss = { InterviewApi.showKnowledgeScreen = false }
            )
        }
    }
}
