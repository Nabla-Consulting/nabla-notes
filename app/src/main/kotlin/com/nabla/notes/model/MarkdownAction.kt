package com.nabla.notes.model

enum class MarkdownAction(
    val label: String,
    val prefix: String,
    val suffix: String = "",
    val isBlock: Boolean = false,       // insert at line start, not at cursor
    val placeholder: String = ""        // text to insert between prefix/suffix when no selection
) {
    UNDO(          "\u21A9", "",            ""),
    REDO(          "\u21AA", "",            ""),
    BOLD(          "B",    "**",          "**",      placeholder = "bold"),
    ITALIC(        "I",    "*",           "*",       placeholder = "italic"),
    STRIKETHROUGH( "S\u0336",   "~~",     "~~",      placeholder = "text"),
    CODE_BLOCK(    "```",  "```\n",       "\n```",   placeholder = "code here"),
    H1(            "H1",   "# ",          "",        isBlock = true),
    H2(            "H2",   "## ",         "",        isBlock = true),
    H3(            "H3",   "### ",        "",        isBlock = true),
    BLOCKQUOTE(    ">",    "> ",          "",        isBlock = true),
    BULLET(        "\u2022",    "- ",     "",        isBlock = true),
    NUMBERED(      "1.",   "1. ",         "",        isBlock = true),
    TASK(          "\u2610",    "[ ] ",   "",        isBlock = true, placeholder = ""),
    HR(            "\u2014",    "\n---\n",""),
    LINK(          "\uD83D\uDD17",  "[",  "](url)",  placeholder = "text"),
    IMAGE(         "\uD83D\uDDBC",  "![", "](url)",  placeholder = "alt"),
    TABLE(         "\u229E",    "\n| Col1 | Col2 | Col3 |\n|------|------|------|\n|      |      |      |\n|      |      |      |\n|      |      |      |\n", ""),
}
