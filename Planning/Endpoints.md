# External Api's

## From Gmaps Api

GET

## From Auto Api (all path variables)

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
output: Owner/NotFound


POST /owner
input: @RequestBody Owner
output: OK/NotAuthenticated

## Reminder Controller


GET /reminder:vinId
input: @pathvariable vinId
output: List<Reminder>/NOT_FOUND

POST /reminder/create
input: @RequestBody Reminder
output: CREATED/BAD_REQUEST/FORBIDDEN

PUT /reminder/update
input: @RequestBody Reminder
output: OK/NOT_FOUND/FORBIDDEN/CONFLICT

DELETE /reminder/delete:reminderId
input: @PathVariable ReminderId
output: OK/NOT_FOUND

## Vin Controller

GET /vin/owner:ownerId
input: @PathVariable ownerId
output: List<Vins>/NOT_FOUND/FORBIDDEN

POST /vin/create
**Will need to call GET Vehicle from Api
input: @RequestBody Vin
output: CREATED/BAD_REQUEST/FORBIDDEN

PUT /vin/update
input: @RequestBody Vin
output: OK/NOT_FOUND/FORBIDDEN/CONFLICT

DELETE /vin/delete
input: @PathVariable Vin#
output: OK/NOT_FOUND/FORBIDDEN

## Record Controller

GET /record:vinId
input: @PathVariable vinId
output: [records]/NOT_FOUND/FORBIDDEN

POST /record/create
input: @RequestBody Record
output: CREATED/BAD_REQUEST/FORBIDDEN

PUT /record/update
input: @RequestBody Record
output: OK/NOT_FOUND/FORBIDDEN/CONFLICT

DELETE /record/delete:recordId
input: @PathVariable recordId
output: OK/NOT_FOUND/FORBIDDEN

GET /record/calculate
input: @RequestBody Record
output: Record (with calculated date)/FORBIDDEN