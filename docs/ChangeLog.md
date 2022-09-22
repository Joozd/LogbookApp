# 1.2
##### Removed Cloud, causing lots of related changes
- No longer sync flights to/from Cloud
- Email storage on server is still here, but works different(see [email](howStuffWorks/email.md))
- Email Backup now works different (see [email](howStuffWorks/backup_email.md))
- All user account logic is removed, except for parts needed for migrating email.

##### fixed issues
- #5: Submitting feedback should close feedback activity
- #6: Saving a "simulator" flight saves PIC name which is not in the dialog
- #8: Clarify that augmented takeoff/landing should include taxi time 
