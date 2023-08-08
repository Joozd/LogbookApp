package nl.joozd.logbookapp.ui.messages.welcomeMessages

abstract class WelcomeMessagesData<T> {
    abstract val currentVersion: Int
    protected abstract val messageToShow: Map<Pair<Int, Int>, T> // map of [oldVersion to newVersion] to the data to be shown.

    fun getMessageForVersion(previousVersion: Int): T? =
        if (previousVersion == currentVersion) null
        else messageToShow[previousVersion to currentVersion]
}