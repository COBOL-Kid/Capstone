import {useNavigate} from "react-router-dom";

import {useEffect, useState} from 'react';
import {Button, Typography} from '@mui/material';
import {Box} from '@mui/system';
import VehicleCards from "../Components/VehicleCards.jsx";
import {Errors} from "../Components/Errors.jsx";

function FleetOverview({user}) {

    const navigate = useNavigate();
    const [vehicles, setVehicles] = useState([]);
    const [errors, setErrors] = useState([]);

    useEffect(() => {
        fetch(`http://localhost:8080/api/vin/${user.ownerId}`,
            {
                method: "GET",
                headers: {
                    Authorization: `Bearer ${user.jwt}`
                }
            })
            .then(response => {
                if (response.status === 200) {
                    response.json().then(json => setVehicles(json));
                } else if (response.status === 204) {
                    setVehicles([])
                } else {
                    response.json().then(json => setErrors(json));
                }
            }).catch(errors => setErrors(["Something Went Wrong"]));
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
                Fleet Summary
            </Typography>
            <Errors errors={errors}/>
            {vehicles.length === 0 ? (
                <Typography variant="h4" align="center">
                    No Vehicles Currently
                </Typography>
            ) : (
                <VehicleCards vehicles={vehicles}/>
            )}
            <Button variant="contained" size="large" style={{alignSelf: 'center'}}
                    onClick={() => navigate("/add_vehicle")}>
                Add A Vehicle
            </Button>
        </Box>
    )
}

export default FleetOverview;