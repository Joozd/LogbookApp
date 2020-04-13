/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import kotlinx.coroutines.*

import nl.joozd.logbookapp.R

import nl.joozd.logbookapp.data.room.Repository
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel

@Deprecated ("use EditFlightFragment")
class EditFlightNew : Fragment(), CoroutineScope by MainScope() {
    companion object{
        const val TAG = "EditFlightNew"
    }
    private val viewModel: JoozdlogViewModel by viewModels()
    private val repository = Repository.getInstance()
    private val supportFragmentManager: FragmentManager by lazy { requireActivity().supportFragmentManager }
    private var autoUpdateJob: Job = Job()
    private var updatingFlight: Job = Job()

/*


    private val aircraftDb = AircraftDb()
    // private val signatureDb = SignatureDb()
    private lateinit var thisView: View
    private var originalFlight: Flight? = null
    private val lastCompletedFlight: Deferred<Flight> = async(Dispatchers.IO) { mostRecentCompleteFlight(repository.requestValidFlights()) }
    private var date: LocalDate = LocalDate.now()
    var namesWorker: NamesWorker? = null // to be filled by calling function after
    private val icaoIataMap = async { repository.getIcaoToIataMap() }
    private var customAirports = emptyList<Airport>() // = ((flightDb.requestAllFlights().map {it.orig} + flightDb.requestAllFlights().map {it.dest}).distinct().filter {it !in airportDb.requestAllAirports().map {a -> a.ident}}).map{name -> makeAirport(ident=name, name = "User airport")}
    private var previousText = "" // to be used by all handlers to pass old entry around
    private var initialSet = true
    private var exiting: Boolean = false

    private var flightInfoText: TextView? = null
    private var flightFlightID: TextView? = null
    private var flightDateField: EditText? = null
    private var flightFlightNumberField: EditText? = null
    private var flightFlightNumberWrapper: TextInputLayout? = null
    private var flightAircraftField: EditText? = null
    private var flightOrigSelector: ImageView? = null
    private var flightOrigField: EditText? = null
    private var flightOrigWrapper: TextInputLayout? = null
    private var flightDestField: EditText? = null
    private var flightDestWrapper: TextInputLayout? = null
    private var flightDestSelector: ImageView? = null
    private var flighttOutStringField: EditText? = null
    private var flighttOutStringWrapper: TextInputLayout? = null
    private var flighttInStringField: EditText? = null
    private var flighttInStringWrapper: TextInputLayout? = null
    private var flightTakeoffLandingField: EditText? = null
    private var flightNameField: EditText? = null
    private var flightName2Field: EditText? = null
    private var flightRemarksField: EditText? = null
    private var signSelector: TextView? = null
    private var simSelector: TextView? = null
    private var dualSelector: TextView? = null
    private var instructorSelector: TextView? = null
    private var picusSelector: TextView? = null
    private var picSelector: TextView? = null
    private var pfSelector: TextView? = null

    private var flightCancelButton2: TextView? = null
    private var flightSavebutton: TextView? = null
    private var flightInfoDataLayout: ConstraintLayout? = null
    private var flightInfoLayout: ConstraintLayout? = null
    private var flightDateSelector: ImageView? = null
    private var flightAcRegSelector: ImageView? = null
    private var flightTakeoffLandingSelector: ImageView? = null
    private var flightName2Selector: ImageView? = null
    private var flightNameSelector: ImageView? = null
    private var flightTakeoffLandingWrapper: TextInputLayout? = null
    private var flightNumberText: TextView? = null
    private var origText: TextView? = null
    private var destText: TextView? = null
    private var tInText: TextView? = null
    private var takeoffLandingText: TextView? = null
    private var autoFillCheckBox: CheckBox? = null
    private var autoWasOn: Boolean = false

    // augmentedFactor is the percentage of the total flight that is being logged in case of augmented crew
    // for the purpose of taking an equal part as night-, IFR  and instruction time
    private var augmentedFactor: Double = 1.0

    /**
     * The flight that is being edited. If nothing set, this will be a new flight.
     * If it is changed it will set Views accordingly
     */

    //        flight = reverseFlight(lastCompletedFlight.await(), repository.highestFlightId() + 1) }

    var flight: Flight = Flight(-3)
    set(flightToSet) {
        updatingFlight.cancel()
        launch {
            updatingFlight.join()
            updatingFlight = launch {
                if (flightToSet.flightID != -3) { // do not trigger on initial set
                    Log.d(TAG, flightToSet.toString())
                    if (initialSet) { // do this only the first time this fragment is being filled with a flight
                        Log.d(TAG, "InitialSet for $flightToSet")

                        val lastCompletedFlight = lastCompletedFlight.await()
                        field = if (flightToSet.planned) {
                            val workingFlight =
                                flightAirportToIataIfThatIsSet(flightToSet, icaoIataMap.await())
                            workingFlight.copy(
                                name = if (workingFlight.name.isEmpty()) lastCompletedFlight.name else workingFlight.name,
                                name2 = if (workingFlight.name2.isEmpty()) lastCompletedFlight.name2 else workingFlight.name2,
                                isPIC = lastCompletedFlight.isPIC,
                                aircraft = if (workingFlight.registration.isEmpty()) lastCompletedFlight.aircraft else workingFlight.aircraft,
                                registration = if (workingFlight.registration.isEmpty()) lastCompletedFlight.registration else workingFlight.registration
                            )
                        } else flightAirportToIataIfThatIsSet(flightToSet, icaoIataMap.await())


                        autoWasOn = flightToSet.autoFill > 0
                        initialSet = false
                        lifecycleScope.launch(Dispatchers.IO) {
                            flight.actualAircraft =
                                aircraftDb.searchRegAndType(reg = flight.registration).firstOrNull()
                        }
                    }
                }
                else //This will trigger on updates from inside this fragment
                    field = if (Preferences.useIataAirports)
                        flightAirportToIataIfThatIsSet(flightToSet, icaoIataMap.await())
                        else flightToSet
            }

            ///set autmentedFactor by dividing corrected duration by total duration
            augmentedFactor =
                flightToSet.correctedDuration.toMinutes() / flightToSet.duration.toMinutes()
                    .toDouble()
            // this will be the updated flight, where updating it will also update displays if done automatically
            if (originalFlight == null) originalFlight = flightToSet
            // set fields of view if it is initialized (ie all references are not null

            flightInfoText?.text = "Edit Flight"
            flightFlightID?.text = flight.flightID.toString()
            flightDateField?.setText(flight.date)
            flightFlightNumberField?.setText(flight.flightNumber)
            flightAircraftField?.setText(if (!flight.sim) ("${flight.registration}(${flight.aircraft})") else flight.aircraft)
            flightOrigField?.let {
                it.setText(flight.orig)
                if (it.text.isNotEmpty()) it.setTypeface(null, Typeface.BOLD)
            }
            flightDestField?.let {
                it.setText(flight.dest)
                if (it.text.isNotEmpty()) it.setTypeface(null, Typeface.BOLD)
            }
            flighttOutStringField?.setText(if (flight.sim) "${flight.simTime / 60}:${flight.simTime % 60}" else flight.timeOutString)
            flighttInStringField?.setText(flight.timeInString)
            flightInfoText?.let {// check if a field is not null, in this case all should be not null so can safely !! them
                if (flight.sim) makeItSim()
            }
            flightTakeoffLandingField?.setText(flight.takeoffLanding)
            flightNameField?.setText(flight.name)
            flightName2Field?.setText(flight.name2)
            flightRemarksField?.setText(flight.remarks)
            if (flight.sim) simSelector?.showAsActive() else simSelector?.showAsInactive()
            if (flight.dual) dualSelector?.showAsActive() else dualSelector?.showAsInactive()
            if (flight.instructor) instructorSelector?.showAsActive() else instructorSelector?.showAsInactive()
            if (flight.picus) picusSelector?.showAsActive() else picusSelector?.showAsInactive()
            if (flight.pic) picSelector?.showAsActive() else picSelector?.showAsInactive()
            if (flight.pf) pfSelector?.showAsActive() else pfSelector?.showAsInactive()
            if (flight.signature.isNotEmpty()) signSelector?.showAsActive() else signSelector?.showAsInactive()

            autoFillCheckBox?.isChecked = flight.autoFill > 0
            date = flightToSet.tOut.toLocalDate()
            Log.d("EditFlightNewDEBUG", "4")
            Log.d("nightTime", "${flight.nightTime}")
            if (activity?.currentFocus is TextInputEditText) {
                (activity?.currentFocus as TextInputEditText).selectAll()
            }
        }
    }





    /**
     * Fill custom Airports list
     */
    init {

    }


    /************************************************************************
     * Interfaces below here                                                *
     ************************************************************************/

    class OnSave (private val f: (flightToSave: Flight, oldFlight: Flight) ->Unit){ // oldFlight =  flight before changes, to undo if needed
        fun save(flightToSave: Flight, oldFlight: Flight){
            f(flightAirportToIcao(flightToSave), oldFlight)
        }
    }
    class OnCancel (private val f: (flightToSave: Flight, oldFlight: Flight) ->Unit){ // oldFlight =  flight before changes, to undo if needed
        fun cancel(flightToSave: Flight, oldFlight: Flight){
            f(flightToSave, oldFlight)
        }
    }
    class CreatedListener(private val f: () -> Unit){
        fun onCreated(){
            f()
        }
    }

    var onCancel: OnCancel? = null
    var onSave: OnSave? = null
    var createdListener: CreatedListener? = null




 */
    /************************************************************************
     *  OnCreateView and other overrides below here                         *
     ************************************************************************/


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        Log.d("EditFlightNewDEBUG", "1")
        return inflater.inflate(R.layout.edit_flight, container, false)
        // thisView = inflater.inflate(R.layout.flight_edit_new, container, false)

        /*
        flightInfoText = thisView.findViewById(R.id.flightInfoText)
        flightFlightID = thisView.findViewById(R.id.flightFlightID)
        flightDateField = thisView.findViewById(R.id.flightDateField)
        flightFlightNumberField = thisView.findViewById(R.id.flightFlightNumberField)
        flightFlightNumberWrapper = thisView.findViewById(R.id.flightFlightNumberWrapper)

        flightAircraftField = thisView.findViewById(R.id.flightAircraftField)

        flightOrigSelector = thisView.findViewById(R.id.flightOrigSelector)
        flightOrigField = thisView.findViewById(R.id.flightOrigField)
        flightOrigWrapper = thisView.findViewById(R.id.flightOrigWrapper)
        flightDestField = thisView.findViewById(R.id.flightDestField)
        flightDestWrapper = thisView.findViewById(R.id.flightDestWrapper)
        flightDestSelector = thisView.findViewById(R.id.flightDestSelector)

        flighttOutStringField = thisView.findViewById(R.id.flighttOutStringField)
        flighttOutStringWrapper = thisView.findViewById(R.id.flighttOutStringWrapper)
        flighttInStringField = thisView.findViewById(R.id.flighttInStringField)
        flighttInStringWrapper = thisView.findViewById(R.id.flighttInStringWrapper)

        flightTakeoffLandingField = thisView.findViewById(R.id.flightTakeoffLandingField)
        flightNameField = thisView.findViewById(R.id.flightNameField)
        flightName2Field = thisView.findViewById(R.id.flightName2Field)
        flightName2Selector = thisView.findViewById(R.id.flightName2Selector)
        flightNameSelector = thisView.findViewById(R.id.flightNameSelector)
        flightRemarksField = thisView.findViewById(R.id.flightRemarksField)

        signSelector = thisView.signSelector
        simSelector = thisView.findViewById(R.id.simSelector)
        dualSelector = thisView.findViewById(R.id.dualSelector)
        instructorSelector = thisView.findViewById(R.id.instructorSelector)
        picusSelector = thisView.findViewById(R.id.picusSelector)
        picSelector = thisView.findViewById(R.id.picSelector)
        pfSelector = thisView.findViewById(R.id.pfSelector)

        flightCancelButton2 = thisView.findViewById(R.id.flightCancelButton2)
        flightSavebutton = thisView.findViewById(R.id.flightSaveButton)
        flightInfoDataLayout = thisView.findViewById(R.id.flightInfoDataLayout)
        flightInfoLayout = thisView.findViewById(R.id.flightInfoLayout)
        flightDateSelector = thisView.findViewById(R.id.flightDateSelector)
        flightAcRegSelector = thisView.findViewById(R.id.flightAcRegSelector)
        flightTakeoffLandingSelector = thisView.findViewById(R.id.flightTakeoffLandingSelector)
        flightTakeoffLandingWrapper = thisView.findViewById(R.id.flightTakeoffLandingWrapper)

        flightNumberText = thisView.findViewById(R.id.flightNumberText)
        origText = thisView.findViewById(R.id.origText)
        destText = thisView.findViewById(R.id.destText)
        takeoffLandingText = thisView.findViewById(R.id.takeoffLandingText)
        autoFillCheckBox = thisView.findViewById(R.id.autoFillCheckBox)
        (flightInfoText?.background as GradientDrawable).colorFilter = PorterDuffColorFilter(requireActivity().getColorFromAttr(android.R.attr.colorPrimary), PorterDuff.Mode.SRC_IN) // set background color to bakground with rounded corners




        launch {
            updatingFlight.join()
            flight = flight // reassign to itself so setter gets triggered and fields filled
        }



        return thisView

         */
    }
/*
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val flightNameFieldAutoComplete = CustomAutoComplete(defaultItems = listOf("SELF"))
        val flightName2FieldAutoComplete = CustomAutoComplete(defaultItems = listOf("SELF"))


        // onclick for functions bar only switches value, reload will (should) take care of layout
        pfSelector?.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            flight = flight.copy(isPF = if (flight.pf) 0 else flight.duration.toMinutes().toInt())
            if (autoFillCheckBox!!.isChecked) launchAutoUpdate()
        }
        dualSelector?.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            flight =
                flight.copy(isDual = if (flight.dual) 0 else flight.duration.toMinutes().toInt())
        }
        instructorSelector?.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            flight = flight.copy(
                isInstructor = if (flight.instructor) 0 else flight.duration.toMinutes().toInt()
            )
        }
        picusSelector?.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            flight =
                flight.copy(isPICUS = if (flight.picus) 0 else flight.duration.toMinutes().toInt())
        }

        picSelector?.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            flight = flight.copy(isPIC = if (flight.pic) 0 else flight.duration.toMinutes().toInt())
        }
        simSelector?.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            if (!flight.sim) autoWasOn = flight.autoFill > 0
            flight = flight.copy(
                isSim = if (flight.sim) 0 else 1,
                simTime = if (flight.sim) 0 else 210,
                autoFill = if (flight.sim && autoWasOn) 1 else 0
            ) // reloading will trigger makeItSim() but not MakeItNotSim()
            if (!flight.sim) makeItNotSim()
        }

        thisView.signSelector.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            val signatureDialog = SignatureDialog().apply {
                setOnSignedListener {
                    Log.d(TAG, it)
                    flight = flight.copy(signature = it)
                }
            }
            if (flight.signature.isNotEmpty()) signatureDialog.signature = flight.signature
            supportFragmentManager.beginTransaction()
                .add(R.id.mainActivityLayout, signatureDialog)
                .addToBackStack(null)
                .commit()
        }



        autoFillCheckBox?.setOnCheckedChangeListener { _, _ ->
            if (autoFillCheckBox!!.isChecked) launchAutoUpdate()
        }
        val timeOnClickListener = View.OnClickListener {
            // Get dateDialog, update flight when a date is picked
            // As times are the same, just change dates in those times

            val timePickerFragment = TimePickerOld()
            timePickerFragment.aircraftDb = aircraftDb
            timePickerFragment.currentFlight = flight.copy()


            timePickerFragment.setOnSaveListener {
                flight = it
            }
            Log.d("after setting nightTime", "${flight.nightTime}")

            supportFragmentManager.beginTransaction()
                .add(R.id.mainActivityLayout, timePickerFragment)
                .addToBackStack(null)
                .commit()
        }



        thisView.flighttOutSelector.setOnClickListener(timeOnClickListener)
        thisView.flighttInSelector.setOnClickListener(timeOnClickListener)


        val dateOnClickListener = View.OnClickListener {
            // Get dateDialog, update flight when a date is picked
            // As times are the same, just change dates in those times

            val datePickerFragment = DatePickerFragment()
            datePickerFragment.onDatePicked = DatePickerFragment.OnDatePicked {
                date = it
                if ((flighttOutStringField!!.text.isNotEmpty()) && (flighttInStringField!!.text.isNotEmpty())) {
                    //flight = flight.copy(
                    //    timeOut = flight.tOut.plusDays(dateDelta.days.toLong()).toInstant(ZoneOffset.UTC).epochSecond,
                    //    timeIn = flight.tOut.plusDays(dateDelta.days.toLong()).toInstant(ZoneOffset.UTC).epochSecond
                    //)
                    if (autoFillCheckBox!!.isChecked) launchAutoUpdate()
                }
            }
            datePickerFragment.show(requireActivity().supportFragmentManager, "datePicker")
        }

        flightDateField?.setOnClickListener(dateOnClickListener)

        flightDateSelector?.setOnClickListener(dateOnClickListener)

        flightFlightNumberField?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            with(v as TextInputEditText) {
                var flightnumber = text.toString()
                var flightnumberDigits = ""
                while (flightnumber.last().isDigit()) {
                    flightnumberDigits = flightnumber.last() + flightnumberDigits
                    flightnumber = flightnumber.dropLast(1)
                }
                if (hasFocus) {
                    previousText = text.toString()
                    this.setText("")
                    this.append(flightnumber)
                } else {
                    if (text.toString() == flightnumber) setText(previousText)
                    if (text.toString() != previousText)
                        flight = flight.copy(flightNumber = text.toString())
                }
            }
        }


        flighttOutStringField?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                with(v as TextInputEditText) {
                    previousText = this.text.toString()
                    v.selectAll()
                }
            } else {
                with(v as TextInputEditText) {
                    val currentText = this.text.toString()
                    if (currentText != previousText) {
                        Log.d(
                            "Values",
                            "$currentText, $previousText, ${currentText == previousText}"
                        )
                        if (!(("([01]\\d|2[0-3]):[0-5]\\d".toRegex()
                                .containsMatchIn(currentText) && currentText.length == 5)
                                    || ("([01]\\d|2[0-3])[0-5]\\d".toRegex().containsMatchIn(
                                currentText.padStart(
                                    4,
                                    '0'
                                )
                            ) && currentText.length <= 4))
                            || (currentText == "")
                        )
                            this.setText(previousText) // not a valid new entry, so no update on flight
                        else {
                            this.setText(
                                if (currentText.length < 5) "${currentText.padStart(
                                    4,
                                    '0'
                                )
                                    .slice(0..1)}:${currentText.padStart(4, '0')
                                    .slice(2..3)}" else currentText
                            )
                            flight = if (flight.isSim == 0) {
                                flight.copy(
                                    timeOut = LocalDateTime.of(
                                        date,
                                        this.text.toString().makeLocalTime()
                                    ).toInstant(ZoneOffset.UTC).epochSecond
                                )
                            } else {
                                // TODO: Flight is sim so this time is sim time
                                val simTime = (this.text!!.toString().slice(0..1)
                                    .toInt() * 60) + this.text.toString().slice(3..4).toInt()
                                flight.copy(simTime = simTime)
                            }
                        }
                        if (this@EditFlightNew.autoFillCheckBox!!.isChecked) launchAutoUpdate()
                    }
                }

            }

        }
        flighttInStringField?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                with(v as TextInputEditText) {
                    previousText = this.text.toString()
                    v.selectAll()
                }
            } else {
                with(v as TextInputEditText) {
                    val currentText = this.text.toString()
                    if (currentText != previousText) {
                        if (!(("([01]\\d|2[0-3]):[0-5]\\d".toRegex()
                                .containsMatchIn(currentText) && currentText.length == 5)
                                    || ("([01]\\d|2[0-3])[0-5]\\d".toRegex().containsMatchIn(
                                currentText.padStart(
                                    4,
                                    '0'
                                )
                            ) && currentText.length <= 4))
                            || (currentText == "")
                        )
                            this.setText(previousText) // not a valid new entry, so no update on flight
                        else {
                            this.setText(
                                if (currentText.length < 5) "${currentText.padStart(
                                    4,
                                    '0'
                                )
                                    .slice(0..1)}:${currentText.padStart(4, '0')
                                    .slice(2..3)}" else currentText
                            )
                            val timeToCheck =
                                LocalDateTime.of(date, this.text.toString().makeLocalTime())
                            val timeIn = if (Duration.between(
                                    flight.tOut,
                                    timeToCheck
                                ).seconds <= 0
                            ) timeToCheck.plusDays(1).toInstant(ZoneOffset.UTC).epochSecond
                            else timeToCheck.toInstant(ZoneOffset.UTC).epochSecond
                            flight = flight.copy(timeIn = timeIn)
                        }
                        if (this@EditFlightNew.autoFillCheckBox!!.isChecked) launchAutoUpdate()
                    }

                }
            }

        }

        flightOrigSelector?.setOnClickListener {
            val airportDialog = AirportPickerOld()
            airportDialog.selectedAirportIdent = flight.orig
            airportDialog.onSelectListener = AirportPickerOld.AirportSelectedListener {
                flight = flight.copy(orig = it.ident)
                if (autoFillCheckBox!!.isChecked) launchAutoUpdate()
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.mainActivityLayout, airportDialog)
                .addToBackStack(null)
                .commit()
        }

        flightDestSelector?.setOnClickListener {
            val airportDialog = AirportPickerOld()
            airportDialog.selectedAirportIdent = flight.dest
            airportDialog.onSelectListener = AirportPickerOld.AirportSelectedListener {
                flight = flight.copy(dest = it.ident)
                if (autoFillCheckBox!!.isChecked) launchAutoUpdate()
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.mainActivityLayout, airportDialog)
                .addToBackStack(null)
                .commit()
        }

        flightOrigField?.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> // //TODO ICAO or IATA

                if (hasFocus) {
                    with(v as TextInputEditText) {
                        previousText = text.toString()
                    }
                } else {
                    /*
                launch {
                    with(v as TextInputEditText) {
                        val currentText = this.text.toString()
                        val foundAirport =
                            if (currentText != "") customAirports.firstOrNull { it.ident == currentText }
                                ?: viewModel.searchAirport(currentText) else null
                        if (foundAirport != null) {
                            Log.d("foundAirport", foundAirport.ident)
                            if (flight.orig != foundAirport.ident) flight =
                                flight.copy(orig = foundAirport.ident)
                        } else flight = flight
                    }
                }
                if (autoFillCheckBox!!.isChecked) launchAutoUpdate()
            }
            */
                }
                flightDestField?.onFocusChangeListener =
                    View.OnFocusChangeListener { v, hasFocus -> // //TODO ICAO or IATA
                        if (hasFocus) {
                            with(v as TextInputEditText) {
                                previousText = text.toString()
                            }
                        } else {
                            launch {
                                with(v as EditText) {
                                    val currentText = this.text.toString()
                                    val foundAirport =
                                        if (currentText != "") customAirports.firstOrNull { it.ident == currentText }
                                            ?: repository.searchAirport(currentText) else null
                                    if (foundAirport != null) {
                                        if (flight.dest != foundAirport.ident) flight =
                                            flight.copy(dest = foundAirport.ident)
                                    } else flight = flight
                                }
                                //if (autoFillCheckBox!!.isChecked) flight = autoValues()
                            }
                        }
                    }

                flightAcRegSelector?.setOnClickListener {
                    activity?.currentFocus?.clearFocus()
                    val aircraftPicker = AircraftPicker()
                    aircraftPicker.flightSelectedListener =
                        AircraftPicker.FlightSelectedListener { ac ->
                            flightAircraftField!!.setText("${ac.registration}(${ac.model})")
                            flight =
                                flight.copy(aircraft = ac.model, registration = ac.registration)

                        }

                    supportFragmentManager.beginTransaction()
                        .add(R.id.mainActivityLayout, aircraftPicker)
                        .addToBackStack(null)
                        .commit()
                    aircraftPicker.setAircraft(flightAircraftField?.text.toString())
                }
                flightAircraftField?.onFocusChangeListener =
                    View.OnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            with(v as TextInputEditText) {
                                previousText = text.toString()
                            }
                        } else {
                            with(v as TextInputEditText) {
                                val currentText = this.text.toString()
                                val foundAircraftList =
                                    if (currentText != "" && currentText != previousText) aircraftDb.searchRegAndType(
                                        currentText,
                                        ""
                                    ) else emptyList() // IO ops fast enough on my phone, check on slower ones
                                Log.d(
                                    "foundAircraftList is empty",
                                    "${foundAircraftList.isEmpty()}"
                                )
                                val foundAircraft = foundAircraftList.firstOrNull()
                                val foundAircraftText: String =
                                    if (foundAircraft != null)
                                        (if (flight.isSim > 0) foundAircraft.model
                                        else "${foundAircraft.registration}(${foundAircraft.model})")
                                    else previousText
                                if (foundAircraftText != previousText) flight = flight.copy(
                                    aircraft = foundAircraft?.model ?: "",
                                    registration = foundAircraft?.registration ?: v.text.toString()
                                )
                            }
                        }
                    }

                val takeoffLandingOnclickListener =
                    View.OnClickListener { // TODO just pass a copy of WorkingFlight back and forth
                        activity?.currentFocus?.clearFocus()
                        val landingsPicker = LandingsPicker()
                        landingsPicker.currentLandingData = LandingsCounter(
                            takeOffDay = flight.takeOffDay,
                            landingDay = flight.landingDay,
                            takeOffNight = flight.takeOffNight,
                            landingNight = flight.landingNight,
                            autoland = flight.autoLand
                        )
                        landingsPicker.onSaveListener = LandingsPicker.OnSaveListener { ld ->
                            with(ld) {
                                autoFillCheckBox?.isChecked = false
                                flight = flight.copy(
                                    takeOffDay = takeOffDay,
                                    landingDay = landingDay,
                                    takeOffNight = takeOffNight,
                                    landingNight = landingNight,
                                    autoLand = autoland,
                                    autoFill = 0
                                )
                            }
                        }
                        supportFragmentManager.beginTransaction()
                            .add(R.id.mainActivityLayout, landingsPicker).addToBackStack(null)
                            .commit()
                    }
                flightTakeoffLandingSelector?.setOnClickListener(takeoffLandingOnclickListener)
                flightTakeoffLandingField?.setOnClickListener(takeoffLandingOnclickListener)

                flightNameSelector?.setOnClickListener {
                    activity?.currentFocus?.clearFocus()
                    if (namesWorker?.isInitialized == true) {       // not if no namesworker or not initialized
                        val namesDialog = NamesDialog()
                        namesDialog.selectOnlyOne = true
                        namesDialog.namesWorker = namesWorker
                        namesDialog.onSelectListener = NamesDialog.NamesSelectedListener { names ->
                            flightNameField?.setText(names)
                            flight = flight.copy(name = names)
                        }
                        namesDialog.previousNames = flightNameField?.text.toString()
                        supportFragmentManager.beginTransaction()
                            .add(R.id.mainActivityLayout, namesDialog)
                            .addToBackStack(null)
                            .commit()
                    } else activity?.toast("Names worker not ready, please try again")
                }

                flightNameField?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        with(v as TextInputEditText) {
                            previousText = text.toString()
                        }
                    } else {
                        with(v as EditText) {
                            if (text.toString() != previousText) {
                                val foundName: String?
                                if (text.toString().isNotEmpty()) {
                                    foundName = namesWorker?.getOneName(text.toString())
                                    foundName?.let { setText(it) }
                                } else foundName = null
                                flight = flight.copy(name = foundName ?: text.toString())
                            }
                        }
                    }
                }
                // do this after onFocusChangeListener due to it overwriting closing logic
                // alternatively, call flightNameFieldAutoComplete.removeList() in new listener.
                flightNameField?.let {
                    flightNameFieldAutoComplete.connectToEditText(it)
                    flightNameFieldAutoComplete.maxItems = 5
                }

                flightName2Field?.onFocusChangeListener =
                    View.OnFocusChangeListener { v, hasFocus -> // TODO make things autocomplete after comma on putting in a comma (so guess change this to onTextChangedlistener)
                        if (hasFocus) {
                            with(v as TextInputEditText) {
                                previousText = text.toString()
                            }
                        } else {
                            with(v as EditText) {
                                if (text.toString() != previousText) {
                                    val foundName: String?
                                    if (text.toString().isNotEmpty()) {
                                        foundName = namesWorker?.getOneName(text.toString())
                                        foundName?.let { setText(it) }
                                    } else foundName = null
                                    flight = flight.copy(name2 = foundName ?: text.toString())
                                }
                            }
                        }
                    }
                flightName2Field?.let {
                    flightName2FieldAutoComplete.connectToEditText(it)
                    flightName2FieldAutoComplete.maxItems = 5
                }
                flightName2Selector?.setOnClickListener {
                    activity?.currentFocus?.clearFocus()
                    if (namesWorker?.isInitialized == true) {       // not if no namesworker or not initialized
                        val namesDialog = NamesDialog()
                        namesDialog.namesWorker = namesWorker
                        namesDialog.onSelectListener = NamesDialog.NamesSelectedListener { names ->
                            flightName2Field?.setText(names)
                            flight = flight.copy(name2 = names)
                        }
                        namesDialog.previousNames = flightName2Field?.text.toString()
                        supportFragmentManager.beginTransaction()
                            .add(R.id.mainActivityLayout, namesDialog)
                            .addToBackStack(null)
                            .commit()
                    } else activity?.toast("Names worker not ready, please try again")
                }


                flightRemarksField?.setOnEditorActionListener { thisview, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        val imm: InputMethodManager =
                            thisview.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(thisview.windowToken, 0)
                        thisview.clearFocus()
                    }
                    true
                }
                flightRemarksField?.onFocusChangeListener =
                    View.OnFocusChangeListener { v, hasFocus ->

                        if (hasFocus) {
                            with(v as TextInputEditText) {
                                previousText = text.toString()
                            }
                        } else {
                            with(v as TextInputEditText) {
                                val currentText = text.toString()
                                if (currentText != previousText) flight =
                                    flight.copy(remarks = currentText)
                            }
                        }
                    }


                flightCancelButton2?.setOnClickListener {
                    exiting = true
                    activity?.currentFocus?.clearFocus()
                    onCancel?.cancel(flight, originalFlight ?: flight)
                    supportFragmentManager.popBackStack()
                }
                flightInfoLayout?.setOnClickListener {
                    exiting = true
                    activity?.currentFocus?.clearFocus()
                    onCancel?.cancel(flight, originalFlight ?: flight)
                    supportFragmentManager.popBackStack()
                }
                flightSavebutton?.setOnClickListener {
                    launch {
                        exiting = true
                        activity?.currentFocus?.clearFocus()
                        if (autoFillCheckBox!!.isChecked) //launchAutoUpdate()

                        // make sure flags are set ok
                            if (flight.sim) flight = flight.copy(
                                isPF = 0,
                                isPIC = 0,
                                isPICUS = 0,
                                isDual = 0,
                                isInstructor = 0,
                                isCoPilot = 0
                            )
                            else {
                                //assume aircraft is multicrew if it is set to that or if augmented crew is set to at least 2 pilots
                                val multiCrew =
                                    Crew.of(flight.augmentedCrew).crewSize >= 2 || flight.actualAircraft?.multipilot ?: 0 > 0
                                if (multiCrew && !flight.pic) flight = flight.copy(isCoPilot = 1)
                            }
                        //add timeStamp
                        updatingFlight.join()
                        flight =
                            flight.copy(timeStamp = Instant.now().epochSecond + Preferences.serverTimeOffset)
                        updatingFlight.join()


                        //onSave?.save(checkIfStillPlanned(flight.asSimIfNeeded()), originalFlight ?: flight)
                        supportFragmentManager.popBackStack()
                    }
                }
                Log.d("EditFlightNewDEBUG", "2")
            }
    }


    /*override fun onDestroyView() {
        activity?.currentFocus?.clearFocus()
        if (!exiting) onCancel?.cancel(flight, originalFlight ?: flight)
        super.onDestroyView()
    }



    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

     */


    /************************************************************************
     *  Helper functions below here                                         *
     ************************************************************************/



    private fun makeItSim(){
        flighttOutStringWrapper?.hint=getString(R.string.simtTime)
        flighttOutStringField?.hint=getString(R.string.simtTime)
        autoFillCheckBox?.isChecked = false
        autoFillCheckBox?.isEnabled = false
        flighttInStringWrapper?.visibility=View.GONE
        flightFlightNumberWrapper?.visibility=View.GONE
        dualSelector?.visibility=View.GONE
        instructorSelector?.visibility=View.GONE
        picusSelector?.visibility=View.GONE
        picSelector?.visibility=View.GONE
        pfSelector?.visibility=View.GONE
        flightOrigSelector?.visibility=View.GONE
        flightOrigWrapper?.visibility=View.GONE
        flightDestWrapper?.visibility=View.GONE
        flightDestSelector?.visibility=View.GONE
        flightTakeoffLandingWrapper?.visibility=View.GONE
        flightTakeoffLandingSelector?.isEnabled=false
        //TODO probably more things will happen
    }
    private fun makeItNotSim() {
        autoFillCheckBox?.isEnabled = flight.autoFill > 0 // TODO got default value for this ro some other smarter way // did a quick fix
        flighttOutStringWrapper?.hint = getString(R.string.timeOut)
        flighttOutStringField?.hint = getString(R.string.timeOut)
        flighttInStringWrapper?.visibility = View.VISIBLE
        flightFlightNumberWrapper?.visibility = View.VISIBLE
        dualSelector?.visibility = View.VISIBLE
        instructorSelector?.visibility = View.VISIBLE
        picusSelector?.visibility = View.VISIBLE
        picSelector?.visibility = View.VISIBLE
        pfSelector?.visibility = View.VISIBLE
        flightOrigSelector?.visibility=View.VISIBLE
        flightOrigWrapper?.visibility=View.VISIBLE
        flightDestWrapper?.visibility=View.VISIBLE
        flightDestSelector?.visibility=View.VISIBLE
        flightTakeoffLandingWrapper?.visibility=View.VISIBLE
        flightTakeoffLandingSelector?.isEnabled=true
    }

        //TODO do the reverse of makeItSim()

    private suspend fun autoValues(flightToAutoValue: Flight = this.flight): Flight = withContext(Dispatchers.Main) {
        autoFillCheckBox?.let {
            // check if at least one part of the view is found, assume the rest is the same.
            var duration: Duration
            if (flighttOutStringField!!.text.toString()
                    .isNotEmpty() && flighttInStringField!!.text.toString().isNotEmpty()
            ) {
                duration = Duration.between(
                    flighttOutStringField!!.text.toString().makeLocalTime(),
                    flighttInStringField!!.text.toString().makeLocalTime()
                )
                if (duration.seconds < 0) duration = duration.plusDays(1)
            } else return@let flightToAutoValue
            // Calculate landings:
            val twilightCalculator = TwilightCalculator(
                LocalDateTime.of(
                    flight.tOut.toLocalDate(),
                    flight.tOut.toLocalTime()
                )
            )
            val orig = repository.searchAirport(flight.orig)
            val dest = repository.searchAirport(flight.dest)
            if (orig == null || dest == null) return@let flightToAutoValue

            var toDay = 0
            var toNight = 0
            var ldgDay = 0
            var ldgNight = 0
            val nightTime: Int
            val ifrTime: Int

            if (flightToAutoValue.pf) {
                if (twilightCalculator.itIsDayAt(
                        orig,
                        flighttOutStringField!!.text.toString().makeLocalTime()
                    )
                ) toDay = 1 else toNight = 1
                if (twilightCalculator.itIsDayAt(
                        dest,
                        flighttInStringField!!.text.toString().makeLocalTime()
                    )
                ) ldgDay = 1 else ldgNight = 1
            }

            //ifr times will only change if an aircraft is found

            nightTime = ((twilightCalculator.minutesOfNight(
                orig,
                dest,
                flight.tOut,
                flight.tIn
            ) * augmentedFactor) + 0.5).toInt()
            Log.d("nightTime:", "nighttime = $nightTime")
            Log.d(
                "IFR checker:",
                "aircraft ${flightToAutoValue.registration}.isIfr = ${aircraftDb.searchRegAndType(
                    reg = flightToAutoValue.registration
                ).firstOrNull()?.isIfr ?: "null"}"
            )
            ifrTime = if ((aircraftDb.searchRegAndType(reg = flightToAutoValue.registration)
                    .firstOrNull()
                    ?.isIfr ?: 0) > 0
            ) ((duration.toMinutes() * augmentedFactor) + 0.5).toInt() else 0
            return@let flightToAutoValue.copy(
                takeOffDay = toDay,
                takeOffNight = toNight,
                landingDay = ldgDay,
                landingNight = ldgNight,
                ifrTime = ifrTime,
                nightTime = nightTime,
                autoFill = 1
            )
        }
        return@withContext flightToAutoValue // will return same flight if view not created
    }

    private fun launchAutoUpdate(){
        autoUpdateJob.cancel()
        launch{
            autoUpdateJob.join()
            autoUpdateJob = launch{ flight = autoValues() }
        }
    }



    private fun checkIfStillPlanned(f: Flight): Flight {
        val currentTime = LocalDateTime.ofInstant(Calendar.getInstance().toInstant(), ZoneId.of("UTC")).plusMinutes(5) // add 5 minutes so if phone or aircraft time is a few minutes off it won't get stuck at "planned" when immediately filling after flight
        if (f.isPlanned == 0) return if (currentTime < f.tIn) f.copy(isPlanned = 1) else f
        return if (currentTime > f.tIn) f.copy(isPlanned = 0) else f
    }

 */
}