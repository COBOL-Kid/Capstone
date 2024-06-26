import { useState } from 'react';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import ListItemSecondaryAction from '@mui/material/ListItemSecondaryAction';
import IconButton from '@mui/material/IconButton';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import TextField from '@mui/material/TextField';

export default function ReminderList({ reminder }) {
    const [reminderDialogOpen, setReminderDialogOpen] = useState(false);
    const [reminderDate, setReminderDate] = useState(reminder.reminderDate);

    const handleReminderClick = () => {
        setReminderDialogOpen(true);
    };

    const handleReminderDialogClose = () => {
        setReminderDialogOpen(false);
    };

    const handleReminderDateChange = (e) => {
        setReminderDate(e.target.value);
    };

    const handleReminderConfirm = () => {
        // todo handle your reminder update
        setReminderDialogOpen(false);
    };

    return (
        <div>
            <ListItem>
                <ListItemText
                    primary={reminder.description}
                    secondary={reminderDate}
                />
                <ListItemSecondaryAction>
                    <IconButton edge="end" aria-label="edit" onClick={handleReminderClick}>
                        <EditIcon />
                    </IconButton>
                    {/* todo delete handler to IconButton onClick */}
                    <IconButton edge="end" aria-label="delete">
                        <DeleteIcon />
                    </IconButton>
                </ListItemSecondaryAction>
            </ListItem>

            <Dialog open={reminderDialogOpen} onClose={handleReminderDialogClose}>
                <DialogTitle>Set Reminder Date</DialogTitle>
                <TextField
                    id="date"
                    type="date"
                    value={reminderDate}
                    InputLabelProps={{
                        shrink: true,
                    }}
                    onChange={handleReminderDateChange}
                />
                <button onClick={handleReminderConfirm}>Confirm</button>
            </Dialog>
        </div>
    );
}
