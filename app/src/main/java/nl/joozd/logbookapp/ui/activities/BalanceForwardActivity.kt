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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_balance_forward.*
import nl.joozd.logbookapp.R
import nl.joozd.joozdlogcommon.BalanceForward


import nl.joozd.logbookapp.ui.adapters.BalanceForwardAdapter


class BalanceForwardActivity : AppCompatActivity() {
    companion object{
        const val TAG = "BALANCE_FORWARD_ACTIVITY"
    }

    //private val balanceForwardDb = BalanceForwardDb()
    private var allBalanceForwards: List<BalanceForward> = emptyList()
    private lateinit var adapter: BalanceForwardAdapter

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_balance_forward, menu)
        return true
    }
/*
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.addBalanceForwardMenu -> {
            addBalanceButton.hide()
            val addBalanceForwardDialog = AddBalanceForwardDialog()
            addBalanceForwardDialog.balanceForwardId = balanceForwardDb.highestId+1
            Log.d(TAG, "id = ${addBalanceForwardDialog.balanceForwardId}")
            Log.d(TAG, "highestId = ${balanceForwardDb.highestId}")
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
            true
        }
        else -> false
    }


*/

    override fun onCreate(savedInstanceState: Bundle?) {
//        TODO("get confirmation on delete and undo SnackBar")
//        TODO("add these to totals")
        //TODO also change Totals to activity instead of fragment

        super.onCreate(savedInstanceState)


        // allBalanceForwards = balanceForwardDb.requestAllBalanceForwards()
        Log.d(TAG, "${allBalanceForwards.size} records found")


        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_balance_forward)
        setSupportActionBar(balance_forward_toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources.getString(R.string.balanceForward)
/*
        val expandableList: ExpandableListView = expandible_listview
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


}


