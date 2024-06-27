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


export default function Maintenance({chosenVehicle, user, setReminders}) {
    const [upcomingMaintenance, setUpcomingMaintenance] = useState([]);
    const [errors, setErrors] = useState([]);
    const [completedMaintenance, setCompletedMaintenance] = useState([]);
    const [reminderDialogOpen, setReminderDialogOpen] = useState({});
    const [reminderDate, setReminderDate] = useState({});
    const navigate = useNavigate();
    const [change, setChange] = useState(false);
    const [updatedUpcomingMaint, setUpdatedUpcomingMain] = useState([]);
    const [filteredUpcomingMaintenance, setFilteredUpcomingMaintenance] = useState([]);

    const todayDate = new Date();
    const todayYear = todayDate.getFullYear();
    const todayMonth = String(todayDate.getMonth() + 1).padStart(2, '0');
    const todayDay = String(todayDate.getDate()).padStart(2, '0');
    const minDateString = `${todayYear}-${todayMonth}-${todayDay}`;

    const dateCompleted = `${todayYear}-${todayMonth}-${todayDay}`;

    const maintenanceItem = {
        "vinId": chosenVehicle.vinId,
        "description": "",
        "dateCompleted": dateCompleted,
        "mileageDue": 0,
        "cost": 0.0
    }

    const reminder = {
        "reminderId": 0,
        "vinId": chosenVehicle.vinId,
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

    const handleReminderConfirm = itemName => {
        reminder.description = itemName;
        let selectedDate = new Date(reminderDate[itemName]);
        let year = selectedDate.getFullYear();
        let month = String(selectedDate.getMonth() + 1).padStart(2, '0');
        let date = String(selectedDate.getDate()).padStart(2, '0');
        reminder.reminderDate = `${year}-${month}-${date}`;

        setReminderDialogOpen(prevState => ({...prevState, [itemName]: false}));

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
            } else if (response.status === 403) {
                localStorage.removeItem("user");
                navigate("/");
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
            }
        }).catch(errors => setErrors(errors));
        setReminderDialogOpen(prevState => ({...prevState, [itemName]: false}));
    }

    useEffect(() => {
        fetch(`http://localhost:8080/api/external/find_maintenance/${chosenVehicle.vin}/${chosenVehicle.mileage}`,
            {method: "GET", headers: {"Content-Type": 'application/json'}}
        )
            .then(response => {
                if (response.status === 200) {
                    response.json()
                        .then(data => {
                            setUpcomingMaintenance(data);
                        })
                } else if (response.status === 403) {
                    localStorage.removeItem("user");
                    navigate("/");
                } else {
                    Promise.reject(`Problem with response. Status: ${response.status}`);
                }
            }).catch(error => {
            setErrors([error.toString()]);
        });

        fetch(`http://localhost:8080/api/maintenance/${chosenVehicle.vinId}`, {
            method: "GET", headers: {
                "Content-Type": 'application/json',
                "Authorization": `Bearer ${user.jwt}`
            }
        }).then(response => {
            if (response.status === 200) {
                response.json().then(data => {
                    setCompletedMaintenance(data);
                })
            } else if (response.status === 204) {
                setCompletedMaintenance([])
            } else if (response.status === 403) {
                localStorage.removeItem("user");
                navigate("/");
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
            }
        }).catch(errors => setErrors(errors))
    }, [change]);


    function handleAddClick(maintenanceItem) {
        fetch("http://localhost:8080/api/maintenance", {
            method: "POST",
            headers: {
                "Content-Type": 'application/json',
                Authorization: `Bearer ${user.jwt}`
            },
            body: JSON.stringify(maintenanceItem)
        })
            .then(response => {
                if (response.status === 201) {
                    setChange(prevChange => !prevChange);  // Change state to force the list to be refreshed
                } else {
                    Promise.reject(`Problem with response. Status: ${response.status}`);
                }
            }).catch(errors => setErrors(errors))
    }

    useEffect(() => {
        const newUpcomingMaintenance = [];
        upcomingMaintenance.forEach((maintenanceItem) => {
            if (!completedMaintenance.some(item => item.description === maintenanceItem.desc && item.mileageDue === maintenanceItem.due_mileage)) {
                newUpcomingMaintenance.push(maintenanceItem);
            }
        });

        setFilteredUpcomingMaintenance(newUpcomingMaintenance);
    }, [upcomingMaintenance, completedMaintenance]);


    return (
        <>
            <Typography variant="h4" sx={{textAlign: 'center', mt: 2}}>
                Upcoming Maintenance
            </Typography>
            <Errors errors={errors}/>
            <Box sx={{width: '100%', maxHeight: '45vh', overflow: 'auto'}}>
                <List sx={{width: '100%', bgcolor: 'background.paper', marginTop: 2}}>
                    {filteredUpcomingMaintenance.map((item, index) => (
                        <ListItem key={index}
                                  secondaryAction={
                                      <>
                                          <Button variant="contained" color="success" onClick={() => {
                                              maintenanceItem.description = item.desc;
                                              maintenanceItem.mileageDue = item.due_mileage;
                                              maintenanceItem.cost = item.repair.total_cost;
                                              handleAddClick(maintenanceItem);
                                          }}>
                                              Add
                                          </Button>
                                          <Button variant="contained" color="primary"
                                                  onClick={() => handleReminderClick(item.desc)}
                                                  style={{marginLeft: '10px'}}>
                                              Reminder
                                          </Button>

                                          <Dialog open={reminderDialogOpen[item.desc]}
                                                  onClose={() => handleReminderDialogClose(item.desc)}>
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
                                                      value={reminderDate[item.desc] || ''}
                                                      min={minDateString}
                                                      onChange={event => handleReminderDateChange(event.target.value, item.desc)}
                                                  />
                                              </DialogContent>
                                              <DialogActions>
                                                  <Button onClick={() => handleReminderDialogClose(item.desc)}
                                                          color="primary">
                                                      Cancel
                                                  </Button>
                                                  <Button onClick={() => handleReminderConfirm(item.desc)}
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
                                primary={<Typography variant="h5">{item.desc}</Typography>}
                                secondary={
                                    <>
                                        <Typography variant="body2">
                                            Due mileage: {item.due_mileage}
                                        </Typography>
                                        <Typography variant="body2">
                                            Total cost: {item.repair.total_cost}
                                        </Typography>
                                        <Typography component="div" variant="body1">
                                            Parts:
                                        </Typography>
                                        {item.parts && item.parts.map((part, index) => (
                                            <Typography key={index} variant="body2">
                                                Part desc: {part.desc}, Price: {part.price}, Qty: {part.qty}
                                            </Typography>
                                        ))}
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