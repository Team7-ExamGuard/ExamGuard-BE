-- 로컬 개발용 예시 시험 4개 + 문항 (Flyway가 실행하지 않음, 클러스터에는 넣지 않음)
--
-- 실행 (DB 컨테이너가 떠 있을 때, 프로젝트 폴더에서):
--   docker exec -i examguard-postgres-1 psql -U examguard -d examguard < scripts/sample-exams.sql
--
-- 다시 실행하면 예시 시험(제목이 [예시]로 시작)을 지우고 지금 시각 기준으로 새로 만든다.
-- 대기 → 응시 자동 전환을 다시 보고 싶을 때도 다시 실행하면 된다.
--   OPEN01: 10분 전 시작, 50분 뒤 종료 (진행 중)
--   WAIT01: 3분 뒤 시작 (시작 전)
--   코드 없음: 작성 중 (DRAFT)
--   DONE01: 이틀 전 종료

BEGIN;

-- 이전 예시 시험 정리 (응시 기록 → 문항 → 시험 순서로 지움)
DELETE FROM attempts WHERE exam_id IN (SELECT id FROM exams WHERE title LIKE '[예시]%');
DELETE FROM questions WHERE exam_id IN (SELECT id FROM exams WHERE title LIKE '[예시]%');
DELETE FROM exams WHERE title LIKE '[예시]%';

-- 예시 시험 4개 (작성자: admin)
INSERT INTO exams (title, code, start_at, end_at, published_at, created_by, created_at, updated_at)
SELECT v.title, v.code, v.start_at, v.end_at, v.published_at, u.id, now(), now()
FROM users u
         CROSS JOIN (VALUES ('[예시] 중2 수학 1단원 평가', 'OPEN01', now() - interval '10 minutes', now() + interval '50 minutes', now() - interval '1 day'),
                            ('[예시] 중3 영어 월말 테스트', 'WAIT01', now() + interval '3 minutes', now() + interval '33 minutes', now()),
                            ('[예시] 중1 과학 단원평가', NULL, now() + interval '7 days', now() + interval '7 days 1 hour', NULL),
                            ('[예시] 중2 국어 주간 테스트', 'DONE01', now() - interval '2 days', now() - interval '2 days' + interval '30 minutes', now() - interval '3 days'))
    AS v(title, code, start_at, end_at, published_at)
WHERE u.login_id = 'admin';

-- 시험마다 문항 3개 (객관식 answer는 보기 인덱스 0~3)
INSERT INTO questions (exam_id, seq, type, content, options, answer)
SELECT e.id, q.seq, q.type, q.content, q.options::jsonb, q.answer
FROM (VALUES ('[예시] 중2 수학 1단원 평가', 1, 'MULTIPLE_CHOICE', '다음 중 유리수가 아닌 것은?', '["0.5", "-3", "√2", "2/3"]', '2'),
             ('[예시] 중2 수학 1단원 평가', 2, 'SHORT_ANSWER', '0.333…을 기약분수로 나타내면?', NULL, '1/3'),
             ('[예시] 중2 수학 1단원 평가', 3, 'MULTIPLE_CHOICE', '(-2)³의 값은?', '["-8", "-6", "6", "8"]', '0'),

             ('[예시] 중3 영어 월말 테스트', 1, 'MULTIPLE_CHOICE', 'I ___ to the library yesterday. 빈칸에 알맞은 것은?', '["go", "goes", "went", "going"]', '2'),
             ('[예시] 중3 영어 월말 테스트', 2, 'SHORT_ANSWER', '"아름다운"을 뜻하는 영어 단어는?', NULL, 'beautiful'),
             ('[예시] 중3 영어 월말 테스트', 3, 'MULTIPLE_CHOICE', 'She is ___ than her sister. 빈칸에 알맞은 것은?', '["tall", "taller", "tallest", "more tall"]', '1'),

             ('[예시] 중1 과학 단원평가', 1, 'MULTIPLE_CHOICE', '물질의 세 가지 상태가 아닌 것은?', '["고체", "액체", "기체", "전기"]', '3'),
             ('[예시] 중1 과학 단원평가', 2, 'SHORT_ANSWER', '1기압에서 물이 끓는 온도는 몇 ℃인가?', NULL, '100'),
             ('[예시] 중1 과학 단원평가', 3, 'MULTIPLE_CHOICE', '식물이 광합성으로 내보내는 기체는?', '["산소", "질소", "수소", "이산화탄소"]', '0'),

             ('[예시] 중2 국어 주간 테스트', 1, 'MULTIPLE_CHOICE', '"가랑비에 옷 젖는 줄 모른다"와 뜻이 가까운 것은?', '["작은 일이 쌓여 큰일이 된다", "말 한마디로 천 냥 빚을 갚는다", "소 잃고 외양간 고친다", "우물 안 개구리"]', '0'),
             ('[예시] 중2 국어 주간 테스트', 2, 'SHORT_ANSWER', '소리는 같지만 뜻이 다른 낱말을 무엇이라 하는가?', NULL, '동음이의어'),
             ('[예시] 중2 국어 주간 테스트', 3, 'MULTIPLE_CHOICE', '다음 중 품사가 다른 하나는?', '["달리다", "먹다", "예쁘다", "읽다"]', '2'))
         AS q(title, seq, type, content, options, answer)
         JOIN exams e ON e.title = q.title;

COMMIT;
