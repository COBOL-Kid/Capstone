# Mappers

    Vin
    - all fields
    - {vehicle}

    Owner
    - all fields

    Record
    - all fields

    Reminder
    - all fields

# Models

    Owner
    - all fields

    Vin
    - all fields
    - {Vehicle}

    Vehicle
    - all fields

    Reminder
    - all fields

    Record
    - all fields

    Result
    - errorMessages[]
    - payload<?>

# Repository

## Record

    Record[] getRecord(vinId)

    Result createRecord(Record)

    Result updateRecord(Record)

    Result deleteRecord(recordId)

## Owner

    Owner getOwner(Owner)

    Owner createOwner(Owner)

## Vin

    Result createVin(Vin)

    Vin getVin(vin#)

    Result updateVin(Vin)

    Result deleteVin(vin#)

## Vehicle

    Result getVehicle(vehicle)

## Reminder

    Result createReminder(Reminder)

    Reminder getReminder(vinId)

    Result updateReminder(Reminder)

    Result deleteReminder(reminderId)

# Service

    
    

