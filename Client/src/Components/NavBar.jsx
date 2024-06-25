import React from 'react';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import MenuItem from '@mui/material/MenuItem';
import Menu from '@mui/material/Menu';
import MenuIcon from '@mui/icons-material/Menu';
import LogoIcon from '@mui/icons-material/Flare';
import {Link} from "react-router-dom";

export default function NavBar() {
    const [anchorEl, setAnchorEl] = React.useState(null);

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    return (
        <AppBar position="static">
            <Toolbar>
                <IconButton edge="start" color="inherit" aria-label="Logo">
                    <LogoIcon />
                </IconButton>
                <Typography variant="h6" style={{ flexGrow: 1, textAlign: 'center' }}>
                    Website Title
                </Typography>
                <div>
                    <IconButton
                        aria-label="account of current user"
                        aria-controls="menu-appbar"
                        aria-haspopup="true"
                        onClick={handleMenu}
                        color="inherit"
                    >
                        <MenuIcon />
                    </IconButton>
                    <Menu
                        id="menu-appbar"
                        anchorEl={anchorEl}
                        keepMounted
                        open={Boolean(anchorEl)}
                        onClose={handleClose}
                    >
                        <MenuItem component={Link} to="/" onClick={handleClose}>Home</MenuItem>
                        <MenuItem component={Link} to="/fleet_overview" onClick={handleClose}>Fleet Summary</MenuItem>
                    </Menu>
                </div>
            </Toolbar>
        </AppBar>
    );
}