import React from 'react';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import {Box} from '@mui/system';
import {Grid, Paper} from '@mui/material';

export default function VehicleOverview({user, chosenVehicle}) {

    return (
        <Box sx={{flexGrow: 1}}>
            <Grid container spacing={2}>
                <Grid item xs={8}>
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
                            <Button variant="contained" color="secondary" sx={{ml: 1}}>
                                Delete Vehicle
                            </Button>
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