package nl.joozd.logbookapp.ui.messages.welcomeMessages

import nl.joozd.logbookapp.R

object EditFlightFragmentWelcomeMessages: WelcomeMessagesData<DialogContent>() {
    override val currentVersion = 1

    override val messageToShow: Map<Pair<Int, Int>, DialogContent> = buildMap {
        this[0 to 1] = DialogContent(R.string.edit_flight_welcome_title, R.string.edit_flight_welcome_message)
    }
}