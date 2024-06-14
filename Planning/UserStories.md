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

    - Upon logging in, I am directed to the account summary page.

    The page includes:
    - A section displaying recent Technical Service Bulletins (TSBs) and safety bulletins.
    - Cards representing my vehicles, each with a picture, year/make/model, and buttons to add/edit a vehicle.
    - An option to find nearby repair services.
    - If I select “Services Near Me,” I am redirected to the services near me page.
    - Choosing “Add a Vehicle” redirects me to the Add a Vehicle Page.

### Services Near Me Page:

    - As a user, when I select “Services Near Me,” I am directed to a dedicated page.

    On this page:
    - I can enter my zipcode.
    - The application retrieves a list of nearby repair facilities.
    - Each facility is accompanied by its Google ratings and a link to its website.
    - This page helps me find convenient repair services in my area.

### Detailed Vehicle Summary Page:

    - Clicking on a vehicle card takes me to a detailed summary page.
        The page displays:
        - Year, make, model, and additional vehicle details.
        - Recently posted TSBs or safety recalls.

    I see options to:
    - Create reminders.
    - Update mileage.
    - Delete the vehicle.
    - An option to lookup check engine light or other repair codes.
    - Navigate to the maintenance information page.


### Maintenance Information Page:

    - If I choose the maintenance option:
        - A picture of the vehicle, along with its year, make, model, and VIN, appears at the top.
        - A list of recommended maintenance items (within ±10,000 miles of the current mileage) is displayed, along with approximate pricing and parts needed
        - Beneath, I find outstanding recalls and service bulletins.

    I see options to:
        - Add a maintenance record.
        - View past repair logs.
        - Create reminders for upcoming maintenance tasks.

    Adding a Maintenance Record:
        - I see suggested maintenance items (within ±10,000 miles).
        - I can check a box next to an item to add it as a maintenance task.
        - I enter the repair mileage and optionally the cost.
        - If the desired item isn’t in the list, I can manually add details (description, notes, price, mileage).
        - After selection, I receive a confirmation summary and can either confirm or edit my choices.
        
### Create Maintenance Reminder:

    If I navigate to the “Create Reminder” page:

    - I see a list of upcoming maintenance tasks.
    - I can select specific maintenance items by checking checkboxes next to them.

    Reminder preferences:
    
    Option A: Custom Reminder Date
    - I manually enter the date when I would like to receive the reminder.

    Option B: Default Estimation
    - I choose the default option, allowing the application to estimate the reminder date based on the maintenance schedule.

    Upon submitting the form, I receive a confirmation page showing the details of the reminder. I can confirm or cancel and am navigated back to the vehicle summary page.

### View Maintenance Log:

    - When I select this option:
    - I see a table displaying maintenance records from latest to earliest.

    Each record includes:
    - Description of the maintenance task.
    - Mileage when the task was performed.
    - Date of the maintenance.
    - Cost incurred.
    - For each record, I have the option to:
    - Edit the record, which navigates me to a form similar to adding a new repair.
    - Delete the record, which displays a confirmation summary without a form.


