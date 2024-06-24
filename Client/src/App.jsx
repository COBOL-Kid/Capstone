import {BrowserRouter, Route, Routes} from "react-router-dom";
import NavBar from "./Components/NavBar.jsx";
import Landing from "./Pages/Landing.jsx";
import React from "react";
import AuthenticationForm from "./Components/AuthenticationForm.jsx";
import Cookies from "universal-cookie";

function App() {
    const cookies = new Cookies();
    const initialUser = cookies.get("user") ? JSON.parse(cookies.get("user")) : null;
    const [user, setUser] = React.useState(initialUser);

    return (
        <BrowserRouter>
            <NavBar/>
            <Routes>
                <Route path="/" element={<Landing/>}/>
                <Route path="/login" element={<AuthenticationForm/>}/>
                <Route path="/signup" element={<AuthenticationForm/>}/>
            </Routes>
        </BrowserRouter>
    )
}

export default App
