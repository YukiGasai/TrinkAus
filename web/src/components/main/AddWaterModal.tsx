import { Modal, Box, Typography, TextField, InputAdornment, Button } from "@mui/material";
import { toast } from "react-toastify";
import { getUnitStringWithValue } from "../../helper/unitHelper";
import { useState } from "react";
import WaterDropIcon from '@mui/icons-material/WaterDrop';
import { handleAddHydration } from "../../helper/handleAddHydration";

export const AddWaterModal = ({
    open,
    setOpen,
}: {
    open: boolean;
    setOpen: (open: boolean) => void;
}) => {

    const [customWaterAmount, setCustomWaterAmount] = useState<number | null>(null);

    return (
        <Modal
            open={open}
            onClose={() => setOpen(false)}
            aria-labelledby="modal-modal-title"
            aria-describedby="modal-modal-description"
        >
            <Box sx={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: 400,
                bgcolor: 'background.paper',
                border: '2px solid #000',
                borderRadius: 4,
                boxShadow: 24,
                p: 4,
            }}>
                <Typography id="modal-modal-title" variant="h6" component="h2" sx={{ marginBottom: 2 }}>
                    Add Custom Water amoung
                </Typography>
                <TextField
                    required
                    fullWidth
                    label="Water Amount"
                    value={customWaterAmount || ''}
                    onChange={(e) => setCustomWaterAmount(e.target.value ? parseFloat(e.target.value) : null)}
                    placeholder="541"
                    type="number"
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <WaterDropIcon />
                            </InputAdornment>
                        ),
                    }}
                />

                <Button
                    variant="contained"
                    color="primary"
                    fullWidth
                    disabled={customWaterAmount === null || customWaterAmount <= 0}
                    sx={{ marginTop: 2 }}
                    onClick={() => {
                        if (customWaterAmount === null || customWaterAmount <= 0) {
                            toast.error("Please enter a valid amount.");
                            return;
                        }
                        setOpen(false);
                        setCustomWaterAmount(null);
                        handleAddHydration(customWaterAmount);
                    }}
                >Add {getUnitStringWithValue(customWaterAmount || 541)}</Button>
            </Box>
        </Modal>
    )

}
