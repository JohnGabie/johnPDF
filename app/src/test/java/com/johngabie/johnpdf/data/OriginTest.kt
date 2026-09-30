package com.johngabie.johnpdf.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OriginTest {
    @Test fun whatsapp_path() = assertEquals(Origin.WHATSAPP,
        originFromPath("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents/a.pdf"))
    @Test fun download_path() = assertEquals(Origin.DOWNLOAD, originFromPath("/storage/emulated/0/Download/a.pdf"))
    @Test fun documents_path() = assertEquals(Origin.DOCUMENTS, originFromPath("/storage/emulated/0/Documents/a.pdf"))
    @Test fun other_path() = assertEquals(Origin.OTHER, originFromPath("/storage/emulated/0/Pictures/a.pdf"))
    @Test fun whatsapp_authority() = assertEquals(Origin.WHATSAPP, originFromAuthority("com.whatsapp.provider.media"))
    @Test fun downloads_authority() = assertEquals(Origin.DOWNLOAD, originFromAuthority("com.android.providers.downloads.documents"))
    @Test fun null_authority() = assertEquals(Origin.OTHER, originFromAuthority(null))
    // The text itself lives in strings.xml; what matters here is that every constant has
    // its own non-zero label, so no source silently falls back to another one.
    @Test fun every_origin_has_a_distinct_label_resource() {
        val ids = Origin.entries.map { it.labelRes }
        assertEquals(Origin.entries.size, ids.toSet().size)
        assertEquals(emptyList<Int>(), ids.filter { it == 0 })
    }
}
