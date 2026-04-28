import { Button, Container, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import { Errors } from "./Errors.jsx";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function AddVehicleForm({ user }) {
  const navigate = useNavigate();

  const initialVehicleData = {
    vin: "",
    currentMileage: 1,
  };

  const [errors, setErrors] = useState([]);
  const [vehicleData, setVehicleData] = useState(initialVehicleData);

  const handleInputChange = (event) => {
    setVehicleData({
      ...vehicleData,
      [event.target.name]: event.target.value,
    });
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    fetch("http://localhost:8080/api/vin", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${user.jwt}`,
      },
      body: JSON.stringify(vehicleData),
    })
      .then((response) => {
        if (response.status === 201 || response.status === 200) {
          navigate("/fleet_overview");
        } else if (response.status === 403) {
          localStorage.removeItem("user");
          navigate("/");
        } else {
          return response.text().then((message) => {
            throw new Error(message || `Problem with response. Status: ${response.status}`);
          });
        }
      })
      .catch((error) => {
        setErrors([error.toString()]);
      });
  };

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
          Please Enter Vehicle Details:
        </Typography>
        <Errors errors={errors} />
        <TextField
          label="Vin"
          name="vin"
          fullWidth
          required
          mx={2}
          value={vehicleData.vin}
          onChange={handleInputChange}
        />
        <TextField
          label="Mileage"
          name="currentMileage"
          fullWidth
          required
          mx={2}
          value={vehicleData.currentMileage}
          onChange={handleInputChange}
        />
        <Button
          type="submit"
          fullWidth
          variant="contained"
          color="primary"
          sx={{ mt: 2 }}
        >
          Submit
        </Button>
      </Box>
    </Container>
  );
}
