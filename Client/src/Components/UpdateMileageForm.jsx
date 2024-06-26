import {Button, Container, TextField} from "@mui/material";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {Errors} from "./Errors.jsx";
import VinConfirm from "./VinConfirm.jsx";
import React from "react";
import {useNavigate} from "react-router-dom";

export default function UpdateMileageForm({chosenVehicle, setChosenVehicle, setErrors, user}) {

    const navigate = useNavigate();

    function handleSubmit(event){
        event.preventDefault();
        fetch("http://localhost:8080/api/vin", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${user.jwt}`
            },
            body: JSON.stringify(chosenVehicle)
        }).then(response => {
            if (response.status === 200) {
                response.json().then(data => {
                    setChosenVehicle(data);
                    navigate("/fleet_overview")
                })
            }
            if (response.status === 403) {
                localStorage.removeItem("user")
                navigate("/")
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
                navigate("/fleet_overview");
            }
        }).catch(error => {
            setErrors([error.toString()]);
        });
    }

    function handleInputChange(e){
        setChosenVehicle({...chosenVehicle, mileage: e.target.value});
        console.log(chosenVehicle)
    }

    return (
        <Container maxWidth="xs">
            <Box
                component="form"
                onSubmit={handleSubmit}
                sx={{
                    marginTop: 8,
                }}
            >
                <Typography variant="h4" align="center" gutterBottom>
                    Vehicle Details:
                </Typography>
                <Typography variant="body2">
                    Vin: {chosenVehicle.vin}
                </Typography>
                <Typography variant="body2">
                    Mileage: {chosenVehicle.mileage}
                </Typography>
                <Typography variant="body2">
                    {chosenVehicle.make} {chosenVehicle.model} ({chosenVehicle.year})
                </Typography>
                <TextField
                    label="Mileage"
                    name="mileage"
                    fullWidth
                    required
                    mx={2}
                    onChange={handleInputChange}
                />
                <Button type="submit" fullWidth variant="contained" color="primary" sx={{mt: 2}}>
                    Submit
                </Button>

            </Box>
        </Container>
    )
}