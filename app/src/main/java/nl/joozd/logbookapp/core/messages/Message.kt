package nl.joozd.logbookapp.core.messages

interface Message{
    // Any message implementing this interface will not be displayed, instead clear any messages with the same tag (e.g. persistent messages)
    interface NoMessage
}