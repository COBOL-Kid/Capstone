import { Box, Button, Container, Typography } from "@mui/material";
import Image from "../assets/CarMaintenance.jpg";
import { useNavigate } from "react-router-dom";

const LandingPage = () => {
  const navigate = useNavigate();

  return (
    <Container>
      <Box sx={{ my: 2, textAlign: "center" }}>
        <Typography variant="h2" component="h1" gutterBottom>
          Honest Car
        </Typography>
        <Box
          component="img"
          src={Image}
          sx={{ width: "100%", maxWidth: 600, height: "auto" }}
        />
        <Box sx={{ display: "flex", justifyContent: "center", mt: 2 }}>
          <Button
            variant="contained"
            color="primary"
            sx={{ mx: 1 }}
            onClick={() => navigate("/login")}
          >
            Login
          </Button>
          <Button
            variant="contained"
            color="secondary"
            sx={{ mx: 1 }}
            onClick={() => navigate("/signup")}
          >
            Sign Up
          </Button>
        </Box>
      </Box>
    </Container>
  );
};

export default LandingPage;
