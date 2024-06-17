# User Stories

## Non-User

    Landing Page Navigation and Signup:

    As a non-user, I want to land on the website’s landing page and easily navigate through the available options. When I visit the site:

    The navigation bar prominently displays three options:
    - “Signup”
    - “About Us”
    - “Contact”

    Upon clicking “Signup,” I am presented with a user-friendly form that requests the following information:
    - Name
    - Email
    - Username
    - Password

    After filling out the form, I submit it. If successful, I am redirected to a confirmation page that indicates my account creation was successful.

    On this confirmation page, I also find a button that allows me to navigate to the account summary page for further interactions.

## User

### Account Summary Page:

    Upon logging in, I am directed to the account summary page.

    The page includes:
    - Cards representing my vehicles, each with a picture, year/make/model, and buttons to add/edit a vehicle. With an option to view a detailed vehicle summary page.
    - An option to find nearby repair services.

### Services Near Me Page:

    As a user, when I select “Services Near Me,” I am directed to a dedicated page.

    On this page:
    - I can enter my zipcode.
    - The application retrieves a list of nearby repair facilities.
    - Each facility is accompanied by its Google ratings and a link to its website.
    - This page helps me find convenient repair services in my area.

### Detailed Vehicle Summary Page:

    Clicking on a vehicle card takes me to a detailed summary page.
        The page displays:
        - Year, make, model, mileage, and a picure of the vehicle.
        - Upcoming reminders.

    I see options to:
    - Update mileage.
    - Delete the vehicle.
    - Navigate to the maintenance information page.
    - Navigate to saftey recall information page.
    - Click next to a reminder to edit it
    - Click next to a reminder to delete it

### Edit a Reminder:

    If I click edit a reminder I am redirected to a new page where I see the existing reminder details and can update the date or description.
    If I click update the reminder is updated, if I click cancel the reminder is not edited. I am redirected back to the detailed vhicle summary page.

### Delete a Reminder:

    If I click delete a reminder I am redirected to a new page where I see the existing reminder details.
    If I click delete on this page the reminder is deleted, if I click cancel the reminder is not deleted. I am redirected back to the detailed vhicle summary page.

### Maintenance Information Page:

    If I choose the maintenance option:
        - A picture of the vehicle, along with its year, make, model, and VIN, appears at the top.
        - A list of non-complete maintenance items (+- 10,000mi) is displayed, along with approximate pricing.
        - A list of completed maintenance items with the completed date and mileage.

    I see options to:
        - Click next to a non-complete maintainence item to add maintenance record for that item.
        - Click next to a completed maintenance item to edit the comments.
        - Click next to a completed maintenance item to delete it.
        - Click next to a non-completed maintenance item to add/edit a reminder for that item.
        - Create a custom reminder.
        - Add a custom maintainence record.

### Add a Maintenance Item:

        If I naviagted here by clicking button next to a non-complete maintenance record I see:
        - Summary Details of record I clicked.
        - A place to enter notes regarding this maintenance item.

        If I navigated here by clicking custom maintenance item:
        - I can manually add details (description, notes, price, mileage, notes).

        After selection, I receive a confirmation summary and can either confirm or edit my choices.

### Edit a Maintenance Item:

    If I click edit a maintenance item notes I am redirected to a new page where I see the existing maintenance details and can update the notes associated with the item.
    If I click update the notes are updated, if I click cancel the notes are not edited. I am redirected back to the maintenance information page.

### Delete a Maintenance Item:

    If I click delte a maintenance item I am redirected to a new page where I see the existing maintenance details.
    If I click delete the item is deleted, if I click cancel the item is not edited. I am redirected back to the maintenance information page.
        
### Add a Maintenance Reminder:

    If I navigate here by clicking next to a maintenance item:
    - I see the details of the maintenance item with the option to choose reminder preferences. (see below)

    Reminder preferences:
        Option A: Custom Reminder Date
        - I manually enter the date when I would like to receive the reminder.
        Option B: Default Estimation
        - I choose the default option, allowing the application to estimate the reminder date based on the maintenance schedule.

    If I navigated here by clicking the add a custom reminder button:
    - I can enter my own custom reminder information.

    Upon submitting the form, I receive a confirmation page showing the details of the reminder. I can confirm or cancel and am navigated back to the vehicle summary page.

### Safety Recall Page:

    If I navigate to the safety recall page, I will see a list of the safety recalls for the specific vehicle