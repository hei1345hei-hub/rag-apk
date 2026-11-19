package com.kyant.backdrop.catalog.interview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// 1. Home Icon
private var _HomeIcon: ImageVector? = null
val HomeIcon: ImageVector
    get() {
        if (_HomeIcon != null) return _HomeIcon!!
        _HomeIcon = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.7f, 2.3f)
                quadToRelative(0.7f, -0.5f, 1.3f, -0.5f)
                quadToRelative(0.6f, 0f, 1.3f, 0.5f)
                lineToRelative(7.2f, 5.5f)
                quadToRelative(0.5f, 0.4f, 0.8f, 1.6f)
                verticalLineToRelative(10.1f)
                quadToRelative(0f, 1.4f, -2.5f, 2.5f)
                horizontalLineToRelative(-3.2f)
                quadToRelative(-0.8f, 0f, -1.5f, -1.5f)
                verticalLineToRelative(-4.5f)
                quadToRelative(0f, -0.6f, -1.0f, -1.0f)
                horizontalLineToRelative(-2.0f)
                quadToRelative(-0.6f, 0f, -1.0f, 1.0f)
                verticalLineToRelative(4.5f)
                quadToRelative(0f, 0.8f, -1.5f, 1.5f)
                horizontalLineTo(5.2f)
                quadToRelative(-1.4f, 0f, -2.5f, -2.5f)
                verticalLineTo(9.4f)
                quadToRelative(0f, -0.6f, 0.8f, -1.6f)
                close()
            }
        }.build()
        return _HomeIcon!!
    }

// 2. Chat / Q&A Icon
private var _ChatIcon: ImageVector? = null
val ChatIcon: ImageVector
    get() {
        if (_ChatIcon != null) return _ChatIcon!!
        _ChatIcon = ImageVector.Builder(
            name = "Chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 4f)
                horizontalLineTo(4f)
                quadToRelative(-1.1f, 0f, -2f, 1.1f)
                verticalLineToRelative(10f)
                quadToRelative(0f, 1.1f, 2f, 2f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(3.5f)
                quadToRelative(0f, 0.4f, 0.8f, 0.4f)
                lineTo(12.3f, 18f)
                horizontalLineTo(20f)
                quadToRelative(1.1f, 0f, 2f, -1.1f)
                verticalLineTo(6f)
                quadToRelative(0f, -1.1f, -2f, -2f)
                close()
                // Three dots
                moveTo(7.5f, 11f)
                quadToRelative(0f, -0.8f, 0.8f, -0.8f)
                quadToRelative(0.8f, 0f, 0.8f, 0.8f)
                quadToRelative(0f, 0.8f, -0.8f, 0.8f)
                quadToRelative(-0.8f, 0f, -0.8f, -0.8f)
                close()
                moveTo(11.5f, 11f)
                quadToRelative(0f, -0.8f, 0.8f, -0.8f)
                quadToRelative(0.8f, 0f, 0.8f, 0.8f)
                quadToRelative(0f, 0.8f, -0.8f, 0.8f)
                quadToRelative(-0.8f, 0f, -0.8f, -0.8f)
                close()
                moveTo(15.5f, 11f)
                quadToRelative(0f, -0.8f, 0.8f, -0.8f)
                quadToRelative(0.8f, 0f, 0.8f, 0.8f)
                quadToRelative(0f, 0.8f, -0.8f, 0.8f)
                quadToRelative(-0.8f, 0f, -0.8f, -0.8f)
                close()
            }
        }.build()
        return _ChatIcon!!
    }

// 3. Interview / Mic Icon
private var _InterviewIcon: ImageVector? = null
val InterviewIcon: ImageVector
    get() {
        if (_InterviewIcon != null) return _InterviewIcon!!
        _InterviewIcon = ImageVector.Builder(
            name = "Interview",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 14f)
                quadToRelative(1.7f, 0f, 3f, -1.3f)
                verticalLineTo(5f)
                quadToRelative(0f, -1.7f, -3f, -3f)
                reflectiveQuadToRelative(-3f, 3f)
                verticalLineToRelative(6f)
                quadToRelative(0f, 1.7f, 3f, 3f)
                close()
                moveTo(17.3f, 11f)
                quadToRelative(0f, 2.9f, -5.3f, 5.3f)
                reflectiveQuadToRelative(-5.3f, -5.3f)
                horizontalLineTo(5f)
                quadToRelative(0f, 3.5f, 6f, 6.9f)
                verticalLineTo(21f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-3.1f)
                quadToRelative(3.4f, -0.5f, 6f, -6.9f)
                close()
            }
        }.build()
        return _InterviewIcon!!
    }

// 4. Question / Book Icon
private var _QuestionIcon: ImageVector? = null
val QuestionIcon: ImageVector
    get() {
        if (_QuestionIcon != null) return _QuestionIcon!!
        _QuestionIcon = ImageVector.Builder(
            name = "Question",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 2f)
                horizontalLineTo(6f)
                quadToRelative(-1.1f, 0f, -2f, 1.1f)
                verticalLineToRelative(16f)
                quadToRelative(0f, 1.1f, 2f, 2f)
                horizontalLineToRelative(12f)
                quadToRelative(1.1f, 0f, 2f, -1.1f)
                verticalLineTo(4f)
                quadToRelative(0f, -1.1f, -2f, -2f)
                close()
                moveTo(8f, 6f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                close()
                moveTo(8f, 10f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                close()
                moveTo(8f, 14f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                close()
            }
        }.build()
        return _QuestionIcon!!
    }

// 5. User / Profile Icon
private var _UserIcon: ImageVector? = null
val UserIcon: ImageVector
    get() {
        if (_UserIcon != null) return _UserIcon!!
        _UserIcon = ImageVector.Builder(
            name = "User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 12f)
                quadToRelative(2.7f, 0f, 4.8f, -2.1f)
                reflectiveQuadToRelative(2.1f, -4.8f)
                reflectiveQuadToRelative(-2.1f, -4.8f)
                reflectiveQuadToRelative(-4.8f, 2.1f)
                reflectiveQuadToRelative(-2.1f, 4.8f)
                reflectiveQuadToRelative(2.1f, 4.8f)
                close()
                moveTo(12f, 14.4f)
                quadToRelative(-3.2f, 0f, -9.6f, 1.6f)
                verticalLineTo(21f)
                horizontalLineToRelative(19.2f)
                verticalLineToRelative(-5.0f)
                quadToRelative(-6.4f, -1.6f, -9.6f, -1.6f)
                close()
            }
        }.build()
        return _UserIcon!!
    }
