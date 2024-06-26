import {useEffect, useState} from 'react';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import IconButton from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/Delete';
import {Button, Typography} from "@mui/material";


export default function MaintenanceList() {
    const [data, setData] = useState([]);

    useEffect(() => {
        fetch(`http://localhost:8080/api/external/find_maintenance/1GNALDEK9FZ108495`,
            { method: "GET", headers: { contentType: 'application/json' } }
        )
            .then(response => response.json())
            .then(data => {
                setData(data);
                console.log(data);
            })
    }, []);

    function handleAddClick() {

    }

    return (
        <List sx={{ width: '100%', bgcolor: 'background.paper', marginTop: 2 }}>
            { data.map(item => (
                <ListItem key={item.desc}
                          secondaryAction={
                              <Button variant="contained" color="success" onClick={() => handleAddClick(item)}>
                                  Add
                              </Button>
                          }
                          sx={{
                              bgcolor: 'background.paper',
                              border: 1,
                              borderColor: 'divider',
                              borderRadius: 2,
                              m: 1
                          }}
                >
                    <ListItemText
                        primary={<Typography variant="h5">{item.desc}</Typography>}
                        secondary={
                            <>
                                <Typography variant="body2">
                                    Due mileage: {item.due_mileage}
                                </Typography>
                                <Typography variant="body2">
                                    Total cost: {item.repair.total_cost}
                                </Typography>
                                <Typography variant="body1">
                                    Parts:
                                </Typography>
                                {item.parts && item.parts.map(part => (
                                    <Typography key={part.desc} variant="body2">
                                        Part desc: {part.desc}, Price: {part.price}, Qty: {part.qty}
                                    </Typography>
                                ))}
                            </>
                        }
                    />
                </ListItem>
            ))}
        </List>
    );
}