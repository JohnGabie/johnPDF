/*
 * Ícones derivados de Material Symbols (Rounded, weight 400, grade 0, optical size 24)
 * de https://github.com/google/material-design-icons
 *
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ARQUIVO GERADO — não editar à mão.
 * Regenerar com: py tools/icons/generate_johnicons.py
 * SVGs de origem: tools/icons/svg/ (commit upstream em tools/icons/UPSTREAM.txt)
 */
package com.johngabie.johnpdf.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/**
 * Constrói um Material Symbol 24dp a partir do atributo `d` do SVG.
 *
 * O viewBox dos Symbols é `0 -960 960 960` (origem embaixo, Y negativo). O
 * [group] com `translationY = 960f` traz o desenho para o quadrante positivo
 * sem mexer numa única coordenada do `d` — é o que mantém a string idêntica
 * ao arquivo upstream.
 *
 * `name` vira `"JohnIcons.<Nome>"`: é como o ícone aparece no dump de
 * semântica e nas mensagens de falha dos testes.
 *
 * A cor preta é um marcador. `Icon(...)` sempre aplica tint
 * (`LocalContentColor` por padrão), então nenhum ícone chega à tela em preto fixo.
 */
private fun symbol(
    name: String,
    d: String,
    autoMirror: Boolean = false,
    fillType: PathFillType = PathFillType.NonZero,
): ImageVector =
    ImageVector.Builder(
        name = "JohnIcons.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = autoMirror,
    ).apply {
        group(translationY = 960f) {
            addPath(
                pathData = addPathNodes(d),
                pathFillType = fillType,
                fill = SolidColor(Color.Black),
            )
        }
    }.build()

/** Os ícones do johnPDF. Sempre usar via `Icon(JohnIcons.X, contentDescription = ...)`. */
object JohnIcons {
    /** `arrow_back` — arrow_back_24px.svg. */
    val ArrowBack: ImageVector by lazy { symbol("ArrowBack", D_ARROW_BACK, autoMirror = true) }
    /** `close` — close_24px.svg. */
    val Close: ImageVector by lazy { symbol("Close", D_CLOSE) }
    /** `delete` — delete_24px.svg. */
    val Delete: ImageVector by lazy { symbol("Delete", D_DELETE) }
    /** `dialpad` — dialpad_24px.svg. */
    val Dialpad: ImageVector by lazy { symbol("Dialpad", D_DIALPAD) }
    /** `error` — error_24px.svg. */
    val Error: ImageVector by lazy { symbol("Error", D_ERROR) }
    /** `folder` — folder_24px.svg. */
    val Folder: ImageVector by lazy { symbol("Folder", D_FOLDER) }
    /** `folder_open` — folder_open_24px.svg. */
    val FolderOpen: ImageVector by lazy { symbol("FolderOpen", D_FOLDER_OPEN) }
    /** `keyboard` — keyboard_24px.svg. */
    val Keyboard: ImageVector by lazy { symbol("Keyboard", D_KEYBOARD) }
    /** `keyboard_arrow_down` — keyboard_arrow_down_24px.svg. */
    val KeyboardArrowDown: ImageVector by lazy { symbol("KeyboardArrowDown", D_KEYBOARD_ARROW_DOWN) }
    /** `keyboard_arrow_up` — keyboard_arrow_up_24px.svg. */
    val KeyboardArrowUp: ImageVector by lazy { symbol("KeyboardArrowUp", D_KEYBOARD_ARROW_UP) }
    /** `library_books` — library_books_24px.svg. */
    val LibraryBooks: ImageVector by lazy { symbol("LibraryBooks", D_LIBRARY_BOOKS) }
    /** `library_books` (fill 1) — library_books_fill1_24px.svg. */
    val LibraryBooksFilled: ImageVector by lazy { symbol("LibraryBooksFilled", D_LIBRARY_BOOKS_FILLED) }
    /** `lock` — lock_24px.svg. */
    val Lock: ImageVector by lazy { symbol("Lock", D_LOCK) }
    /** `picture_as_pdf` — picture_as_pdf_24px.svg. */
    val PictureAsPdf: ImageVector by lazy { symbol("PictureAsPdf", D_PICTURE_AS_PDF) }
    /** `schedule` — schedule_24px.svg. */
    val Schedule: ImageVector by lazy { symbol("Schedule", D_SCHEDULE) }
    /** `schedule` (fill 1) — schedule_fill1_24px.svg. */
    val ScheduleFilled: ImageVector by lazy { symbol("ScheduleFilled", D_SCHEDULE_FILLED) }
    /** `screen_lock_rotation` — screen_lock_rotation_24px.svg. */
    val ScreenLockRotation: ImageVector by lazy { symbol("ScreenLockRotation", D_SCREEN_LOCK_ROTATION) }
    /** `screen_rotation` — screen_rotation_24px.svg. */
    val ScreenRotation: ImageVector by lazy { symbol("ScreenRotation", D_SCREEN_ROTATION) }
    /** `search` — search_24px.svg. */
    val Search: ImageVector by lazy { symbol("Search", D_SEARCH) }
    /** `search_off` — search_off_24px.svg. */
    val SearchOff: ImageVector by lazy { symbol("SearchOff", D_SEARCH_OFF) }
    /** `visibility` — visibility_24px.svg. */
    val Visibility: ImageVector by lazy { symbol("Visibility", D_VISIBILITY) }
    /** `visibility_off` — visibility_off_24px.svg. */
    val VisibilityOff: ImageVector by lazy { symbol("VisibilityOff", D_VISIBILITY_OFF) }

    /**
     * Todos os ícones, para os testes de sanidade de `JohnIconsTest`.
     * Não usar em código de produção.
     */
    val All: List<ImageVector>
        get() = listOf(
            ArrowBack, Close, Delete, Dialpad, Error, Folder, FolderOpen, Keyboard,
            KeyboardArrowDown, KeyboardArrowUp, LibraryBooks, LibraryBooksFilled, Lock,
            PictureAsPdf, Schedule, ScheduleFilled, ScreenLockRotation, ScreenRotation, Search,
            SearchOff, Visibility, VisibilityOff,
        )
}

// Copiado byte a byte do atributo `d` de tools/icons/svg/arrow_back_24px.svg — não editar.
private const val D_ARROW_BACK =
    "m313-440 196 196q12 12 11.5 28T508-188q-12 11-28 11.5T452-188L188-452q-6-6-8.5-13t-2.5-15q0-8 2.5-15t8.5-13l264-264q11-11 27.5-11t28.5 11q12 12 12 28.5T508-715L313-520h447q17 0 28.5 11.5T800-480q0 17-11.5 28.5T760-440H313Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/close_24px.svg — não editar.
private const val D_CLOSE =
    "M480-424 284-228q-11 11-28 11t-28-11q-11-11-11-28t11-28l196-196-196-196q-11-11-11-28t11-28q11-11 28-11t28 11l196 196 196-196q11-11 28-11t28 11q11 11 11 28t-11 28L536-480l196 196q11 11 11 28t-11 28q-11 11-28 11t-28-11L480-424Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/delete_24px.svg — não editar.
private const val D_DELETE =
    "M280-120q-33 0-56.5-23.5T200-200v-520q-17 0-28.5-11.5T160-760q0-17 11.5-28.5T200-800h160q0-17 11.5-28.5T400-840h160q17 0 28.5 11.5T600-800h160q17 0 28.5 11.5T800-760q0 17-11.5 28.5T760-720v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM400-280q17 0 28.5-11.5T440-320v-280q0-17-11.5-28.5T400-640q-17 0-28.5 11.5T360-600v280q0 17 11.5 28.5T400-280Zm160 0q17 0 28.5-11.5T600-320v-280q0-17-11.5-28.5T560-640q-17 0-28.5 11.5T520-600v280q0 17 11.5 28.5T560-280ZM280-720v520-520Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/dialpad_24px.svg — não editar.
private const val D_DIALPAD =
    "M480-40q-33 0-56.5-23.5T400-120q0-33 23.5-56.5T480-200q33 0 56.5 23.5T560-120q0 33-23.5 56.5T480-40ZM240-760q-33 0-56.5-23.5T160-840q0-33 23.5-56.5T240-920q33 0 56.5 23.5T320-840q0 33-23.5 56.5T240-760Zm0 240q-33 0-56.5-23.5T160-600q0-33 23.5-56.5T240-680q33 0 56.5 23.5T320-600q0 33-23.5 56.5T240-520Zm0 240q-33 0-56.5-23.5T160-360q0-33 23.5-56.5T240-440q33 0 56.5 23.5T320-360q0 33-23.5 56.5T240-280Zm480-480q-33 0-56.5-23.5T640-840q0-33 23.5-56.5T720-920q33 0 56.5 23.5T800-840q0 33-23.5 56.5T720-760ZM480-280q-33 0-56.5-23.5T400-360q0-33 23.5-56.5T480-440q33 0 56.5 23.5T560-360q0 33-23.5 56.5T480-280Zm240 0q-33 0-56.5-23.5T640-360q0-33 23.5-56.5T720-440q33 0 56.5 23.5T800-360q0 33-23.5 56.5T720-280Zm0-240q-33 0-56.5-23.5T640-600q0-33 23.5-56.5T720-680q33 0 56.5 23.5T800-600q0 33-23.5 56.5T720-520Zm-240 0q-33 0-56.5-23.5T400-600q0-33 23.5-56.5T480-680q33 0 56.5 23.5T560-600q0 33-23.5 56.5T480-520Zm0-240q-33 0-56.5-23.5T400-840q0-33 23.5-56.5T480-920q33 0 56.5 23.5T560-840q0 33-23.5 56.5T480-760Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/error_24px.svg — não editar.
private const val D_ERROR =
    "M480-280q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280Zm0-160q17 0 28.5-11.5T520-480v-160q0-17-11.5-28.5T480-680q-17 0-28.5 11.5T440-640v160q0 17 11.5 28.5T480-440Zm0 360q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/folder_24px.svg — não editar.
private const val D_FOLDER =
    "M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h207q16 0 30.5 6t25.5 17l57 57h320q33 0 56.5 23.5T880-640v400q0 33-23.5 56.5T800-160H160Zm0-80h640v-400H447l-80-80H160v480Zm0 0v-480 480Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/folder_open_24px.svg — não editar.
private const val D_FOLDER_OPEN =
    "M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h207q16 0 30.5 6t25.5 17l57 57h360q17 0 28.5 11.5T880-680q0 17-11.5 28.5T840-640H447l-80-80H160v480l79-263q8-26 29.5-41.5T316-560h516q41 0 64.5 32.5T909-457l-72 240q-8 26-29.5 41.5T760-160H160Zm84-80h516l72-240H316l-72 240Zm-84-262v-218 218Zm84 262 72-240-72 240Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/keyboard_24px.svg — não editar.
private const val D_KEYBOARD =
    "M160-200q-33 0-56.5-23.5T80-280v-400q0-33 23.5-56.5T160-760h640q33 0 56.5 23.5T880-680v400q0 33-23.5 56.5T800-200H160Zm0-80h640v-400H160v400Zm200-40h240q17 0 28.5-11.5T640-360q0-17-11.5-28.5T600-400H360q-17 0-28.5 11.5T320-360q0 17 11.5 28.5T360-320Zm-200 40v-400 400Zm80-280q17 0 28.5-11.5T280-600q0-17-11.5-28.5T240-640q-17 0-28.5 11.5T200-600q0 17 11.5 28.5T240-560Zm120 0q17 0 28.5-11.5T400-600q0-17-11.5-28.5T360-640q-17 0-28.5 11.5T320-600q0 17 11.5 28.5T360-560Zm120 0q17 0 28.5-11.5T520-600q0-17-11.5-28.5T480-640q-17 0-28.5 11.5T440-600q0 17 11.5 28.5T480-560Zm120 0q17 0 28.5-11.5T640-600q0-17-11.5-28.5T600-640q-17 0-28.5 11.5T560-600q0 17 11.5 28.5T600-560Zm120 0q17 0 28.5-11.5T760-600q0-17-11.5-28.5T720-640q-17 0-28.5 11.5T680-600q0 17 11.5 28.5T720-560ZM240-440q17 0 28.5-11.5T280-480q0-17-11.5-28.5T240-520q-17 0-28.5 11.5T200-480q0 17 11.5 28.5T240-440Zm120 0q17 0 28.5-11.5T400-480q0-17-11.5-28.5T360-520q-17 0-28.5 11.5T320-480q0 17 11.5 28.5T360-440Zm120 0q17 0 28.5-11.5T520-480q0-17-11.5-28.5T480-520q-17 0-28.5 11.5T440-480q0 17 11.5 28.5T480-440Zm120 0q17 0 28.5-11.5T640-480q0-17-11.5-28.5T600-520q-17 0-28.5 11.5T560-480q0 17 11.5 28.5T600-440Zm120 0q17 0 28.5-11.5T760-480q0-17-11.5-28.5T720-520q-17 0-28.5 11.5T680-480q0 17 11.5 28.5T720-440Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/keyboard_arrow_down_24px.svg — não editar.
private const val D_KEYBOARD_ARROW_DOWN =
    "M480-361q-8 0-15-2.5t-13-8.5L268-556q-11-11-11-28t11-28q11-11 28-11t28 11l156 156 156-156q11-11 28-11t28 11q11 11 11 28t-11 28L508-372q-6 6-13 8.5t-15 2.5Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/keyboard_arrow_up_24px.svg — não editar.
private const val D_KEYBOARD_ARROW_UP =
    "M480-528 324-372q-11 11-28 11t-28-11q-11-11-11-28t11-28l184-184q12-12 28-12t28 12l184 184q11 11 11 28t-11 28q-11 11-28 11t-28-11L480-528Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/library_books_24px.svg — não editar.
private const val D_LIBRARY_BOOKS =
    "M440-400h80q17 0 28.5-11.5T560-440q0-17-11.5-28.5T520-480h-80q-17 0-28.5 11.5T400-440q0 17 11.5 28.5T440-400Zm0-120h240q17 0 28.5-11.5T720-560q0-17-11.5-28.5T680-600H440q-17 0-28.5 11.5T400-560q0 17 11.5 28.5T440-520Zm0-120h240q17 0 28.5-11.5T720-680q0-17-11.5-28.5T680-720H440q-17 0-28.5 11.5T400-680q0 17 11.5 28.5T440-640ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-520q0-17 11.5-28.5T120-720q17 0 28.5 11.5T160-680v520h520q17 0 28.5 11.5T720-120q0 17-11.5 28.5T680-80H160Zm160-720v480-480Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/library_books_fill1_24px.svg — não editar.
private const val D_LIBRARY_BOOKS_FILLED =
    "M440-400h80q17 0 28.5-11.5T560-440q0-17-11.5-28.5T520-480h-80q-17 0-28.5 11.5T400-440q0 17 11.5 28.5T440-400Zm0-120h240q17 0 28.5-11.5T720-560q0-17-11.5-28.5T680-600H440q-17 0-28.5 11.5T400-560q0 17 11.5 28.5T440-520Zm0-120h240q17 0 28.5-11.5T720-680q0-17-11.5-28.5T680-720H440q-17 0-28.5 11.5T400-680q0 17 11.5 28.5T440-640ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320ZM160-80q-33 0-56.5-23.5T80-160v-520q0-17 11.5-28.5T120-720q17 0 28.5 11.5T160-680v520h520q17 0 28.5 11.5T720-120q0 17-11.5 28.5T680-80H160Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/lock_24px.svg — não editar.
private const val D_LOCK =
    "M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240Zm0-80h480v-400H240v400Zm240-120q33 0 56.5-23.5T560-360q0-33-23.5-56.5T480-440q-33 0-56.5 23.5T400-360q0 33 23.5 56.5T480-280ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85v80ZM240-160v-400 400Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/picture_as_pdf_24px.svg — não editar.
private const val D_PICTURE_AS_PDF =
    "M400-540h40q17 0 28.5-11.5T480-580v-40q0-17-11.5-28.5T440-660h-60q-8 0-14 6t-6 14v160q0 8 6 14t14 6q8 0 14-6t6-14v-60Zm0-40v-40h40v40h-40Zm200 120q17 0 28.5-11.5T640-500v-120q0-17-11.5-28.5T600-660h-60q-8 0-14 6t-6 14v160q0 8 6 14t14 6h60Zm-40-40v-120h40v120h-40Zm160-40h20q8 0 14-6t6-14q0-8-6-14t-14-6h-20v-40h20q8 0 14-6t6-14q0-8-6-14t-14-6h-40q-8 0-14 6t-6 14v160q0 8 6 14t14 6q8 0 14-6t6-14v-60ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-520q0-17 11.5-28.5T120-720q17 0 28.5 11.5T160-680v520h520q17 0 28.5 11.5T720-120q0 17-11.5 28.5T680-80H160Zm160-720v480-480Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/schedule_24px.svg — não editar.
private const val D_SCHEDULE =
    "M520-496v-144q0-17-11.5-28.5T480-680q-17 0-28.5 11.5T440-640v159q0 8 3 15.5t9 13.5l132 132q11 11 28 11t28-11q11-11 11-28t-11-28L520-496ZM480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-400Zm0 320q133 0 226.5-93.5T800-480q0-133-93.5-226.5T480-800q-133 0-226.5 93.5T160-480q0 133 93.5 226.5T480-160Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/schedule_fill1_24px.svg — não editar.
private const val D_SCHEDULE_FILLED =
    "M520-496v-144q0-17-11.5-28.5T480-680q-17 0-28.5 11.5T440-640v159q0 8 3 15.5t9 13.5l132 132q11 11 28 11t28-11q11-11 11-28t-11-28L520-496ZM480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/screen_lock_rotation_24px.svg — não editar.
private const val D_SCREEN_LOCK_ROTATION =
    "M634-600q-14 0-24-10t-10-24v-132q0-14 10-24t24-10h6v-40q0-33 23.5-56.5T720-920q33 0 56.5 23.5T800-840v40h6q14 0 24 10t10 24v132q0 14-10 24t-24 10H634Zm46-200h80v-40q0-17-11.5-28.5T720-880q-17 0-28.5 11.5T680-840v40ZM441-129l-77-77q-11-11-11-28t11-28q11-11 28-11t28 11L590-92q12 12 7 28t-22 19q-14 2-27.5 2.5T520-42q-99 0-186.5-38T181-183q-65-65-103-152.5T40-522q0-17 11.5-28.5T80-562q17 0 28.5 11.5T120-522q0 72 24.5 137T212-268q43 52 102 88.5T441-129Zm149-73q-14 0-28.5-5.5T536-224L222-538q-11-11-16.5-25.5T200-592q0-15 5.5-29t16.5-25l174-174q11-11 25.5-17t28.5-6q15 0 29 6t25 17l17 17q11 11 11.5 27T522-748q-11 12-28 12.5T465-747l-15-15-170 170 310 310 170-170-14-14q-11-11-11-28t11-28q11-11 28-11t28 11l16 16q11 11 17 25t6 29q0 14-6 28.5T818-398L644-224q-11 11-25 16.5t-29 5.5Zm-70-320Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/screen_rotation_24px.svg — não editar.
private const val D_SCREEN_ROTATION =
    "M496-182 182-496q-23-23-23-54t23-54l174-174q23-23 54-23t54 23l314 314q23 23 23 54t-23 54L604-182q-23 23-54 23t-54-23Zm54-58 170-170-310-310-170 170 310 310Zm-70-240Zm79-393 77 77q11 11 11 28t-11 28q-11 11-28 11t-28-11L410-910q-12-12-6.5-28t22.5-19q14-2 27-2.5t27-.5q99 0 186.5 37.5t153 103q65.5 65.5 103 153T960-480q0 17-11.5 28.5T920-440q-17 0-28.5-11.5T880-480q0-71-24-136t-66.5-117Q747-785 688-821.5T559-873ZM401-87l-77-77q-11-11-11-28t11-28q11-11 28-11t28 11L550-50q12 12 6.5 28.5T534-3q-14 2-27 2.5T480 0q-99 0-186.5-37.5t-153-103Q75-206 37.5-293.5T0-480q0-17 11.5-28.5T40-520q17 0 28.5 11.5T80-480q0 71 24 136t66.5 117Q213-175 272-138.5T401-87Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/search_24px.svg — não editar.
private const val D_SEARCH =
    "M380-320q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l224 224q11 11 11 28t-11 28q-11 11-28 11t-28-11L532-372q-30 24-69 38t-83 14Zm0-80q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/search_off_24px.svg — não editar.
private const val D_SEARCH_OFF =
    "m280-252 56 57q6 6 14 6t14-6q6-6 6-14.5t-6-14.5l-56-56 57-57q6-6 6-14t-6-14q-6-6-14-6t-14 6l-57 57-57-57q-6-6-14-6t-14 6q-6 6-6 14t6 14l57 57-57 57q-6 6-6 14t6 14q6 6 14 6t14-6l57-57Zm0 172q-83 0-141.5-58.5T80-280q0-83 58.5-141.5T280-480q83 0 141.5 58.5T480-280q0 83-58.5 141.5T280-80Zm288-296q-12-13-25.5-26.5T516-428q38-24 61-64t23-88q0-75-52.5-127.5T420-760q-75 0-127.5 52.5T240-580q0 6 .5 11.5T242-557q-18 2-39.5 8T164-535q-2-11-3-22t-1-23q0-109 75.5-184.5T420-840q109 0 184.5 75.5T680-580q0 43-13.5 81.5T629-428l223 224q11 11 11.5 27.5T852-148q-11 11-28 11t-28-11L568-376Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/visibility_24px.svg — não editar.
private const val D_VISIBILITY =
    "M480-320q75 0 127.5-52.5T660-500q0-75-52.5-127.5T480-680q-75 0-127.5 52.5T300-500q0 75 52.5 127.5T480-320Zm0-72q-45 0-76.5-31.5T372-500q0-45 31.5-76.5T480-608q45 0 76.5 31.5T588-500q0 45-31.5 76.5T480-392Zm0 192q-134 0-244.5-72T61-462q-5-9-7.5-18.5T51-500q0-10 2.5-19.5T61-538q64-118 174.5-190T480-800q134 0 244.5 72T899-538q5 9 7.5 18.5T909-500q0 10-2.5 19.5T899-462q-64 118-174.5 190T480-200Zm0-300Zm0 220q113 0 207.5-59.5T832-500q-50-101-144.5-160.5T480-720q-113 0-207.5 59.5T128-500q50 101 144.5 160.5T480-280Z"

// Copiado byte a byte do atributo `d` de tools/icons/svg/visibility_off_24px.svg — não editar.
private const val D_VISIBILITY_OFF =
    "M607-627q29 29 42.5 66t9.5 76q0 15-11 25.5T622-449q-15 0-25.5-10.5T586-485q5-26-3-50t-25-41q-17-17-41-26t-51-4q-15 0-25.5-11T430-643q0-15 10.5-25.5T466-679q38-4 75 9.5t66 42.5Zm-127-93q-19 0-37 1.5t-36 5.5q-17 3-30.5-5T358-742q-5-16 3.5-31t24.5-18q23-5 46.5-7t47.5-2q137 0 250.5 72T904-534q4 8 6 16.5t2 17.5q0 9-1.5 17.5T905-466q-18 40-44.5 75T802-327q-12 11-28 9t-26-16q-10-14-8.5-30.5T753-392q24-23 44-50t35-58q-50-101-144.5-160.5T480-720Zm0 520q-134 0-245-72.5T60-463q-5-8-7.5-17.5T50-500q0-10 2-19t7-18q20-40 46.5-76.5T166-680l-83-84q-11-12-10.5-28.5T84-820q11-11 28-11t28 11l680 680q11 11 11.5 27.5T820-84q-11 11-28 11t-28-11L624-222q-35 11-71 16.5t-73 5.5ZM222-624q-29 26-53 57t-41 67q50 101 144.5 160.5T480-280q20 0 39-2.5t39-5.5l-36-38q-11 3-21 4.5t-21 1.5q-75 0-127.5-52.5T300-500q0-11 1.5-21t4.5-21l-84-82Zm319 93Zm-151 75Z"
