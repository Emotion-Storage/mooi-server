package com.example.emotion_storage.user.service;

import com.example.emotion_storage.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketInitService {

    private final UserRepository userRepository;

    /**
     * 매일 KST 기준 자정(00:00)에 모든 활성 사용자의 ticketCount를 10으로 초기화
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    @Transactional
    public void initAllUserTickets() {
        log.info("티켓 초기화 스케줄러 시작 - 시간: {}", LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        
        try {
            int updatedCount = userRepository.resetTicketCountForAllActiveUsers(10L);
            log.info("티켓 초기화 완료 - 처리된 사용자 수: {}", updatedCount);
            
        } catch (Exception e) {
            log.error("티켓 초기화 중 오류 발생", e);
            throw new RuntimeException("티켓 초기화 작업 실패", e);
        }
    }
}
