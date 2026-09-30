package com.johngabie.johnpdf.data

import kotlinx.serialization.Serializable

@Serializable
enum class Origin(val label: String) {
    WHATSAPP("WhatsApp"),
    DOWNLOAD("Download"),
    DOCUMENTS("Documentos"),
    OTHER("Outros"),
}

fun originFromPath(path: String): Origin {
    val p = path.lowercase()
    return when {
        "whatsapp" in p -> Origin.WHATSAPP
        "/download/" in p || "/downloads/" in p -> Origin.DOWNLOAD
        "/documents/" in p || "/documentos/" in p -> Origin.DOCUMENTS
        else -> Origin.OTHER
    }
}

fun originFromAuthority(authority: String?): Origin {
    val a = authority?.lowercase() ?: return Origin.OTHER
    return when {
        "whatsapp" in a -> Origin.WHATSAPP
        "downloads" in a -> Origin.DOWNLOAD
        else -> Origin.OTHER
    }
}
