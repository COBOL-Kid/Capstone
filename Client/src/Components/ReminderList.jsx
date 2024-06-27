import {useState} from 'react';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import ListItemSecondaryAction from '@mui/material/ListItemSecondaryAction';
import IconButton from '@mui/material/IconButton';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import TextField from '@mui/material/TextField';
import {useNavigate} from "react-router-dom";

export default function ReminderList({reminder, user, setErrors, setChange}) {
    const [reminderDialogOpen, setReminderDialogOpen] = useState(false);
    const [reminderDate, setReminderDate] = useState(reminder.reminderDate);
    const navigate = useNavigate();

    const today = new Date();
    const todayDay = String(today.getDate()).padStart(2, '0');
    const todayMonth = String(today.getMonth() + 1).padStart(2, '0');
    const todayYear = today.getFullYear();

    const todayFormatted = todayYear + '-' + todayMonth + '-' + todayDay;

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
        setReminderDialogOpen(false);

        const updatedReminder = {...reminder, reminderDate};

        fetch("http://localhost:8080/api/reminder", {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${user.jwt}`,
            },
            body: JSON.stringify(updatedReminder)
        })
            .then(response => {
                if (response.status === 200) {
                    setChange(prevChange => !prevChange);
                }
                if (response.status === 403) {
                    localStorage.removeItem("user")
                    navigate("/")
                } else {
                    Promise.reject(`Problem with response. Status: ${response.status}`);
                }
            }).catch(error => {
            setErrors([error.toString()]);
        });
    };

    const handleReminderDelete = () => {
        fetch(`http://localhost:8080/api/reminder/delete/${reminder.reminderId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${user.jwt}`,
            }
        })
            .then(response => {
                if (response.status === 200) {
                    setChange(prevChange => !prevChange);
                }
                if (response.status === 403) {
                    localStorage.removeItem("user")
                    navigate("/")
                } else {
                    Promise.reject(`Problem with response. Status: ${response.status}`);
                }
            }).catch(error => {
            setErrors([error.toString()]);
        });
    }

    return (
        <div>
            <ListItem>
                <ListItemText
                    primary={reminder.description}
                    secondary={reminderDate}
                />
                <ListItemSecondaryAction>
                    <IconButton edge="end" aria-label="edit" onClick={handleReminderClick}>
                        <EditIcon/>
                    </IconButton>
                    {/* todo delete handler to IconButton onClick */}
                    <IconButton edge="end" aria-label="delete" onClick={handleReminderDelete}>
                        <DeleteIcon/>
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
                    inputProps={{
                        min: todayFormatted,
                    }}
                />
                <button onClick={handleReminderConfirm}>Confirm</button>
            </Dialog>
        </div>
    );
}
