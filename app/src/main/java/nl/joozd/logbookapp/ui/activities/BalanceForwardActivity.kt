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
import kotlinx.android.synthetic.main.activity_balance_forward.*
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
//        TODO("add these to totals")

        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)
        binding = ActivityBalanceForwardBinding.inflate(layoutInflater).apply{
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

        setSupportActionBarWithReturn(balance_forward_toolbar)?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.balanceForward)
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
/*

        adapter = BalanceForwardAdapter(this, allBalanceForwards, expandible_listview)
        adapter.let {a->
            a.setOnActionImageViewClicked { bf ->
                alert("Are you sure you want to delete?") {
                    yesButton {
                        balanceForwardDb.deleteBalanceForward(bf)
                        allBalanceForwards = balanceForwardDb.requestAllBalanceForwards()
                        a.balancesForward = allBalanceForwards
                        val snackBar = CustomSnackbar.make(balanceForwardLayout)
                        snackBar.setMessage("Deleted Balance Forward")
                        snackBar.setOnActionBarShown { addBalanceButton.hide() }
                        snackBar.setOnActionBarGone { addBalanceButton.show() }
                        snackBar.setOnAction {
                            balanceForwardDb.saveBalanceForward(bf)
                            allBalanceForwards = balanceForwardDb.requestAllBalanceForwards()
                            a.balancesForward = allBalanceForwards
                            snackBar.dismiss()
                        }
                        snackBar.duration =1000*10
                        snackBar.show()
                    }
                    noButton { }
                }.show()
            }

            a.setOnItemClicked {bf ->
                addBalanceButton.hide()
                val addBalanceForwardDialog = AddBalanceForwardDialog()
                addBalanceForwardDialog.balanceForward = bf
                addBalanceForwardDialog.balanceForwardId = balanceForwardDb.highestId
                addBalanceForwardDialog.setOnSave {
                    balanceForwardDb.saveBalanceForward(it)
                    allBalanceForwards = allBalanceForwards.filter{bf -> bf.id != it.id} + it
                    adapter?.balancesForward = allBalanceForwards
                }
                addBalanceForwardDialog.setOnClose {
                    addBalanceButton.show()
                }
                supportFragmentManager.beginTransaction()
                    .add(R.id.balanceForwardLayoutBelowToolbar, addBalanceForwardDialog)
                    .addToBackStack(null)
                    .commit()
            }
        }
        expandableList.setAdapter(adapter)
        adapter.notifyDataSetChanged()

        addBalanceButton.setOnClickListener {
            addBalanceButton.hide()
            val addBalanceForwardDialog = AddBalanceForwardDialog()
            addBalanceForwardDialog.balanceForwardId = balanceForwardDb.highestId+1
            addBalanceForwardDialog.setOnSave {
                balanceForwardDb.saveBalanceForward(it)
                allBalanceForwards += it
                adapter.balancesForward = allBalanceForwards
            }
            addBalanceForwardDialog.setOnClose {
                addBalanceButton.show()
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.balanceForwardLayoutBelowToolbar, addBalanceForwardDialog)
                .addToBackStack(null)
                .commit()
        }
      */

    }

    private fun showAddBalanceForwardDialog(): AddBalanceForwardDialog = AddBalanceForwardDialog().also{
        supportFragmentManager.commit {
            add(R.id.balanceForwardLayout, it)
            addToBackStack(null)
        }
    }

}

