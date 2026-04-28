import { useLocation, useNavigate } from "react-router-dom";
import CardContent from "@mui/material/CardContent";
import Card from "@mui/material/Card";
import CardMedia from "@mui/material/CardMedia";
import { Box, Button, Typography } from "@mui/material";

export default function VinConfirm({ vehicleData, user, setErrors }) {
  const navigate = useNavigate();
  const location = useLocation();

  function handleConfirm() {
    if (location.pathname === "/add_vehicle") {
      fetch("http://localhost:8080/api/vin", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${user.jwt}`,
        },
        body: JSON.stringify(vehicleData),
      })
        .then((response) => {
          if (response.status === 201) {
            navigate("/fleet_overview");
          }
          if (response.status === 403) {
            localStorage.removeItem("user");
            navigate("/");
          } else {
            Promise.reject(`Problem with response. Status: ${response.status}`);
          }
        })
        .catch((error) => {
          setErrors([error.toString()]);
          navigate("/fleet_overview");
        });
    } else {
      fetch(`http://localhost:8080/api/vin/${vehicleData.vinId}`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${user.jwt}`,
        },
      })
        .then((response) => {
          if (response.status === 200) {
            navigate("/fleet_overview");
          }
          if (response.status === 403) {
            localStorage.removeItem("user");
            navigate("/");
          } else {
            Promise.reject(`Problem with response. Status: ${response.status}`);
          }
        })
        .catch((error) => {
          setErrors([error.toString()]);
          navigate("/fleet_overview");
        });
    }
  }

  return (
    <Card>
      <CardMedia
        component="img"
        height="200"
        image={vehicleData.selectedImageUrl || "/honestCar.png"}
        alt="vehicle image"
      />
      <CardContent>
        <Typography variant="h5" component="div">
          {vehicleData.make} {vehicleData.model} ({vehicleData.year})
        </Typography>
        <Typography variant="body2" color="text.secondary">
          VIN: {vehicleData.vin}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Mileage: {vehicleData.currentMileage}
        </Typography>
      </CardContent>

      <Box m={2}>
        <Button variant="contained" color="primary" onClick={handleConfirm}>
          Confirm
        </Button>
        <Button
          variant="contained"
          color="secondary"
          onClick={() => navigate("/fleet_overview")}
          sx={{ ml: 2 }}
        >
          Cancel
        </Button>
      </Box>
    </Card>
  );
}
