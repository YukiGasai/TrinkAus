import { useState } from "react";
import { Card, IconButton, Typography, Modal, Box } from "@mui/material";
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { addMonths, format, isSameMonth, subMonths } from "date-fns";
import { historyDate } from "../../helper/signals";
import ArrowForwardIos from "@mui/icons-material/ArrowForwardIos";
import ArrowBackIos from "@mui/icons-material/ArrowBackIos";
import { AppDarkTheme } from "../../helper/theme";

export const MonthSelector = () => {
    const [modalOpen, setModalOpen] = useState(false);
    const handleDateChange = (date: Date | null) => {
        if (date) {
            historyDate.value = date;
            setModalOpen(false);
        }
    };
    return (
        <>
            <Card sx={{
                padding: 0.5,
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center',
                width: '80%',
                justifyContent: 'space-between',
                paddingLeft: 2,
                paddingRight: 2,
                marginBottom: 2,
            }}>
                <IconButton aria-label="Minus one month" onClick={() => {
                    historyDate.value = subMonths(historyDate.peek(), 1);
                }}>
                    <ArrowBackIos sx={{
                        color: AppDarkTheme.palette.primary.main,
                        width: 16,
                        height: 16,
                    }} />
                </IconButton>
                <Typography variant="h6" component="h2" sx={{ textTransform: 'capitalize', 'cursor': 'pointer' }} onClick={() => setModalOpen(true)}>
                    {format(historyDate.value, 'MMMM yyyy')}
                </Typography>
                <IconButton aria-label="Plus one month" disabled={isSameMonth(historyDate.value, new Date())} onClick={() => {
                    historyDate.value = addMonths(historyDate.peek(), 1);
                }}>
                    <ArrowForwardIos sx={{
                        width: 16,
                        height: 16,
                        color: AppDarkTheme.palette.primary.main,
                        opacity: isSameMonth(historyDate.value, new Date()) ? 0 : 1
                    }} />
                </IconButton>
            </Card>

        <Modal
            open={modalOpen}
            onClose={() => setModalOpen(false)}
            aria-labelledby="modal-modal-title"
            aria-describedby="modal-modal-description"
        >
            <Box sx={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                display: 'flex',
                flexDirection: 'column',
                width: 400,
                bgcolor: 'background.paper',
                border: '2px solid #000',
                borderRadius: 4,
                boxShadow: 24,
                p: 4,
            }}>
                <Typography id="modal-modal-title" variant="h6" component="h2" sx={{ marginBottom: 2 }}>
                    Select a Date
                </Typography>
                    <DatePicker
                        views={["year", "month"]}
                        value={historyDate.value}
                        onChange={handleDateChange}
                        disableFuture
                        sx={{ mt: 2 }}
                    />
                </Box>
            </Modal>
        </>
    )
}
