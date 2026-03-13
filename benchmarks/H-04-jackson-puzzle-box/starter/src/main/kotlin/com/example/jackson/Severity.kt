package com.example.jackson

import com.fasterxml.jackson.annotation.JsonValue

enum class Severity(@JsonValue val value: String) {
    LOW("low"),
    WARNING("warning"),
    CRITICAL("critical");
}
