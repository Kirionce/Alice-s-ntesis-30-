package com.example.data.local

enum class ChatChannel(val id: String, val displayName: String, val subtitle: String) {
    ALICE("ALICE", "Alice", "Alice Synthesis Thirty"),
    YUI("YUI", "Yui", "Compañera de apoyo"),
    GROUP("GROUP", "Alice y Yui", "Chat grupal · Ambas presentes")
}

enum class MessageSpeaker(val id: String) {
    USER("USER"),
    ALICE("ALICE"),
    YUI("YUI"),
    SYSTEM("SYSTEM")
}
