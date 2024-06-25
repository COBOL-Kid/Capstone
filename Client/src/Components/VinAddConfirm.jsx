import {useLocation, useNavigate} from "react-router-dom";
import CardContent from "@mui/material/CardContent";
import Card from "@mui/material/Card";
import CardMedia from "@mui/material/CardMedia";
import {Box, Button, Typography} from "@mui/material";

export default function VinAddConfirm({vehicleData}) {

    const navigate = useNavigate();

    function handleConfirm() {

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
                <Button variant="contained" color="primary" onClick={handleConfirm} >
                    Confirm
                </Button>
                <Button variant="contained" color="secondary" onClick={() => navigate("/fleet_overview")} sx={{ ml: 2 }}>
                    Cancel
                </Button>
            </Box>
        </Card>
    )
}
