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

package nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status

import nl.joozd.logbookapp.core.messages.UserMessage

/**
 * Used to communicate a user choice to UI.
 * choice 1 should map to positive, choice 2 to negative
 */
class UserChoice(
    titleResource: Int,
    descriptionResource: Int,
    choice1Resource: Int?,
    choice2Resource: Int?,
    action1: ActionListener,
    action2: ActionListener
): HandlerStatus, UserMessage(titleResource, descriptionResource, choice1Resource, choice2Resource, action1, action2) {
    class Builder : UserMessage.Builder() {
        override fun build(): UserChoice =
            UserChoice(
                titleResource ?: android.R.string.unknownName,
                descriptionResource ?: android.R.string.unknownName,
                choice1Resource,
                choice2Resource,
                action1Listener,
                action2Listener
            )
    }
}