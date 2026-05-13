package com.example.emotion_storage.report.service;

import com.example.emotion_storage.global.exception.BaseException;
import com.example.emotion_storage.global.exception.ErrorCode;
import com.example.emotion_storage.notification.service.NotificationService;
import com.example.emotion_storage.report.domain.EmotionVariation;
import com.example.emotion_storage.report.domain.Keyword;
import com.example.emotion_storage.report.domain.Report;
import com.example.emotion_storage.report.dto.request.DailyReportGenerateRequest;
import com.example.emotion_storage.report.dto.response.DailyReportGenerateResponse;
import com.example.emotion_storage.report.repository.ReportRepository;
import com.example.emotion_storage.timecapsule.domain.AnalyzedEmotion;
import com.example.emotion_storage.timecapsule.domain.AnalyzedFeedback;
import com.example.emotion_storage.timecapsule.domain.TimeCapsule;
import com.example.emotion_storage.timecapsule.repository.TimeCapsuleRepository;
import com.example.emotion_storage.user.domain.User;
import com.example.emotion_storage.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReportGenerateService {

    private final UserRepository userRepository;
    private final TimeCapsuleRepository timeCapsuleRepository;
    private final ReportRepository reportRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    private static final String REFERENCE_MESSAGE_PREFIX = "다음은 오늘 하루 동안 생성된 복수의 타임캡슐이야.\n\n";

    /**
     * 매일 KST 기준 00시 01분에 전날 타임캡슐을 종합하여 일일 리포트 생성
     */
   @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Seoul")
    @Transactional
    public void generateDailyReportsForAllUsers() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate yesterday = now.toLocalDate().minusDays(1);
        
        log.info("일일 리포트 생성 스케줄러 시작 - 대상 날짜: {}", yesterday);
        
        try {
            List<Long> activeUserIds = userRepository.findAllActiveUserIdsWithValidGender();
            log.info("리포트 생성 대상 사용자 수(유효 성별): {}", activeUserIds.size());
            
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            
            for (Long userId : activeUserIds) {
                try {
                    boolean generated = generateDailyReportForUser(userId, yesterday);
                    if (generated) {
                        successCount++;
                    } else {
                        skipCount++;
                    }
                } catch (Exception e) {
                    log.error("사용자 ID {}의 일일 리포트 생성 중 오류 발생", userId, e);
                    errorCount++;
                }
            }
            
            log.info("일일 리포트 생성 완료 - 성공: {}, 스킵: {}, 실패: {}", successCount, skipCount, errorCount);
            
        } catch (Exception e) {
            log.error("일일 리포트 생성 스케줄러 실행 중 오류 발생", e);
            throw new BaseException(ErrorCode.DAILY_REPORT_GENERATION_FAILED);
        }
    }

    /**
     * 특정 사용자에 대해 전날 타임캡슐을 종합하여 일일 리포트 생성
     */
    @Transactional
    public boolean generateDailyReportForUser(Long userId, LocalDate targetDate) {
        log.info("일일 리포트 생성 시작 - userId: {}, date: {}", userId, targetDate);
        
        // 이미 리포트가 존재하는지 확인
        boolean reportExists = reportRepository.findByUserIdAndHistoryDate(userId, targetDate).isPresent();
        if (reportExists) {
            log.info("이미 존재하는 리포트 - userId: {}, date: {}", userId, targetDate);
            return false;
        }
        
        // 전날 타임캡슐 조회
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime startOfNextDay = targetDate.plusDays(1).atStartOfDay();
        List<TimeCapsule> timeCapsules = timeCapsuleRepository.findByUserIdAndHistoryDate(
                userId,
                startOfDay,
                startOfNextDay
        );
        
        if (timeCapsules.isEmpty()) {
            log.info("타임캡슐이 없어 리포트 생성 스킵 - userId: {}, date: {}", userId, targetDate);
            return false;
        }
        
        log.info("타임캡슐 발견 - userId: {}, date: {}, count: {}", userId, targetDate, timeCapsules.size());
        
        // 타임캡슐 정보를 문자열로 변환
        String referenceMessage = buildReferenceMessage(timeCapsules);
        
        // AI 서버로 요청 전송
        DailyReportGenerateResponse aiResponse = callAiServer(referenceMessage);
        
        // AI 응답을 Report 엔티티로 변환하여 저장
        User reportOwner = userRepository.getReferenceById(userId);
        Report report = convertToReport(reportOwner, targetDate, timeCapsules, aiResponse);
        reportRepository.save(report);

        // 알림 저장
        notificationService.createDailyReportArrival(userId, report.getId());
        
        log.info("일일 리포트 생성 완료 - userId: {}, date: {}, reportId: {}", 
                userId, targetDate, report.getId());
        
        return true;
    }

    /**
     * 타임캡슐 리스트를 reference_message 형식의 문자열로 변환
     */
    private String buildReferenceMessage(List<TimeCapsule> timeCapsules) {
        StringBuilder sb = new StringBuilder(REFERENCE_MESSAGE_PREFIX);
        
        for (int i = 0; i < timeCapsules.size(); i++) {
            TimeCapsule tc = timeCapsules.get(i);
            sb.append("---\n");
            sb.append("타임캡슐 ").append(i + 1).append(":\n");
            sb.append("제목: ").append(tc.getOneLineSummary()).append("\n");
            sb.append("한 줄 요약: ").append(tc.getOneLineSummary()).append("\n");
            sb.append("상세 요약: ").append(tc.getDialogueSummary()).append("\n");
            
            // 감정 키워드 및 비율
            String emotionKeywords = formatEmotionKeywords(tc.getAnalyzedEmotions());
            sb.append("감정 키워드: ").append(emotionKeywords).append("\n");
            
            // 피드백
            String feedbacks = formatFeedbacks(tc.getAnalyzedFeedbacks());
            sb.append("피드백: ").append(feedbacks).append("\n");
            sb.append("---\n\n");
        }
        
        return sb.toString().trim();
    }

    /**
     * AnalyzedEmotion 리스트를 "감정명 비율%" 형식으로 변환
     */
    private String formatEmotionKeywords(List<AnalyzedEmotion> emotions) {
        if (emotions == null || emotions.isEmpty()) {
            return "";
        }
        
        return emotions.stream()
                .map(emotion -> emotion.getAnalyzedEmotion() + " " + emotion.getPercentage() + "%")
                .collect(Collectors.joining(", "));
    }

    /**
     * AnalyzedFeedback 리스트를 하나의 문자열로 변환
     */
    private String formatFeedbacks(List<AnalyzedFeedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return "";
        }
        
        return feedbacks.stream()
                .map(AnalyzedFeedback::getAnalyzedFeedback)
                .collect(Collectors.joining(" "));
    }

    /**
     * AI 서버로 일일 리포트 생성 요청 전송
     */
    private DailyReportGenerateResponse callAiServer(String referenceMessage) {
        try {
            DailyReportGenerateRequest request = DailyReportGenerateRequest.builder()
                    .roleMessage("")
                    .referenceMessage(referenceMessage)
                    .analyzeMessage("")
                    .build();

            String url = aiServerBaseUrl + "/daily-report/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<DailyReportGenerateRequest> requestEntity = new HttpEntity<>(request, headers);
            
            log.info("AI 서버에 일일 리포트 생성 요청 전송 - URL: {}", url);
            log.debug("요청 데이터: {}", objectMapper.writeValueAsString(request));
            
            ResponseEntity<DailyReportGenerateResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    DailyReportGenerateResponse.class
            );
            
            DailyReportGenerateResponse responseBody = response.getBody();
            log.info("AI 서버로부터 일일 리포트 생성 응답 수신");
            log.debug("응답 데이터: {}", objectMapper.writeValueAsString(responseBody));
            
            if (responseBody == null) {
                log.error("AI 서버 응답이 null입니다.");
                throw new BaseException(ErrorCode.AI_SERVER_RESPONSE_NULL);
            }
            
            return responseBody;
            
        } catch (HttpClientErrorException e) {
            log.error("AI 서버 클라이언트 오류 (4xx): {}", e.getResponseBodyAsString(), e);
            throw new BaseException(ErrorCode.AI_SERVER_REQUEST_FAILED);
            
        } catch (HttpServerErrorException e) {
            log.error("AI 서버 서버 오류 (5xx): {}", e.getResponseBodyAsString(), e);
            throw new BaseException(ErrorCode.AI_SERVER_REQUEST_FAILED);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("일일 리포트 생성 요청 중 예상치 못한 오류 발생", e);
            throw new BaseException(ErrorCode.DAILY_REPORT_GENERATION_FAILED);
        }
    }

    /**
     * AI 서버 응답을 Report 엔티티로 변환
     */
    private Report convertToReport(User user, LocalDate historyDate, 
                                   List<TimeCapsule> timeCapsules,
                                   DailyReportGenerateResponse aiResponse) {
        // summaries를 하나의 문자열로 합치기
        String todaySummary = String.join("\n", aiResponse.getSummaries());
        
        // Report 생성
        Report report = Report.builder()
                .user(user)
                .historyDate(historyDate)
                .todaySummary(todaySummary)
                .stressIndex(aiResponse.getStressLevel())
                .happinessIndex(aiResponse.getHappinessLevel())
                .emotionSummary(aiResponse.getSentimentReview())
                .isOpened(false)
                .build();
        
        // Keywords 추가
        if (aiResponse.getKeywords() != null) {
            for (String keywordStr : aiResponse.getKeywords()) {
                Keyword keyword = Keyword.builder()
                        .keyword(keywordStr)
                        .build();
                report.addKeyword(keyword);
            }
        }
        
        // EmotionVariations 추가 (sentiment_changes 파싱)
        if (aiResponse.getSentimentChanges() != null) {
            for (String sentimentChange : aiResponse.getSentimentChanges()) {
                EmotionVariation emotionVariation = parseSentimentChange(
                        sentimentChange, historyDate, report
                );
                if (emotionVariation != null) {
                    report.addEmotionVariation(emotionVariation);
                }
            }
        }
        
        // TimeCapsules 연결
        for (TimeCapsule timeCapsule : timeCapsules) {
            report.addTimeCapsule(timeCapsule);
        }
        
        return report;
    }

    /**
     * sentiment_changes 형식 ("HH:mm 감정")을 EmotionVariation으로 파싱
     */
    private EmotionVariation parseSentimentChange(String sentimentChange, 
                                                   LocalDate baseDate,
                                                   Report report) {
        try {
            // "08:00 😡짜증" 형식을 파싱
            String[] parts = sentimentChange.trim().split("\\s+", 2);
            if (parts.length != 2) {
                log.warn("잘못된 sentiment_changes 형식: {}", sentimentChange);
                return null;
            }
            
            String timeStr = parts[0]; // "08:00"
            String label = parts[1];   // "😡짜증"
            
            // 시간 파싱 (HH:mm 형식)
            String[] timeParts = timeStr.split(":");
            if (timeParts.length != 2) {
                log.warn("잘못된 시간 형식: {}", timeStr);
                return null;
            }
            
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            LocalDateTime time = baseDate.atTime(hour, minute);
            
            return EmotionVariation.builder()
                    .time(time)
                    .label(label)
                    .build();
                    
        } catch (Exception e) {
            log.warn("sentiment_changes 파싱 실패: {}", sentimentChange, e);
            return null;
        }
    }
}
