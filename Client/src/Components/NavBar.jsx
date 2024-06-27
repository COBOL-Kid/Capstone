import React from 'react';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import MenuItem from '@mui/material/MenuItem';
import Menu from '@mui/material/Menu';
import MenuIcon from '@mui/icons-material/Menu';
import {Link, useNavigate} from "react-router-dom";


export default function NavBar({user}) {
    const [anchorEl, setAnchorEl] = React.useState(null);
    const navigate = useNavigate();
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
                    <img src={"/honestCar.png"} alt="logo" style={{width: '40px', height: '40px'}}/>
                </IconButton>
                <Typography variant="h6" style={{flexGrow: 1, textAlign: 'center'}}>
                    Honest Car
                </Typography>
                <div>
                    {user && (
                        <React.Fragment>
                            <IconButton
                                aria-label="account of current user"
                                aria-controls="menu-appbar"
                                aria-haspopup="true"
                                onClick={handleMenu}
                                color="inherit"
                            >
                                <MenuIcon/>
                            </IconButton>
                            <Menu
                                id="menu-appbar"
                                anchorEl={anchorEl}
                                keepMounted
                                open={Boolean(anchorEl)}
                                onClose={handleClose}
                            >
                                <MenuItem component={Link} to="/" onClick={handleClose}>
                                    Home
                                </MenuItem>
                                <MenuItem component={Link} to="/fleet_overview" onClick={handleClose}>
                                    Fleet Summary
                                </MenuItem>
                                <MenuItem component={Link} to="/" onClick={() => {
                                    localStorage.removeItem("user");
                                    handleClose();
                                }}>
                                    Log Out
                                </MenuItem>
                            </Menu>
                        </React.Fragment>
                    )}
                </div>
            </Toolbar>
        </AppBar>
    );
}