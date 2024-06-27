import { Dialog, DialogTitle, DialogContent, List, ListItem } from '@mui/material';
import * as React from 'react';

export const Errors = ({ errors }) => {
    const [open, setOpen] = React.useState(false);

    React.useEffect(() => {
        if (errors && errors.length > 0) {
            setOpen(true);
        } else {
            setOpen(false);
        }
    }, [errors]);

    const handleClose = () => {
        setOpen(false);
    };

    if (!errors || errors.length === 0) {
        return null;
    }

    return (
        <Dialog open={open} onClose={handleClose} aria-labelledby="alert-dialog-title" aria-describedby="alert-dialog-description">
            <DialogTitle id="alert-dialog-title">{"An Error Occurred"}</DialogTitle>
            <DialogContent>
                <List>
                    {errors.map((error, index) => (
                        <ListItem key={index}>{error}</ListItem>
                    ))}
                </List>
            </DialogContent>
        </Dialog>
    );
};