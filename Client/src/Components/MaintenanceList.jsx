import {useEffect, useState} from 'react';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import {Box, Button, Typography} from "@mui/material";
import {useNavigate} from "react-router-dom";
import {Errors} from "./Errors.jsx";


export default function MaintenanceList({chosenVehicle, user}) {
    const [upcomingMaintenance, setUpcomingMaintenance] = useState([]);
    const [errors, setErrors] = useState([]);
    const [completedMaintenance, setCompletedMaintenance] = useState([]);
    const navigate = useNavigate();
    const today = new Date();

    const date = String(today.getDate()).padStart(2, '0');
    const month = String(today.getMonth() + 1).padStart(2, '0');

    const dateCompleted = `${today.getFullYear()}-${month}-${date}`;

    const maintenanceItem = {
        "vinId": chosenVehicle.vinId,
        "description": "",
        "dateCompleted": dateCompleted,
        "mileageDue": 0,
        "cost": 0.0
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
                }
            }).catch(errors => setErrors(["Something Went Wrong"]))

        fetch(`http://localhost:8080/api/maintenance/${chosenVehicle.vinId}`, {
            method: "GET", headers: {
                "Content-Type": 'application/json',
                Authorization: `Bearer ${user.jwt}`
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
                setErrors()
            }
        }).catch(errors => setErrors(errors))
    }, []);

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
                    navigate("/test");
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
                                      <Button variant="contained" color="success" onClick={() => {
                                          maintenanceItem.description = item.desc;
                                          maintenanceItem.mileageDue = item.due_mileage;
                                          maintenanceItem.cost = item.repair.total_cost;
                                          handleAddClick(maintenanceItem);
                                      }
                                      }>
                                          Add
                                      </Button>
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