package com.johngabie.johnpdf.data

import androidx.annotation.StringRes
import com.johngabie.johnpdf.R
import kotlinx.serialization.Serializable

/**
 * Where a file came from. The enum name is what gets serialized, so renaming a constant
 * breaks stored recents — the label is only for display and lives in strings.xml.
 */
@Serializable
enum class Origin(@StringRes val labelRes: Int) {
    WHATSAPP(R.string.origin_whatsapp),
    DOWNLOAD(R.string.origin_download),
    DOCUMENTS(R.string.origin_documents),
    OTHER(R.string.origin_other),
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
