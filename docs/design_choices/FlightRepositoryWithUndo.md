#FlightRepository with Undo.
This document is to explain why, not how.

###Basic principle
I am keeping track of changes in a mp of `FlightState(val before: Flight?, val after: Flight?)`
objects. These are keps in two stacks, undo and redo. 

The reason I didn't opt for an `Undoable` interface is that I am only tracking changes to saved 
flights, and this way I can easily combine multiple undoable actions into a single object, as I am 
only looking at changes in flights before and after.

###Invalidating redo stack
To prevent undefined behaviour, whwnever an undo item isa dded from any other place than a redo item 
being redone, the redo stack is cleared. This makes sure it is always clear when will be 
undone/redone when a button is clicked and prevents edge cases where redoing might overwrite 
something that has just been done (because, for instance, undo/redo references by flight ID and 
something might happen to a flight which ends up having the same ID. I can't really think of a case 
where that might happen but it's better to be sure I guess.)