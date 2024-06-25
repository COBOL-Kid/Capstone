import {useNavigate} from "react-router-dom";
import CardContent from "@mui/material/CardContent";
import Card from "@mui/material/Card";
import CardMedia from "@mui/material/CardMedia";
import {Box, Button, Typography} from "@mui/material";

export default function VinAddConfirm({vehicleData, user, setErrors}) {

    const navigate = useNavigate();

    function handleConfirm() {
        fetch("http://localhost:8080/api/vin", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${user.jwt}`
            },
            body: JSON.stringify(vehicleData)
        }).then(response => {
            if (response.status === 201) {
                navigate("/fleet_overview");
            }
            if (response.status === 403) {
                localStorage.removeItem("user")
                navigate("/");
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
            }
        }).catch(error => {
            setErrors([error.toString()])
            navigate("/add_vehicle");
        })
    }

    return (
        <Card>
            <CardMedia
                component="img"
                height="140"
                image={vehicleData.vehicleInfo.image}
                alt="vehicle image"
            />
            <CardContent>
                <Typography variant="h5" component="div">
                    {vehicleData.vehicleInfo.make} {vehicleData.vehicleInfo.model} ({vehicleData.vehicleInfo.year})
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    VIN: {vehicleData.vin}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Mileage: {vehicleData.mileage}
                </Typography>
            </CardContent>

            <Box m={2}>
                <Button variant="contained" color="primary" onClick={handleConfirm}>
                    Confirm
                </Button>
                <Button variant="contained" color="secondary" onClick={() => navigate("/fleet_overview")} sx={{ml: 2}}>
                    Cancel
                </Button>
            </Box>
        </Card>
    )
}
