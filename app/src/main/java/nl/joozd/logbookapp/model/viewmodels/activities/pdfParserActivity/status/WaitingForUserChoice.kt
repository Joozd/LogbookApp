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

class WaitingForUserChoice(
    val titleResource: Int,
    val descriptionResource: Int,
    val choice1Resource: Int,
    val choice2Resource: Int,
    val action1: UserChoiceListener,
    val action2: UserChoiceListener
): HandlerStatus() {
    fun interface UserChoiceListener{
        operator fun invoke()
    }

    class Builder{
        var titleResource: Int? = null
        var descriptionResource: Int? = null
        var choice1Resource: Int? = null
        var choice2Resource: Int? = null
        private var action1 = UserChoiceListener {  }
        private var action2 = UserChoiceListener {  }

        fun setAction1(action: UserChoiceListener){
            action1 = action
        }

        fun setAction2(action: UserChoiceListener){
            action2 = action
        }

        fun build(): WaitingForUserChoice =
            WaitingForUserChoice(titleResource!!, descriptionResource!!, choice1Resource!!, choice2Resource!!, action1, action2)
    }

}