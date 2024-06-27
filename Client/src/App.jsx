import {BrowserRouter, Navigate, Route, Routes} from "react-router-dom";
import NavBar from "./Components/NavBar.jsx";
import Landing from "./Pages/Landing.jsx";
import React, {useState} from "react";
import AuthenticationForm from "./Components/AuthenticationForm.jsx";
import FleetOverview from "./Pages/FleetOverview.jsx";
import AddVehicleForm from "./Components/AddVehicleForm.jsx";
import VehicleOverview from "./Pages/VehicleOverview.jsx";
import Maintenance from "./Pages/Maintenance.jsx";

function App() {
    const initialUser = localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : null
    const [user, setUser] = React.useState(initialUser);
    const [vehicles, setVehicles] = useState([]);
    const [chosenVehicle, setChosenVehicle] = useState({})
    const protectedRoute = <Navigate to='/'/>;

    return (

        <BrowserRouter>
            <NavBar user={user}/>
            <Routes>
                <Route path="/" element={<Landing/>}/>
                <Route path="/login" element={<AuthenticationForm setUser={setUser}/>}/>
                <Route path="/signup" element={<AuthenticationForm setUser={setUser}/>}/>
                <Route path="/fleet_overview"
                       element={user ? <FleetOverview user={user} vehicles={vehicles} setVehicles={setVehicles}
                                                      setChosenVehicle={setChosenVehicle}/> : protectedRoute}/>
                <Route path="/add_vehicle" element={user ? <AddVehicleForm user={user}/> : protectedRoute}/>
                <Route path="/vehicle_overview"
                       element={user ? <VehicleOverview user={user} chosenVehicle={chosenVehicle}
                                                        setChosenVehicle={setChosenVehicle}
                       /> : protectedRoute}/>
                <Route path="/maintenance_list"
                       element={user ? <Maintenance chosenVehicle={chosenVehicle} user={user}
                       /> : protectedRoute}/>
            </Routes>
        </BrowserRouter>

    );
}

export default App
