package com.realestate.calendar;

import com.realestate.calendar.dto.CalendarEventResponse;
import com.realestate.calendar.model.CalendarEventType;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalendarController.class)
@Import({SecurityConfig.class, CalendarExceptionHandler.class})
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalendarService calendarService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void authenticatedBrokerCanRetrieveCalendarEventsWithDateRange() throws Exception {
        when(calendarService.events("broker@example.local",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")))
                .thenReturn(List.of(new CalendarEventResponse(
                        "FOLLOW_UP-9", CalendarEventType.FOLLOW_UP, "Call buyer",
                        LocalDate.parse("2026-10-08"), LocalTime.parse("09:00:00"), null,
                        "PENDING", "HIGH", 5L, "Buyer", null, null, 9L, "Check interest"
                )));

        mockMvc.perform(get("/api/calendar/events")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("FOLLOW_UP"))
                .andExpect(jsonPath("$[0].relatedLeadId").value(5))
                .andExpect(jsonPath("$[0].date").value("2026-10-08"));

        verify(calendarService).events("broker@example.local",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
    }

    @Test
    void unauthenticatedUserCannotRetrieveCalendar() throws Exception {
        mockMvc.perform(get("/api/calendar/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonBrokerCannotRetrieveCalendar() throws Exception {
        mockMvc.perform(get("/api/calendar/events")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }
}
