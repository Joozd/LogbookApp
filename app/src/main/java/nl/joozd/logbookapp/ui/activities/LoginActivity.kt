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

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.ui.utils.longToast

import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext

class LoginActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Preferences.username?.let{
            usernameEditText.setText(it)
        }
        Preferences.password?.let{
            // passwordEditText.setText(getString(R.string.hidden_password))
            passwordEditText.setText(it)
        }

        saveLoginDetailsButton.setOnClickListener {
            //Save username
            var goAhead = true
            if (Preferences.username != null){
                AlertDialog.Builder(this).apply {
                    setMessage("TODO blabla change username")

                    setNegativeButton("CANCEL", DialogInterface.OnClickListener {_, _ -> goAhead = false })
                }.create().apply{
                    setTitle("TIETEN")
                    show()
                }

            }
            val username = usernameEditText.text.toString()
            if (goAhead && username.isNotEmpty()) Preferences.username = username


            goAhead = true
            if (Preferences.password != null){
                AlertDialog.Builder(this).apply {
                    setMessage("TODO blabla change paaswoord")

                    setNegativeButton("CANCEL", DialogInterface.OnClickListener {_, _ -> goAhead = false })
                }.create().apply{
                    setTitle("TIETEN")
                    show()
                }
            }
            val password = passwordEditText.text.toString()
            if (goAhead
                && password.isNotEmpty()
                && password != getString(R.string.hidden_password)
                && password != Preferences.password)
                Preferences.password = password
        }

        newAccountButton.setOnClickListener {
            //Save username
            var goAhead = true
            if (Preferences.username != null){
                AlertDialog.Builder(this).apply {
                    setMessage("TODO blabla change new user")

                    setNegativeButton("CANCEL", DialogInterface.OnClickListener {_, _ -> goAhead = false })
                }.create().apply{
                    setTitle("TIETEN")
                    show()
                }
            }
            val username = usernameEditText.text.toString()
            if (username.isEmpty())
                goAhead = false


            var goAhead2 = true
            if (Preferences.password != null){
                AlertDialog.Builder(this).apply {
                    setMessage("TODO blabla change paaswoord")

                    setNegativeButton("CANCEL", DialogInterface.OnClickListener {_, _ -> goAhead = false })
                }.create().apply{
                    setTitle("TIETEN")
                    show()
                }
            }
            val password = passwordEditText.text.toString()
            if (password.isEmpty()
                || password == getString(R.string.hidden_password)
                || password == Preferences.password)
                goAhead2 = false



            if (goAhead && goAhead2){
                val encodedPassword = with (MessageDigest.getInstance("MD5")){
                    update(password.toByteArray())
                    digest()
                }
                launch{
                    if (Cloud.createNewUser(username, encodedPassword)) {
                        Preferences.password = password
                        Preferences.username = username
                        longToast("Yay updated, user is now ${Preferences.username}")
                        this@LoginActivity.finish()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }
}
