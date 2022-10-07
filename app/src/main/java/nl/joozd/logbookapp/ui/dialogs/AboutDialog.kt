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
import nl.joozd.logbookapp.BuildConfig
import nl.joozd.logbookapp.R

class AboutDialog: LongTextDialog() {

    override val titleRes = R.string.about
    override val textFlow = createFlowFromRaw(R.raw.about_joozdlog).map{ s ->
        HtmlCompat.fromHtml(insertVersionAndBuildCodeIntoString(s), HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    private fun insertVersionAndBuildCodeIntoString(s: String) =
        s.replace(VERSION_STRING, BuildConfig.VERSION_NAME)
            .replace(BUILD_STRING, BuildConfig.VERSION_CODE.toString())

    companion object{
        const val VERSION_STRING = "\$VERSION\$"
        const val BUILD_STRING = "\$BUILD\$"
    }
}