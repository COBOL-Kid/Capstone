import React, {useEffect, useState} from 'react';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import {Box} from '@mui/system';
import {Dialog, DialogContent, DialogTitle, Grid, List, Paper} from '@mui/material';
import {Errors} from "../Components/Errors.jsx";
import UpdateMileageForm from "../Components/UpdateMileageForm.jsx";
import VinConfirm from "../Components/VinConfirm.jsx";
import {useNavigate} from "react-router-dom";
import ReminderList from "../Components/ReminderList.jsx";

export default function VehicleOverview({user, chosenVehicle, setChosenVehicle, reminders, setReminders}) {

    const [isDeleteClicked, setIsDeleteClicked] = useState(false);
    const [isUpdateClicked, setIsUpdateClicked] = useState(false);
    const [errors, setErrors] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        fetchReminders();
    }, []);

    function fetchReminders() {
        fetch(`http://localhost:8080/api/reminder/${chosenVehicle.vinId}`, {
            method: 'GET',
            header: {
                Authorization: `Bearer ${user.jwt}`,
            }
        }).then(response => {
            if (response.status === 200) {
                response.json().then(data => {
                    setReminders(data);
                })
            } else if (response.status === 403) {
                localStorage.removeItem("user");
                navigate("/")
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
            }
        }).catch(errors => setErrors(["Something Went Wrong"]));
    }

    const handleDeleteClick = () => {
        setIsDeleteClicked(true);
    };

    const handleUpdateClick = () => {
        setIsUpdateClicked(true);
    };

    return (
        <Box sx={{flexGrow: 1}}>
            <Grid container spacing={2}>
                <Grid item xs={8}>
                    <Errors errors={errors}/>
                    <Paper sx={{p: 2}}>
                        <Typography variant="h5" gutterBottom>
                            {chosenVehicle.model}
                        </Typography>
                        <Typography variant="body2">
                            Manufacturer: {chosenVehicle.make}
                        </Typography>
                        <Typography variant="body2">
                            Model Year: {chosenVehicle.year}
                        </Typography>
                        <Typography variant="body2">
                            Mileage: {chosenVehicle.mileage}
                        </Typography>
                        <Box pt={2}>
                            <Grid container spacing={2}>
                                <Grid item>
                                    <Button variant="contained" color="primary"
                                            onClick={() => navigate("/maintenance_list")}>
                                        View Maintenance
                                    </Button>
                                </Grid>
                                <Grid item>
                                    <Dialog open={isUpdateClicked} onClose={() => setIsUpdateClicked(false)}>
                                        <DialogTitle>Update Mileage</DialogTitle>
                                        <DialogContent>
                                            <UpdateMileageForm
                                                chosenVehicle={chosenVehicle}
                                                setChosenVehicle={setChosenVehicle}
                                                setErrors={setErrors}
                                                user={user}
                                                setIsUpdateClicked={setIsUpdateClicked}
                                            />
                                        </DialogContent>
                                    </Dialog>
                                    <Button variant="contained" color="primary"
                                            onClick={handleUpdateClick}>
                                        Update Mileage
                                    </Button>
                                </Grid>
                                <Grid item>
                                    <Dialog open={isDeleteClicked} onClose={() => setIsDeleteClicked(false)}>
                                        <DialogTitle>Confirm VIN</DialogTitle>
                                        <DialogContent>
                                            <VinConfirm
                                                vehicleData={chosenVehicle}
                                                user={user}
                                                setErrors={setErrors}
                                            />
                                        </DialogContent>
                                    </Dialog>
                                    <Button variant="contained" color="secondary" onClick={handleDeleteClick}>
                                        Delete Vehicle
                                    </Button>
                                </Grid>
                            </Grid>
                        </Box>
                    </Paper>
                </Grid>
                <Grid item xs={4}>
                    <img src={chosenVehicle.image} alt={chosenVehicle.model} style={{width: '100%'}}/>
                </Grid>
            </Grid>
            <Box my={4}>
                <Typography variant="h5" gutterBottom align="left">
                    Reminders
                </Typography>
                {reminders.length > 0 ?
                    <List>
                        {reminders.map(reminder => (
                            <ReminderList key={reminder.id} reminder={reminder} user={user} setErrors={setErrors}
                                          fetchReminders={fetchReminders}/>
                        ))}
                    </List>
                    : <Typography variant="body2" gutterBottom align="left">
                        No Reminders
                    </Typography>}
            </Box>
        </Box>
    )
}