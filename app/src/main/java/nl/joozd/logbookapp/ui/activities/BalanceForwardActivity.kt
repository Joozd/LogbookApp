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

package nl.joozd.logbookapp.ui.activities


import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.databinding.ActivityBalanceForwardBinding
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.model.viewmodels.activities.BalanceForwardActivityViewmodel
import nl.joozd.logbookapp.ui.adapters.BalanceForwardAdapter

import nl.joozd.logbookapp.ui.dialogs.AddBalanceForwardDialog
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast

// TODO get confirmation on delete or make undo SnackBar
class BalanceForwardActivity : JoozdlogActivity() {
    private val viewModel: BalanceForwardActivityViewmodel by viewModels()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_balance_forward, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.addBalanceForwardMenu -> {
            showFragment<AddBalanceForwardDialog>()
            true
        }
        else -> false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityBalanceForwardBinding.inflate(layoutInflater).apply {
            initializeToolBar()
            initializeBalanceForwardExpandableListView()
        }

        setContentView(binding.root)
    }



    private fun ActivityBalanceForwardBinding.initializeBalanceForwardExpandableListView() {
        val adapter = BalanceForwardAdapter(activity).apply {
            onDeleteClicked = { bf -> deleteBalanceForward(bf) }
            onListItemClicked = { bf, item -> balanceForwardItemClicked(bf, item) }
        }

        balanceForwardExpandableListView.setAdapter(adapter)
        viewModel.balancesForward.launchCollectWhileLifecycleStateStarted {
            adapter.updateList(it)
        }
    }

    @Suppress("UNUSED_PARAMETER") // this is a stub
    private fun balanceForwardItemClicked(bf: BalanceForward, item: Int) {
        toast("Not implemented!")
    }

    private fun deleteBalanceForward(bf: BalanceForward) {
        lifecycleScope.launch {
            viewModel.delete(bf)
            toast("Deleted!") // TODO make this undoable or ask for confirmation
        }
    }

    private fun ActivityBalanceForwardBinding.initializeToolBar() {
        setSupportActionBarWithReturn(balanceForwardToolbar)?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.balanceForward)

            addBalanceButton.setOnClickListener {
                showFragment<AddBalanceForwardDialog>()
            }
        }
    }
}


