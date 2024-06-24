import React from 'react';
import { Container, Typography, Box, Button } from '@mui/material';
import Image from '../assets/CarMaintenance.jpg';

const LandingPage = () => {
  return (
    <Container>
      <Box sx={{ my: 2, textAlign: 'center' }}>
        <Typography variant="h2" component="h1" gutterBottom>
          HonestCar
        </Typography>
        <Box
          component="img"
          src={Image}
          sx={{ width: '100%', maxWidth: 600, height: 'auto' }}
        />
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
          <Button variant="contained" color="primary" sx={{ mx: 1 }}>
            Login
          </Button>
          <Button variant="contained" color="secondary" sx={{ mx: 1 }}>
            Sign Up
          </Button>
        </Box>
      </Box>
    </Container>
  );
};

export default LandingPage;