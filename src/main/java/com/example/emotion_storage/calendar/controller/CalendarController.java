package com.example.emotion_storage.calendar.controller;

import com.example.emotion_storage.calendar.dto.response.CalendarFilledDatesResponse;
import com.example.emotion_storage.calendar.service.CalendarService;
import com.example.emotion_storage.global.api.ApiResponse;
import com.example.emotion_storage.global.api.SuccessMessage;
import com.example.emotion_storage.global.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "캘린더 관련 API")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/date")
    @Operation(
            summary = "캘린더 상 타임캡슐 및 일일리포트 존재 날짜 조회",
            description = "yyyy년 MM월의 날짜 중 타임캡슐 또는 일일리포트가 존재하는 날짜 목록을 반환합니다."
    )
    public ResponseEntity<ApiResponse<CalendarFilledDatesResponse>> getCalendarFilledDates(
            @RequestParam int year, @RequestParam int month, @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : 1L; // TODO: 개발 테스트를 위한 코드
        log.info("사용자 {}의 {}년 {}월의 타임캡슐 및 일일리포트 목록 조회를 요청받았습니다.", userId, year, month);
        CalendarFilledDatesResponse response = calendarService.getExistDates(year, month, userId);
        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_CALENDAR_DATES_SUCCESS.getMessage(), response)
        );
    }
}
