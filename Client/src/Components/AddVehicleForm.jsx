import {Button, Container, TextField} from "@mui/material";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {Errors} from "./Errors.jsx";
import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import VinConfirm from "./VinConfirm.jsx";

export default function AddVehicleForm({user}) {

    const navigate = useNavigate();

    const initialVehicleData = {
        vinId: 0,
        ownerId: user.ownerId,
        vin: "",
        mileage: 1,
        year: 1,
        make: "",
        model: "",
        image: ""

    }

    const [errors, setErrors] = useState([]);
    const [vehicleData, setVehicleData] = useState(initialVehicleData);
    const [isVehicleInfoUpdated, setIsVehicleInfoUpdated] = useState(false);

    useEffect(() => {
        if (vehicleData.year !== initialVehicleData.year) {
            setIsVehicleInfoUpdated(true);
        } else {
            setIsVehicleInfoUpdated(false);
        }
    }, [vehicleData]);

    const handleInputChange = (event) => {
        setVehicleData({
            ...vehicleData,
            [event.target.name]: event.target.value
        });
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        fetch(`http://localhost:8080/api/external/find_vin/${vehicleData.vin}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${user.jwt}`
            }
        }).then(response => {
            if (response.status === 200) {
                response.json().then(data => {
                    setVehicleData({
                        ...vehicleData,
                        year: data.year,
                        make: data.make,
                        model: data.model,
                        image: data.image,
                    });
                })
            }
            if (response.status === 403) {
                localStorage.removeItem("user")
                navigate("/")
            } else {
                Promise.reject(`Problem with response. Status: ${response.status}`);
            }
        }).catch(error => {
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
                <Errors errors={errors}/>
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
                    name="mileage"
                    fullWidth
                    required
                    mx={2}
                    value={vehicleData.mileage}
                    onChange={handleInputChange}
                />
                <Button type="submit" fullWidth variant="contained" color="primary" sx={{mt: 2}}>
                    Submit
                </Button>

                {isVehicleInfoUpdated &&
                    <Box sx={{mt: 2}}>
                        <VinConfirm vehicleData={vehicleData} user={user} setErrors={setErrors}/>
                    </Box>
                }
            </Box>
        </Container>
    )
}
