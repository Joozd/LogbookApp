/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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


/**
 * Checks password safety. As in: Number of characters and such
 * When editing, don't forget to adjust R.string.passwords_do_not_match
 */
package nl.joozd.logbookapp.utils

//minimum length of password
private const val MIN_LENGTH = 6

//max amount of characters repeating, eg "1222345" has 3
// setting this to 0 means any amount of repetitions is allowed
private const val MAX_REPEATING_CHARACTERS = 0

//max amount of characters in order, eg abcde = 5 and 12354 = 3
//TODO not implemented
private const val MAX_CONSECUTIVE_CHARACTERS = 0

//needs a letter and a number
private const val NEEDS_LETTER_AND_NUMBER = false

// needs a non-letter/number character
private const val NEEDS_NON_ALPHANUMERIC_CHARACTER = false

//minimum amount of distinct characters
private const val MIN_DISTINCT_CHARACTERS = 2


fun checkPasswordSafety(p: String): Boolean =
    p.length >= MIN_LENGTH
            && p.checkRepeatingChars(MAX_REPEATING_CHARACTERS)
            && p.checkConsecutiveChars(MAX_CONSECUTIVE_CHARACTERS)
            && p.hasLetterAndNumber(NEEDS_LETTER_AND_NUMBER)
            && p.hasNotAlphaNumeric(NEEDS_NON_ALPHANUMERIC_CHARACTER)
            && p.distinctChars() >= MIN_DISTINCT_CHARACTERS


/**
 * Checks if any character repeats more than [maxAmount] times in a String
 * 0 to allow any amount of repetitions
 */
private fun String.checkRepeatingChars(maxAmount: Int): Boolean{
    if (maxAmount == 0) return true
    for(n in (0..length-(maxAmount+1))){
        if (slice(n until n+maxAmount+1).toSet().size == 1) return false
    }
    return true
}

private fun String.checkConsecutiveChars(maxAmount: Int): Boolean{
    if (maxAmount == 0) return true
    else TODO("Not implemented")
}

private fun String.hasLetterAndNumber(required: Boolean): Boolean{
    return if (!required) true
    else this.any{it.isLetter()} && this.any{it.isDigit()}
}

private fun String.hasNotAlphaNumeric(required: Boolean): Boolean{
    return if (!required) true
    else this.any{!it.isLetterOrDigit()}
}

private fun String.distinctChars(): Int = this.toSet().size