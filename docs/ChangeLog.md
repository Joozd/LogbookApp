# 1.0: (no name)
- Minimum Viable Product

# 1.1: Alpaca
- Switched from LiveData to Flow, 
- many other improvements

# 1.2: Beaver
### Name inspired by [post10](https://www.youtube.com/channel/UCsCNU-ptlze2tqAJSDeVGNQ)
##### Removed Cloud, causing lots of related changes
- Started this changelog
- No longer sync flights to/from Cloud
- Email storage on server is still here, but works different(see [email](howStuffWorks/email.md))
- Email Backup now works different (see [email](howStuffWorks/backup_email.md))
- All user account logic is removed, except for parts needed for migrating email.

##### Version tracking now with `core.metadata.Version`. 
- This also tracks if it is a new install.

##### fixed issues
- \#5: Submitting feedback should close feedback activity
- \#6: Saving a "simulator" flight saves PIC name which is not in the dialog
- \#8: Clarify that augmented takeoff/landing should include taxi time 

##### miscellaneous things like removing dead code and cleaning up