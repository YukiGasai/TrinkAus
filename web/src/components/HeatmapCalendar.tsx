import React, { useMemo } from 'react';
import { Box, Paper, Typography, CircularProgress, Stack } from '@mui/material';
import { motion, AnimatePresence } from 'framer-motion';
import { startOfMonth, getDaysInMonth, getDay, format, isSameDay } from 'date-fns';
import { darkThemeColors } from '../helper/theme';

interface HeatmapCalendarProps {
  yearMonth: Date;
  data: Record<string, number>;
  minValue: number;
  maxValue: number;
  onDayClick: (date: Date, value: number | undefined) => void;
  isLoading?: boolean;
  colors?: {
    empty: string;
    start: string;
    end: string;
  };
}

const containerVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.02 } },
};
const cellVariants = {
  hidden: { scale: 0.5 },
  visible: { scale: 1 },
};

export const HeatmapCalendar: React.FC<HeatmapCalendarProps> = ({
  yearMonth,
  data,
  minValue,
  maxValue,
  onDayClick,
  isLoading = false,
  colors = {
    empty: 'rgba(255, 255, 255, 0.1)',
    start: darkThemeColors.primaryContainer,
    end: darkThemeColors.primary,
  },
}) => {
  const calendarGrid = useMemo(() => {
    const firstDay = startOfMonth(yearMonth);
    const daysInMonth = getDaysInMonth(yearMonth);
    const firstDayOfWeekOffset = (getDay(firstDay) + 6) % 7;
    const emptyCells = Array.from({ length: firstDayOfWeekOffset }, (_, i) => ({
      key: `empty-${i}`, type: 'empty' as const
    }));
    const dayCells = Array.from({ length: daysInMonth }, (_, i) => {
      const dayNumber = i + 1;
      const date = new Date(yearMonth.getFullYear(), yearMonth.getMonth(), dayNumber);
      const dateString = format(date, 'yyyy-MM-dd');
      return {
        key: dateString, type: 'day' as const, date, dayNumber, value: data[dateString]
      };
    });
    return [...emptyCells, ...dayCells];
  }, [yearMonth, data]);

  if (isLoading) {
    return (
      <Stack
        direction="row"
        justifyContent="center"
        alignItems="center"
        sx={{ height: '100%', width: '100%' }}
      >
        <CircularProgress />
      </Stack>
    );
   }

  return (
    <Box
      component={motion.div}
      variants={containerVariants}
      initial="hidden"
      animate="visible"
      display="grid"
      width={ '100%'}
      gridTemplateColumns="repeat(7, 1fr)"
      gap={0.5}
    >
      <AnimatePresence>
        {calendarGrid.map((cell) =>
          cell.type === 'day' ? (
            <DayCell
              key={cell.key}
              date={cell.date}
              dayNumber={cell.dayNumber}
              value={cell.value}
              minValue={minValue}
              maxValue={maxValue}
              colors={colors}
              onClick={() => onDayClick(cell.date, cell.value)}
            />
          ) : ( <Box key={cell.key} sx={{ aspectRatio: '1 / 1' }} /> ),
        )}
      </AnimatePresence>
    </Box>
  );
};

interface DayCellProps {
  date: Date;
  dayNumber: number;
  value: number | undefined;
  minValue: number;
  maxValue: number;
  colors: { empty: string; start: string; end: string };
  onClick: () => void;
}

const DayCell: React.FC<DayCellProps> = ({
  date,
  dayNumber,
  value,
  minValue,
  maxValue,
  colors,
  onClick,
}) => {
  const { backgroundColor, textColor } = useMemo(() => {
    if (value === undefined || value <= 0) {
      return { backgroundColor: colors.empty, textColor: 'rgba(255,255,255,0.5)' };
    }

    const range = maxValue - minValue;
    if (range <= 0) {
      return { backgroundColor: colors.end, textColor: 'black' };
    }

    const fraction = Math.max(0, Math.min(1, (value - minValue) / range));

    const bgColor = interpolate(colors.start, colors.end, fraction);
    const txtColor = fraction > 0.5 ? 'black' : 'white';

    return { backgroundColor: bgColor, textColor: txtColor };
  }, [value, minValue, maxValue, colors]);

  const isToday = isSameDay(date, new Date());

  return (
    <Box component={motion.div} variants={cellVariants} sx={{ aspectRatio: '1 / 1' }}>
      <Paper
        variant="outlined"
        onClick={onClick}
        sx={{
          height: '100%',
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          background: backgroundColor,
          border: isToday ? `2px solid ${colors.end}` : '1px solid rgba(255,255,255,0.1)',
          transition: 'transform 0.2s ease-in-out',
          '&:hover': {
            transform: 'scale(1.1)',
            zIndex: 10,
          },
        }}
      >
        <Typography
          variant="caption"
          sx={{ color: textColor, fontWeight: isToday ? 'bold' : 'normal' }}
        >
          {dayNumber}
        </Typography>
      </Paper>
    </Box>
  );
};

function interpolate(color1: string, color2: string, percent: number): string {
  const r1 = parseInt(color1.substring(1, 3), 16);
  const g1 = parseInt(color1.substring(3, 5), 16);
  const b1 = parseInt(color1.substring(5, 7), 16);

  const r2 = parseInt(color2.substring(1, 3), 16);
  const g2 = parseInt(color2.substring(3, 5), 16);
  const b2 = parseInt(color2.substring(5, 7), 16);

  const r = Math.round(r1 + (r2 - r1) * percent);
  const g = Math.round(g1 + (g2 - g1) * percent);
  const b = Math.round(b1 + (b2 - b1) * percent);

  return(
    "#" +
    r.toString(16).padStart(2, "0") +
    g.toString(16).padStart(2, "0") +
    b.toString(16).padStart(2, "0")
  );
}
