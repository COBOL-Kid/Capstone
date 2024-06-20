# Mappers

    ReminderEmail
    - reminder description
    - vin
    - {owner}

    Vin
    - all fields
    - {vehicleInfo}

    Owner
    - all fields

    Record
    - all fields

    Reminder
    - all fields

# Models

    ReminderEmail
    - {Reminder}
    - vin#
    - {User}

    SecurityKey
    - Key

    Owner
    - all fields

    Vin
    - all fields
    - {vehicleInfo}

    vehicleInfo
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

    Owner getOwnerByUsername(Owner)

    Owner createOwner(Owner)

## Vin

    Result createVin(Vin)

    List<Vin> getVinByOwnerId(ownerId)

    Result updateVin(Vin)

    Result deleteVin(vinId)

## vehicleInfo

    Result getvehicleInfo(vehicleInfoId)

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

    List(Record) getRecord(vinId)

    Result createRecord(Record)

    Result updateRecord(Record)

    Result deleteRecord(recordId)

## Owner

    Validation:
    - Cannot be a duplicate (ie same first name, last name, email).
    - No fields cannot be null or empty.

    Owner getOwnerByUsername(Owner)

    Owner createOwner(Owner)

## Vin

    Validation:
    - fields cannot be null or empty (create/update).
    - vin must exist (delete).
    - mileage cannot be negative.

    Result createVin(Vin)

    List<Vin> getVinByOwnerId(ownerId)

    Result updateVin(Vin)

    Result deleteVin(vinId)

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

# Controllers

## Global Error Handler

    Catch all Handler

**See Endpoints for the rest of the Controllers**