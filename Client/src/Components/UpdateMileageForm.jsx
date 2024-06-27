import {Button, Container, TextField} from "@mui/material";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";

export default function UpdateMileageForm({chosenVehicle, setChosenVehicle, setErrors, user, setIsUpdateClicked}) {

    const navigate = useNavigate();
    const [fixedMiles, setFixedMiles] = useState(0);
    const [change, setChange] = useState(true);

    function handleSubmit(event) {
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
                    setIsUpdateClicked(false);
                    setChange(prevChange => !prevChange);
                })
            } else if (response.status === 403) {
                localStorage.removeItem("user")
                navigate("/")
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
                setIsUpdateClicked(false)
            }
        }).catch(error => {
            setErrors(error);
        });
    }

    useEffect(() => {
        setFixedMiles(chosenVehicle.mileage);
    },[change])

    function handleInputChange(e) {
        setChosenVehicle({...chosenVehicle, mileage: e.target.value});
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
                    Mileage: {fixedMiles}
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