import * as React from 'react';
import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import CardMedia from '@mui/material/CardMedia';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import {useNavigate} from "react-router-dom";

export default function VehicleCards({vehicles, setChosenVehicle}) {

    const navigate = useNavigate();

    return vehicles.map((vehicle) => (
        <Card sx={{ maxWidth: 345 }} key={vehicle.id}>
            <CardMedia
                sx={{ height: 140 }}
                image={vehicle.image}
                title={vehicle.model}
            />
            <CardContent>
                <Typography gutterBottom variant="h5" component="div">
                    {vehicle.make} {vehicle.model} ({vehicle.mileage})
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    {vehicle.vin} {vehicle.mileage}
                </Typography>
            </CardContent>
            <CardActions>
                <Button size="large" onClick={() => {
                    setChosenVehicle(vehicle);
                    navigate("/vehicle_overview")
                }}>See Details</Button>
            </CardActions>
        </Card>
    ));
}
