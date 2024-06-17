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

GET /owner
input: @ Owner
output: Owner


POST /owner
input: @RequestBody Owner

## Reminder Controller


GET /reminder:vinId
input: @pathvariable vinId
output: [reminders]

POST /reminder/create
input: @RequestBody Reminder

PUT /reminder/update
input: @RequestBody Reminder

DELETE /reminder/delete:reminderId
input: @PathVariable ReminderId

## Vin Controller

/vin/owner
GET /vin/owner:ownerId
input: @PathVariable ownerId
output: List<Vins>

GET /vin:vin#
input: @PathVariable Vin#
output: Vin

POST /vin/create
**Will need to call GET Vehicle from Api
input: @RequestBody Vin

PUT /vin/update
input: @RequestBody Vin

DELETE /vin/delete
input: @PathVariable Vin#

## Record Controller

GET /record:vinId
input: @PathVariable vinId
output: [records]

POST /record/create
input: @RequestBody Record

PUT /record/update
input: @RequestBody Record

DELETE /record/delete:recordId
input: @PathVariable recordId

GET /record/calculate
input: @RequestBody Record
output: Record (with calculated date)