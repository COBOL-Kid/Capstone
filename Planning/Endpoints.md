# From Api (all path variables)

GET Vehicle
input: Vin#
returns: make, model, year

GET Vehicle Image
input: make, model, year
output: image url

GET Safety Recall
input: make, model, year
output: [recalls]

GET Maintenance
input: VIN
output: [maintenance +- 10k mi, decription, cost, mileage]

# My Backend

## Owner

/owner
GET Owner
input: @RequestBody Owner

/owner
POST Owner
input: @RequestBody Owner

## Reminder Controller

/reminder:vinId
GET Reminder
input: @pathvariable vinId
output: [reminders]

/reminder/create
POST Reminder
input: @RequestBody Reminder

/reminder/update
PUT Reminder
input: @RequestBody Reminder

/reminder/delete:reminderId
DELETE Reminder
input: @PathVariable ReminderId

## Vin Controller

/vin
GET Vin
input: @PathVariable Vin#
output: Vin Model

/vin/create
POST Vin
**Will need to call GET Vehicle from Api
input: @RequestBody Vin

/vin/update
PUT Vin
input: @RequestBody Vin

/vin/delete
DELETE Vin
input: @PathVariable Vin#

## Record Controller

/record:vinId
GET Record
input: @PathVariable vinId
output: [records]

/record/create
POST Record
input: @RequestBody Record

/record/update
PUT Record
input: @RequestBody Record

/record/delete:recordId
DELETE Record
input: @PathVariable recordId

/record/calculate
GET Record
input: @RequestBody Record
output: Record (with calculated date)