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

package nl.joozd.logbookapp.ui.activities


import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityBalanceForwardBinding
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.activities.BalanceForwardActivityViewmodel
import nl.joozd.logbookapp.ui.adapters.BalanceForwardAdapter

import nl.joozd.logbookapp.ui.dialogs.AddBalanceForwardDialog
import nl.joozd.logbookapp.ui.utils.toast


class BalanceForwardActivity : JoozdlogActivity() {
    private val viewModel: BalanceForwardActivityViewmodel by viewModels()
    var binding: ActivityBalanceForwardBinding? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_balance_forward, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.addBalanceForwardMenu -> {
            showAddBalanceForwardDialog()
            true
        }
        else -> false
    }



    override fun onCreate(savedInstanceState: Bundle?) {
//        TODO("get confirmation on delete and undo SnackBar")

        super.onCreate(savedInstanceState)
        ActivityBalanceForwardBinding.inflate(layoutInflater).apply{

            setSupportActionBarWithReturn(balanceForwardToolbar)?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = resources.getString(R.string.balanceForward)


                addBalanceButton.setOnClickListener {
                showAddBalanceForwardDialog()
            }

            val adapter = BalanceForwardAdapter().apply{
                onDeleteClicked = {bf -> viewModel.delete(bf)}
                onListItemClicked = {bf, item -> viewModel.itemClicked(bf, item)}
            }
            balanceForwardExListView.setAdapter(adapter)
            viewModel.balancesForward.observe(this@BalanceForwardActivity, Observer {
                adapter.updateList(it)
            })

            setContentView(root)
        }

}


        /**
         * Feedback observers:
         */

        viewModel.feedbackEvent.observe(this, Observer {
            when (it.getEvent()){
                FeedbackEvents.BalanceForwardActivityEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                FeedbackEvents.BalanceForwardActivityEvents.DELETED -> toast("Deleted! (needs a snackbar)") // TODO needs a snackbar
                FeedbackEvents.BalanceForwardActivityEvents.UNDELETE_OK -> toast("Undeleted!")
                FeedbackEvents.BalanceForwardActivityEvents.UNDELETE_FAILED -> toast("Unable to undelete!")
            }
        })

    }

    private fun showAddBalanceForwardDialog(): AddBalanceForwardDialog = AddBalanceForwardDialog().also{
        supportFragmentManager.commit {
            add(R.id.balanceForwardLayout, it)
            addToBackStack(null)
        }
    }

}


