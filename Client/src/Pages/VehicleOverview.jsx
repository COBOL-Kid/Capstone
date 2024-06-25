import React, {useState} from 'react';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import {Box} from '@mui/system';
import {Grid, Paper} from '@mui/material';
import VinConfirm from "../Components/VinConfirm.jsx";
import {Errors} from "../Components/Errors.jsx";

export default function VehicleOverview({user, chosenVehicle}) {

    const [isDeleteClicked, setIsDeleteClicked] = useState(false);
    const [errors, setErrors] = useState([]);

    const handleDeleteClick = () => {
        setIsDeleteClicked(true);
    }

    return (
        <Box sx={{flexGrow: 1}}>
            <Grid container spacing={2}>
                <Grid item xs={8}>
                    <Errors errors={errors} />
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
                            <Button variant="contained" color="primary">
                                Update Mileage
                            </Button>
                            {isDeleteClicked ? (
                                    <VinConfirm vehicleData={chosenVehicle} user={user} setErrors={setErrors} />
                            ) : (
                                <Button variant="contained" color="secondary" sx={{ml: 1}} onClick={handleDeleteClick}>
                                    Delete Vehicle
                                </Button>
                            )}
                        </Box>
                    </Paper>
                </Grid>
                <Grid item xs={4}>
                    <img src={chosenVehicle.image} alt={chosenVehicle.model} style={{width: '100%'}}/>
                </Grid>
            </Grid>
        </Box>
    )
}