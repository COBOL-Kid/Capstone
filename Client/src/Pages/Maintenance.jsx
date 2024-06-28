import {useEffect, useState} from 'react';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    TextField,
    Typography
} from "@mui/material";
import {useNavigate} from "react-router-dom";
import {Errors} from "../Components/Errors.jsx";
import DeleteIcon from "@mui/icons-material/Delete";
import IconButton from "@mui/material/IconButton";
import AddCircleIcon from '@mui/icons-material/AddCircle';


export default function Maintenance({chosenVehicle, user}) {
    const [upcomingExternalApiMaintenance, setUpcomingExternalApiMaintenance] = useState([]);
    const [errors, setErrors] = useState([]);
    const [allMaintenance, setAllMaintenance] = useState([]);
    const [reminderDialogOpen, setReminderDialogOpen] = useState({});
    const [reminderDate, setReminderDate] = useState({});
    const navigate = useNavigate();
    const [change, setChange] = useState(false);
    const [upcomingMaintenance, setUpcomingMaintenance] = useState([]);
    const [mappedExternalApiMaintenance, setMappedExternalApiMaintenance] = useState([]);
    const [completedMaintenance, setCompletedMaintenance] = useState([]);

    const todayDate = new Date();
    const todayYear = todayDate.getFullYear();
    const todayMonth = String(todayDate.getMonth() + 1).padStart(2, '0');
    const todayDay = String(todayDate.getDate()).padStart(2, '0');
    const minDateString = `${todayYear}-${todayMonth}-${todayDay}`;

    const dateCompleted = `${todayYear}-${todayMonth}-${todayDay}`;

    const todayMaintenanceItem = {
        "maintenanceRecordId": 0,
        "vinId": chosenVehicle.vinId,
        "description": "",
        "dateCompleted": dateCompleted,
        "mileageDue": 0,
        "cost": 0.0
    }

    let convertedMaintenanceitem = {
        "vinId": chosenVehicle.vinId,
        "description": "",
        "mileageDue": 0,
        "cost": 0.0
    }

    const reminder = {
        "reminderId": 0,
        "vinId": chosenVehicle.vinId,
        "maintenanceRecordId": null,
        "description": "",
        "reminderDate": ""
    }

    const handleReminderClick = itemName => {
        setReminderDialogOpen(prevState => ({...prevState, [itemName]: true}));
    }

    const handleReminderDialogClose = itemName => {
        setReminderDialogOpen(prevState => ({...prevState, [itemName]: false}));
    }

    const handleReminderDateChange = (date, itemName) => {
        setReminderDate(prevState => ({...prevState, [itemName]: date}));
    }

    const handleReminderConfirm = item => {
        reminder.description = item.description;
        reminder.maintenanceRecordId = item.maintenanceRecordId;
        let selectedDate = new Date(reminderDate[item.description]);
        let year = selectedDate.getFullYear();
        let month = String(selectedDate.getMonth() + 1).padStart(2, '0');
        let date = String(selectedDate.getDate()).padStart(2, '0');
        reminder.reminderDate = `${year}-${month}-${date}`;
        setReminderDialogOpen(prevState => ({...prevState, [item.description]: false}));
        fetch("http://localhost:8080/api/reminder", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${user.jwt}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(reminder)
        }).then(response => {
            if (response.status === 201) {
                navigate("/maintenance_list");
                alert('Reminder added');
            } else if (response.status === 403) {
                localStorage.removeItem("user");
                navigate("/");
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
            }
        }).catch(errors => setErrors(errors));
        setReminderDialogOpen(prevState => ({...prevState, [item.description]: false}));
    }

    useEffect(() => {
        const fetchMaintenance = async () => {
            try {
                const response = await fetch(`http://localhost:8080/api/external/find_maintenance/${chosenVehicle.vin}/${chosenVehicle.mileage}`,
                    {method: "GET", headers: {"Content-Type": 'application/json'}}
                );

                if (response.status === 200) {
                    const data = await response.json();
                    setUpcomingExternalApiMaintenance(data);
                } else if (response.status === 403) {
                    localStorage.removeItem("user");
                    navigate("/");
                } else {
                    throw new Error(`Problem with response. Status: ${response.status}`);
                }
            } catch (error) {
                setErrors([error.toString()]);
            }
        };

        const convertAndMapData = async () => {
            const convertedUpcomingArr = [];
            upcomingExternalApiMaintenance.forEach((record) => {
                let convertedMaintenanceitem = {
                    "vinId": chosenVehicle.vinId,
                    "description": record.desc,
                    "mileageDue": record.due_mileage,
                    "cost": record.repair.total_cost
                };
                convertedUpcomingArr.push(convertedMaintenanceitem);
            });
            setMappedExternalApiMaintenance(convertedUpcomingArr);
        }

        const updateMaintenanceRecord = async () => {
            try {
                const response = await fetch("http://localhost:8080/api/maintenance/update_maintenance_records", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${user.jwt}`
                    },
                    body: JSON.stringify(mappedExternalApiMaintenance)
                });
                if (response.status !== 201) {
                    throw new Error(`Problem with response. Status: ${response.status}`);
                }
            } catch (errors) {
                setErrors(errors);
            }
        };

        const fetchMaintenanceById = async () => {
            try {
                const response = await fetch(`http://localhost:8080/api/maintenance/${chosenVehicle.vinId}`, {
                    method: "GET",
                    headers: {
                        "Content-Type": 'application/json',
                        "Authorization": `Bearer ${user.jwt}`
                    }
                });
                if (response.status === 200) {
                    const data = await response.json();
                    setAllMaintenance(data);
                    return data;
                } else if (response.status === 204) {
                    setAllMaintenance([]);
                    return [];
                } else if (response.status === 403) {
                    localStorage.removeItem("user");
                    navigate("/");
                } else {
                    throw new Error(`Problem with response. Status: ${response.status}`);
                }
            } catch (error) {
                setErrors([error.toString()]);
            }
        }

        const operations = async () => {
            const fetchedMaintenance = await fetchMaintenance();
            console.log("fetched maintenance");
            console.log(fetchedMaintenance);
            const convertedData = await convertAndMapData(fetchedMaintenance);
            console.log("converted data")
            console.log(convertedData);
            await updateMaintenanceRecord(convertedData);
            const fetchedMaintenanceById = await fetchMaintenanceById();
            console.log("fetched maintenance by Id");
            console.log(fetchedMaintenanceById);
            const completedMaintenance = fetchedMaintenanceById.filter((record) => record.dateCompleted !== null);
            console.log("completed maintenance");
            console.log(completedMaintenance);
            const upcomingMaintenance = fetchedMaintenanceById.filter((record) => record.dateCompleted === null);
            console.log("upcoming maintenance")
            console.log(upcomingMaintenance);
            setUpcomingMaintenance(upcomingMaintenance);
            setCompletedMaintenance(completedMaintenance);
        }

        operations();
    }, [change]);


    function handleAddClick(maintenanceItem) {
        fetch("http://localhost:8080/api/maintenance", {
            method: "PUT",
            headers: {
                "Content-Type": 'application/json',
                Authorization: `Bearer ${user.jwt}`
            },
            body: JSON.stringify(maintenanceItem)
        })
            .then(response => {
                if (response.status === 200) {
                    setChange(prevChange => !prevChange);
                } else if (response.status === 403) {
                    localStorage.removeItem("user");
                    navigate("/");
                } else {
                    Promise.reject(`Problem with response. Status: ${response.status}`);
                }
            }).catch(errors => setErrors(errors))
    }

    function handleDeleteClick(item) {
        fetch(`http://localhost:8080/api/maintenance/${item.maintenanceRecordId}`, {
            method: "DELETE",
            headers: {
                "Content-Type": 'application/json',
                Authorization: `Bearer ${user.jwt}`
            }
        }).then(response => {
            if (response.status === 200) {
                setChange(prevChange => !prevChange);
            } else if (response.status === 403) {
                localStorage.removeItem("user");
                navigate("/");
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
            }
        }).catch(errors => setErrors(errors))
    }


    return (
        <>
            <Typography variant="h4" sx={{textAlign: 'center', mt: 2}}>
                Upcoming Maintenance
            </Typography>
            <Errors errors={errors}/>
            <Box sx={{width: '100%', maxHeight: '45vh', overflow: 'auto'}}>
                <List sx={{width: '100%', bgcolor: 'background.paper', marginTop: 2}}>
                    {upcomingMaintenance.map((item, index) => (
                        <ListItem key={index}
                                  secondaryAction={
                                      <>
                                          <IconButton color="primary" aria-label="add" onClick={() => {
                                              todayMaintenanceItem.description = item.description;
                                              todayMaintenanceItem.mileageDue = item.mileageDue;
                                              todayMaintenanceItem.cost = item.cost;
                                              todayMaintenanceItem.maintenanceRecordId = item.maintenanceRecordId;
                                              handleAddClick(todayMaintenanceItem);
                                          }}>
                                              <AddCircleIcon/>
                                          </IconButton>
                                          <Button variant="contained" color="primary"
                                                  onClick={() => handleReminderClick(item.description)}
                                                  style={{marginLeft: '10px'}}>
                                              Reminder
                                          </Button>

                                          <Dialog open={reminderDialogOpen[item.description]}
                                                  onClose={() => handleReminderDialogClose(item.description)}>
                                              <DialogTitle>Add Reminder</DialogTitle>
                                              <DialogContent>
                                                  <DialogContentText>
                                                      Please enter the reminder date
                                                  </DialogContentText>
                                                  <TextField
                                                      autoFocus
                                                      margin="dense"
                                                      id="reminderDate"
                                                      type="date"
                                                      fullWidth
                                                      value={reminderDate[item.description] || ''}
                                                      min={minDateString}
                                                      onChange={event => handleReminderDateChange(event.target.value, item.description)}
                                                  />
                                              </DialogContent>
                                              <DialogActions>
                                                  <Button onClick={() => handleReminderDialogClose(item.description)}
                                                          color="primary">
                                                      Cancel
                                                  </Button>
                                                  <Button onClick={() => handleReminderConfirm(item)}
                                                          color="primary">
                                                      Confirm
                                                  </Button>
                                              </DialogActions>
                                          </Dialog>
                                      </>
                                  }
                                  sx={{
                                      bgcolor: 'background.paper',
                                      border: 1,
                                      borderColor: 'divider',
                                      borderRadius: 2,
                                      m: 1
                                  }}
                        >
                            <ListItemText
                                primary={<Typography variant="h5">{item.description}</Typography>}
                                secondary={
                                    <>
                                        <Typography variant="body2">
                                            Due mileage: {item.mileageDue}
                                        </Typography>
                                        <Typography variant="body2">
                                            Total cost: {item.cost}
                                        </Typography>
                                    </>
                                }
                            />
                        </ListItem>
                    ))}
                </List>
            </Box>
            <Typography variant="h4" sx={{textAlign: 'center', mt: 2}}>
                Completed Maintenance
            </Typography>
            <Box sx={{width: '100%', maxHeight: '45vh', overflow: 'auto'}}>
                <List sx={{width: '100%', bgcolor: 'background.paper', marginTop: 2}}>
                    {completedMaintenance.map((item, index) => (
                        <ListItem key={index}
                                  secondaryAction={
                                      <IconButton edge="end" aria-label="delete"
                                                  onClick={() => handleDeleteClick(item)}>
                                          <DeleteIcon/>
                                      </IconButton>
                                  }
                                  sx={{
                                      bgcolor: 'background.paper',
                                      border: 1,
                                      borderColor: 'divider',
                                      borderRadius: 2,
                                      m: 1
                                  }}
                        >
                            <ListItemText
                                primary={<Typography variant="h5">{item.description}</Typography>}
                                secondary={
                                    <>
                                        <Typography variant="body2">
                                            Due mileage: {item.mileageDue}
                                        </Typography>
                                        <Typography variant="body2">
                                            Total cost: {item.cost}
                                        </Typography>
                                        <Typography variant={"body2"}>
                                            Date completed: {item.dateCompleted}
                                        </Typography>
                                    </>
                                }
                            />
                        </ListItem>
                    ))}
                </List>
            </Box>
        </>
    );
}