import {List, ListItem} from '@mui/material';

export const Errors = ({errors}) => {
    if (!errors) {
        return null;
    }

    return (
        <List>
            {errors.map((error, index) => (
                <ListItem key={index}>
                    {error}
                </ListItem>
            ))}
        </List>
    );
};
