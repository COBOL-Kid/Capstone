import {useEffect, useState} from 'react';
import {Button, Typography} from '@mui/material';
import {Box} from '@mui/system';
import VehicleCards from "../Components/VehicleCards.jsx";

function FleetOverview({user}) {

    const [vehicles, setVehicles] = useState([]);

    useEffect(() => {
        fetch('/api/vehicles')
            .then(response => response.json())
            .then(data => setVehicles(data));
    }, []);

    return (
        <Box
            display="flex"
            flexDirection="column"
            justifyContent="space-between"
            height="100vh"
            p={2}
        >
            <Typography variant="h2" align="center">
                My Title
            </Typography>
            <VehicleCards/>
            <Button variant="contained" size="large" style={{alignSelf: 'center'}}>
                My Large Button
            </Button>
        </Box>
    )
}

export default FleetOverview;