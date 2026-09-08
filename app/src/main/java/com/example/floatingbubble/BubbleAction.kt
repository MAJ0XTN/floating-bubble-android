package com.example.floatingbubble

enum class BubbleAction(
    val label: String,
    val emoji: String
) {
    SELECT_ALL("انتخاب همه", "▦"),
    COPY("کپی", "📋"),
    PASTE("پیست", "📥"),
    ENTER("اینتر", "⏎"),
    ALT("Alt", "⎇"),
    TAB("Tab", "⇥"),
    SHIFT("Shift", "⇧"),
    SCREENSHOT("عکس صفحه", "📸")
}
