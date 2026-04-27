-- 1) nullable 로 컬럼 추가
ALTER TABLE reports
    ADD COLUMN user_id BIGINT NULL;

-- 2) 기존 report.user_id 채우기
-- Report는 time_capsules.report_id로 연결되어 있고 time_capsules.user_id로 유저 확인 가능
UPDATE reports r
SET user_id = x.user_id
FROM (
    SELECT tc.report_id, MIN(tc.user_id) AS user_id
    FROM time_capsules tc
    WHERE tc.report_id IS NOT NULL
    GROUP BY tc.report_id
    ) x
WHERE x.report_id = r.report_id;
