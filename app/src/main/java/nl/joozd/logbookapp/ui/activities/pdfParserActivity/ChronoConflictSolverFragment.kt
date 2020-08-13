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

package nl.joozd.logbookapp.ui.activities.pdfParserActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityPdfParserViewpagerFragmentBinding
import nl.joozd.logbookapp.extensions.toDateString
import nl.joozd.logbookapp.extensions.toTimeString
import nl.joozd.logbookapp.extensions.toTimeStringLocalized
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.PdfParserActivityViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

/**
 * don't forget tro assign a pagenumber.
 */
class ChronoConflictSolverFragment: JoozdlogFragment() {
    private var pageNumber: Int? = null
    private val viewModel: PdfParserActivityViewModel by activityViewModels()

    fun assignPageNumber(page: Int){
        pageNumber = page
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        ActivityPdfParserViewpagerFragmentBinding.bind(inflater.inflate(R.layout.activity_pdf_parser_viewpager_fragment, container, false)).apply{
            pageNumber = pageNumber ?: savedInstanceState?.getInt(PAGE_NUMBER) ?: 0
            pageNumberText.text = (pageNumber ?: -1 + 1).toString()
            fillFlightData(this)

            return root
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PAGE_NUMBER, pageNumber ?: 0)
    }

    companion object{
        const val PAGE_NUMBER = "PAGE_NUMBER"
    }

    fun fillFlightData(binding: ActivityPdfParserViewpagerFragmentBinding) = with (binding) {
        viewModel.parsedChronoData?.conflicts?.get(pageNumber ?: 0)?.let{ conflict ->
            val orig = conflict.second
            val new = conflict.first
            origDateText.text = orig.tOut().toDateString()
            origFlightnumberText.text = orig.flightNumber
            origOrigText.text = orig.orig
            origDestText.text = orig.dest
            origTimeOutText.text = orig.tOut().toTimeStringLocalized()
            origTimeInText.text = orig.tIn().toTimeStringLocalized()

            newDateText.text = new.tOut().toDateString()
            newFlightnumberText.text = new.flightNumber
            newOrigText.text = new.orig
            newDestText.text = new.dest
            newTimeOutText.text = new.tOut().toTimeStringLocalized()
            newTimeInText.text = new.tIn().toTimeStringLocalized()
        }

    }

}