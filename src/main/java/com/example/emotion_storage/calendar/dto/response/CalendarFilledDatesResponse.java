package com.example.emotion_storage.calendar.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CalendarFilledDatesResponse(
        int totalDates,
        List<LocalDate> dates
) {}
