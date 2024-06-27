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
        <Card sx={{maxWidth: 345}} key={vehicle.vinId}>
            <CardMedia
                sx={{height: 170}}
                image={vehicle.image}
                title={vehicle.model}
            />
            <CardContent>
                <Typography gutterBottom variant="h5" component="div">
                    {vehicle.make} {vehicle.model}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    {vehicle.vin}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Mileage: {vehicle.mileage}
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
