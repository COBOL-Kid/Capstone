# Mappers

    ReminderEmail
    - reminder description
    - vin
    - {owner}

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
    - errorMessages
    - payload<?>

# Repository

## Record

    List<Record> getRecord(vinId)

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

    List<ReminderEmail> findReminderByToday()

# Service

## Record 

    Validation:
    - Fields must be non-null with the exception of Notes. (create/update)
    - Must have a VinId that exists. (create/update)
    - Mileage and cost cannot be negative numbers.
    - Cannot be a duplicate (ie all fields the same).
    - Record must exist (delete)

    Record[] getRecord(vinId)

    Result createRecord(Record)

    Result updateRecord(Record)

    Result deleteRecord(recordId)

## Owner

    Validation:
    - Cannot be a duplicate (ie same first name, last name, email).
    - No fields cannot be null or empty.

    Owner getOwner(Owner)

    Owner createOwner(Owner)

## Vin

    Validation:
    - fields cannot be null or empty (create/update).
    - vin must exist (delete).
    - mileage cannot be negative.

    Result createVin(Vin)

    Vin getVin(vin#)

    Result updateVin(Vin)

    Result deleteVin(vin#)

## Reminder

    Validation:
    - fields cannot be null/empty (create/update).
    - date must be in the future (create/update).
    - vinId must be exist.

    Reminder calculateDate(Reminder)
    -> averages miles/day based on previous maintenance records
    -> if no previous records uses the standard average of 12,000mi/yr

    Result createReminder(Reminder)

    Reminder getReminder(vinId)

    Result updateReminder(Reminder)

    Result deleteReminder(reminderId)

## Email Service

    void sendEmail(ReminderEmail)

## Email Scheduler

    emailScheduler()
    -> will fetch emails using findReminderByToday()
    -> will send the emails using sendEmail()