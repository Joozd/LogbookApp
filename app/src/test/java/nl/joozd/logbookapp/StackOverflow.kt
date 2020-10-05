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

package nl.joozd.logbookapp

import androidx.lifecycle.*

/**
 * Just here to prevent compiler errors
 */
@Suppress("ClassName")
object myRepository{
    val someData = MutableLiveData(3)
}

abstract class MyClass: ViewModel(){
    private val _myMediator = MediatorLiveData<String>()

    protected abstract val mySource: LiveData<String>

    val myObservable: LiveData<String>
        get() = _myMediator

    // This will cause a NullPointerException at runtime
    init{
        _myMediator.addSource(mySource){ _myMediator.value = it }
    }

    //This should work, but requires this to be called in child class
    protected fun addSources(){
        _myMediator.addSource(mySource){ _myMediator.value = it }
    }
}

abstract class MyFixedClass: ViewModel(){
    private val _myMediator: MediatorLiveData<String> by lazy{
        MediatorLiveData<String>().apply{
            addSource(mySource){ this.value = it }
        }
    }

    protected abstract val mySource: LiveData<String>

    val myObservable: LiveData<String>
        get() = _myMediator
}

class MyChild: MyFixedClass(){
    override val mySource = Transformations.map(myRepository.someData) { it.toString() }
}