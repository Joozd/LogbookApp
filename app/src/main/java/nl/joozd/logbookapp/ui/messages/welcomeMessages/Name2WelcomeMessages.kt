package nl.joozd.logbookapp.ui.messages.welcomeMessages

import nl.joozd.logbookapp.R

object Name2WelcomeMessages: WelcomeMessagesData<DialogContent>() {
    override val currentVersion = 1

    override val messageToShow: Map<Pair<Int, Int>, DialogContent> = buildMap {
        this[0 to 1] = DialogContent(R.string.name2_dialog_0_to_1_title, R.string.name2_dialog_0_to_1_content)
    }



}