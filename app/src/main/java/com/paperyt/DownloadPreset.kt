package com.paperyt

enum class DownloadPreset(val label: String) {
    BEST_VIDEO_AUDIO("最高画質 + 最高音質"),
    AUDIO_ONLY("音声のみ"),
    VIDEO_ONLY("動画のみ");

    fun toArgs(extension: String): List<String> = when (this) {
        BEST_VIDEO_AUDIO -> listOf(
            "-f", "bv*+ba/b",
            "--merge-output-format", extension
        )

        AUDIO_ONLY -> listOf(
            "-x",
            "--audio-format", extension
        )

        VIDEO_ONLY -> listOf(
            "-f", "bv",
            "--recode-video", extension
        )
    }
}
