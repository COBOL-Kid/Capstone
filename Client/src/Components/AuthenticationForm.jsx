import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button, Container, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import { Errors } from "./Errors.jsx";
import Typography from "@mui/material/Typography";
import { jwtDecode } from "jwt-decode";

function AuthenticationForm({ setUser }) {
  const [userEmail, setUserEmail] = useState("");
  const [userPassword, setUserPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [errors, setErrors] = useState([]);
  const location = useLocation();
  const isSignup = location.pathname === "/signup";
  const navigate = useNavigate();

  const handleSubmit = (event) => {
    event.preventDefault();

    if (isSignup) {
      fetch("http://localhost:8080/api/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({
          firstname: firstName,
          lastname: lastName,
          email: userEmail,
          password: userPassword,
        }),
      })
        .then((response) => {
          if (response.status === 200) {
            response.json().then((json) => {
              const userObject = jwtDecode(json.token);
              userObject.jwt = json.token;
              setUser(userObject);
              localStorage.setItem("user", JSON.stringify(userObject));
              navigate("/fleet_overview");
            });
          } else {
            return Promise.reject();
          }
        })
        .catch((error) => {
          setErrors(["Something Went Wrong"]);
        });
    } else {
      fetch("http://localhost:8080/api/auth/authenticate", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({
          email: userEmail,
          password: userPassword,
        }),
      })
        .then((response) => {
          if (response.status === 200) {
            response.json().then((json) => {
              const userObject = jwtDecode(json.token);
              userObject.jwt = json.token;
              setUser(userObject);
              localStorage.setItem("user", JSON.stringify(userObject));
              navigate("/fleet_overview");
            });
          } else {
            return Promise.reject();
          }
        })
        .catch((error) => {
          setErrors(["Invalid User"]);
        });
    }
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
          {isSignup ? "Sign Up" : "Login"}
        </Typography>
        <Errors errors={errors} />
        {isSignup && (
          <>
            <Box mt={2}>
              <TextField
                label="First Name"
                fullWidth
                required
                mx={2}
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
              />
            </Box>
            <Box mt={2}>
              <TextField
                label="Last Name"
                fullWidth
                required
                mx={2}
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
              />
            </Box>
          </>
        )}
        <Box mt={2}>
          <TextField
            label="Email"
            fullWidth
            required
            mx={2}
            value={userEmail}
            onChange={(e) => setUserEmail(e.target.value)}
          />
        </Box>
        <Box mt={2}>
          <TextField
            label="Password"
            type="password"
            fullWidth
            required
            mx={2}
            value={userPassword}
            onChange={(e) => setUserPassword(e.target.value)}
          />
        </Box>
        <Button
          type="submit"
          fullWidth
          variant="contained"
          color="primary"
          sx={{ mt: 2 }}
        >
          {isSignup ? "Signup" : "Login"}
        </Button>
      </Box>
    </Container>
  );
}

export default AuthenticationForm;
