import {BrowserRouter, Route, Routes} from "react-router-dom";
import NavBar from "./Components/NavBar.jsx";
import Landing from "./Pages/Landing.jsx";
import React, {useState} from "react";
import AuthenticationForm from "./Components/AuthenticationForm.jsx";
import FleetOverview from "./Pages/FleetOverview.jsx";
import AddVehicleForm from "./Components/AddVehicleForm.jsx";
import VehicleOverview from "./Pages/VehicleOverview.jsx";

function App() {
    const initialUser = localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : null
    const [user, setUser] = React.useState(initialUser);
    const [vehicles, setVehicles] = useState([]);
    const [chosenVehicle, setChosenVehicle] = useState({})

    return (
        <BrowserRouter>
            <NavBar/>
            <Routes>
                <Route path="/" element={<Landing/>}/>
                <Route path="/login" element={<AuthenticationForm setUser={setUser}/>}/>
                <Route path="/signup" element={<AuthenticationForm setUser={setUser}/>}/>
                <Route path="/fleet_overview"
                       element={<FleetOverview user={user} vehicles={vehicles} setVehicles={setVehicles}
                                               setChosenVehicle={setChosenVehicle}/>}/>
                <Route path="/add_vehicle" element={<AddVehicleForm user={user}/>}/>
                <Route path="/vehicle_overview" element={<VehicleOverview user={user} chosenVehicle={chosenVehicle} setChosenVehicle={setChosenVehicle}/>}/>
            </Routes>
        </BrowserRouter>
    );
}

export default App
