import { Box, Button, SvgIcon } from "@mui/material";
import { addHydrationAmountLarge, addHydrationAmountMedium, addHydrationAmountSmall } from "../../helper/signals";
import { AppDarkTheme } from "../../helper/theme";
import { handleAddHydration } from "../../helper/handleAddHydration";
import IconSmall from '../../assets/martini.svg?react'
import IconMedium from '../../assets/glass-water.svg?react'
import IconLarge from '../../assets/milk.svg?react'

export function AddButtons() {

    return (
        <Box sx={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-evenly', position: 'relative', width: '100%' }}>
            <Button variant="contained" color="primary"
                style={{ backgroundColor: AppDarkTheme.palette.primary.dark, borderRadius: 8 }}
                onClick={() => handleAddHydration(addHydrationAmountSmall.value)}>
                <SvgIcon component={IconSmall} inheritViewBox sx={{
                    background: 'transparent',
                    fill: 'transparent'
                }} />
            </Button>
            <Button variant="contained" color="primary"
                style={{ backgroundColor: AppDarkTheme.palette.primary.dark, borderRadius: 8 }}
                onClick={() => handleAddHydration(addHydrationAmountMedium.value)}>
                <SvgIcon component={IconMedium} inheritViewBox sx={{
                    background: 'transparent',
                    fill: 'transparent'
                }} />
            </Button>
            <Button variant="contained" color="primary"
                style={{ backgroundColor: AppDarkTheme.palette.primary.dark, borderRadius: 8 }}
                onClick={() => handleAddHydration(addHydrationAmountLarge.value)}>
                <SvgIcon component={IconLarge} inheritViewBox sx={{
                    background: 'transparent',
                    fill: 'transparent'
                }} />
            </Button>
        </Box>
    );
}
