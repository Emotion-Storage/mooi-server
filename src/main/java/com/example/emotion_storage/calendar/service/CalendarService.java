package com.example.emotion_storage.calendar.service;

import com.example.emotion_storage.calendar.dto.response.CalendarFilledDatesResponse;
import com.example.emotion_storage.report.repository.ReportRepository;
import com.example.emotion_storage.timecapsule.repository.TimeCapsuleRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final TimeCapsuleRepository timeCapsuleRepository;
    private final ReportRepository reportRepository;

    public CalendarFilledDatesResponse getExistDates(
            int year, int month, Long userId
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        log.info("{}년 {}월에 타임캡슐이 존재하는 날짜 목록을 조회합니다.", year, month);
        List<LocalDate> timeCapsuleDates =
                timeCapsuleRepository.findActiveDatesInRange(userId, start, end).stream()
                        .map(Date::toLocalDate)
                        .toList();

        log.info("{}년 {}월에 일일리포트가 존재하는 날짜 목록을 조회합니다.", year, month);
        List<LocalDate> reportDates =
                reportRepository.findActiveDatesInRange(userId, start.toLocalDate(), end.toLocalDate());

        log.info("{}년 {}월에 존재하는 타임캡슐과 일일리포트의 날짜 목록을 종합합니다.", year, month);
        Set<LocalDate> mergedDates = new HashSet<>();
        mergedDates.addAll(timeCapsuleDates);
        mergedDates.addAll(reportDates);

        List<LocalDate> dates = mergedDates.stream()
                .sorted()
                .toList();
        return new CalendarFilledDatesResponse(dates.size(), dates);
    }
}
