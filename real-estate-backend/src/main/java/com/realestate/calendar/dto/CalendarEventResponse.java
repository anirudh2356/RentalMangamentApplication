package com.realestate.calendar.dto;

import com.realestate.calendar.model.CalendarEventType;

import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarEventResponse(
        String id,
        CalendarEventType type,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String priority,
        Long relatedLeadId,
        String relatedLeadName,
        Long relatedPropertyId,
        String relatedPropertyTitle,
        Long sourceId,
        String details
) {
}
