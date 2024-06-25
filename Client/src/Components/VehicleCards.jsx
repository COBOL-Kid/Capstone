import * as React from 'react';
import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import CardMedia from '@mui/material/CardMedia';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';

export default function VehicleCards({vehicles}) {

    return vehicles.map((vehicle) => (
        <Card sx={{maxWidth: 345}} key={vehicle.id}>
            <CardMedia
                sx={{height: 140}}
                image={vehicle.image}
                title={vehicle.name}
            />
            <CardContent>
                <Typography gutterBottom variant="h5" component="div">
                    {vehicle.name}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    {vehicle.description}
                </Typography>
            </CardContent>
            <CardActions>
                <Button size="small">Share</Button>
                <Button size="small">Learn More</Button>
            </CardActions>
        </Card>
    ));
}
