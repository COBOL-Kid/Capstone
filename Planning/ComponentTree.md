# Component Tree

    App {Owner}**
    - NavBar
    - Login
        - Account Summary {specificVin}
            - Vin Cards {onwerVins}
            - Vehicle Summary {-> specificVin, specificReminder}
                - Vin Details List {-> specificVin}
                - Reminders List {allReminders}
                - Maintenance {-> specificVin, specificMaintenance, specificReminder}
                    - Maintenance List {allMaintenance}
                    - Maintenance Form {-> specificMaintenance}
                    - Reminder Form {-> specificReminder}
                    - Vehicle Details {-> specificVin}
                - SafteyRecalls {recalls}
            - Services Near Me
    - Sign Up

# Folder Structure (Components vs Pages)

    App
    
    Components
    - NavBar
    - Vin Details List
    - Vin Cards
    - Reminders List
    - Maintenance List
    -Maintenance Form
    -Reminder Form
    
    Pages
    - Landing
    - Account Summary
    - Vehicle Summary
    - Maintainence
    - Safety Recall
    - Confirmation Page

# Needed States
    - Owner
    - ownerVins
    - specificVin
    - allReminders
    - specificReminder
    - allMaintenance
    - specificMaintencance