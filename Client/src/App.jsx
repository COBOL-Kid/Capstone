import {BrowserRouter, Route, Routes} from "react-router-dom";
import NavBar from "./Components/NavBar.jsx";
import Landing from "./Pages/Landing.jsx";
import React from "react";
import AuthenticationForm from "./Components/AuthenticationForm.jsx";
import FleetOverview from "./Pages/FleetOverview.jsx";

function App() {
    const initialUser = localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : null
    const [user, setUser] = React.useState(initialUser);

    return (
        <BrowserRouter>
            <NavBar/>
            <Routes>
                <Route path="/" element={<Landing/>}/>
                <Route path="/login" element={<AuthenticationForm setUser={setUser}/>}/>
                <Route path="/signup" element={<AuthenticationForm setUser={setUser}/>}/>
                <Route path="/fleet_overview" element={<FleetOverview user={user}/>}/>
            </Routes>
        </BrowserRouter>
    )
}

export default App
