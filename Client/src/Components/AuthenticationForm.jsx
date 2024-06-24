import React from "react";
import {useLocation} from "react-router-dom";
import {Button, Container, TextField} from "@mui/material";
import Box from '@mui/material/Box';

function AuthenticationForm() {

    const location = useLocation();
    const isSignup = location.pathname === '/signup';



    const handleSubmit = (event) => {
        event.preventDefault();
        // handle form submission
        if (isSignup) {
            // handle signup
        } else {
            // handle login
        }
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
                {isSignup && (
                    <>
                        <TextField label="First Name" fullWidth required mx={2}/>
                        <TextField label="Last Name" fullWidth required mx={2}/>
                    </>
                )}
                <TextField label="Email" fullWidth required mx={2}/>
                <TextField label="Password" type="password" fullWidth required mx={2}/>
                <Button type="submit" fullWidth variant="contained" color="primary" sx={{mt: 2}}>
                    {isSignup ? "Signup" : "Login"}
                </Button>
            </Box>
        </Container>
    );
}

export default AuthenticationForm;