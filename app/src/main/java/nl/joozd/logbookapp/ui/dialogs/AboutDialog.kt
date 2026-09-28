/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp.ui.dialogs

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.R

/**
 * Displays information about the application, including its installed version and build number.
 */
class AboutDialog : LongTextDialog() {

    override val titleRes = R.string.about

    override val textFlow = createFlowFromRaw(R.raw.about_joozdlog).map { text ->
        val context = requireContext()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        HtmlCompat.fromHtml(
            text
                .replace(VERSION_STRING, packageInfo.versionName.orEmpty())
                .replace(BUILD_STRING, packageInfo.longVersionCode.toString()),
            HtmlCompat.FROM_HTML_MODE_COMPACT,
        )
    }

    companion object {
        private const val VERSION_STRING = $$"$VERSION$"
        private const val BUILD_STRING = $$"$BUILD$"
    }
}