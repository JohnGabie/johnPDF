package com.johngabie.johnpdf.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.johngabie.johnpdf.R
import com.johngabie.johnpdf.util.DateLabels

/** Pulls the localized date words and patterns out of resources for [com.johngabie.johnpdf.util.friendlyDate]. */
@Composable
fun rememberDateLabels(): DateLabels = DateLabels(
    today = stringResource(R.string.date_today),
    yesterday = stringResource(R.string.date_yesterday),
    sameYearPattern = stringResource(R.string.date_pattern_same_year),
    otherYearPattern = stringResource(R.string.date_pattern_other_year),
)
