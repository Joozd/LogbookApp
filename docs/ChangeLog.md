# 1.0: (no name)
- Minimum Viable Product

# 1.1: Alpaca
- Switched from LiveData to Flow, 
- many other improvements

# 1.2: Beaver
### Name inspired by watching too many [post10](https://www.youtube.com/channel/UCsCNU-ptlze2tqAJSDeVGNQ) unclogging videos
##### Removed Cloud, causing lots of related changes
- Started this changelog
- No longer sync flights to/from Cloud
- Email storage on server is still here, but works different(see [email](howStuffWorks/email.md))
- Email Backup now works different (see [email](howStuffWorks/backup_email.md))
- All user account logic is removed, except for parts needed for migrating email.

##### fixed issues
- \#1: Many logins to server when creating new user
- \#2: Augmented crew times standard value not used
- \#3: On a new install, enabling calendar sync requires clicking "calendar on this device" twice
- \#5: Submitting feedback should close feedback activity
- \#6: Saving a "simulator" flight saves PIC name which is not in the dialog
- \#8: Clarify that augmented takeoff/landing should include taxi time 
- \#10: Autocomplete text for names in EditFlightFragment unreadable in dark mode
- \#11: Manual backup results in a warning "You have not backed up up for 0 days"
- \#12: Auto-backup while backup dialog is open doesn't close backup dialog
- \#13: Total Times per type are incorrect

##### Work done under the hood
A lot of things under the hood, new implementation of uyndo/redo, a new system for displaying
messages to user, improved version tracking, and a LOT of other things. 

If you really want to know, look at all the github commits...

##### miscellaneous things like removing dead code and cleaning up

### 1.2.1:
- Added support for KLM ICA monthly overviews

### 1.2.2:
- Improved "other names" dialog
### 1.2.3:
- Errors when parsing imported files now no longer crash the app but give an error message.
- Fix: KLC roster no longer crashes when a PSN starts with zero's
### 1.2.4:
- General improvements
### 1.2.5:
- Fix for "Being PF but not doing takeoff/landing causes issues in takeoff/landing dialog"
- library updates