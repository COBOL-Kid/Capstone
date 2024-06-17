# Mappers

    Vehicle
    Owner
    Record

    Reminder
    - ReminderId
    - Description
    - Date
    - Vin

# Models

    Vin
    - vinId
    - mileage
    - {Vehicle}

    Vehicle
    - vehicleId
    - year
    - make
    - model
    - image

    Reminder
    - reminderId
    - description
    - reminderDate
    - vinId

    Record
    - recordId
    - vinId
    - description
    - notes
    - dateCompleted
    - mileage
    - cost

    