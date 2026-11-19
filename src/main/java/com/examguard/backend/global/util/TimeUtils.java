package com.examguard.backend.global.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * DB는 Instant(시간대 없는 한 순간)로 저장하고, API는 명세대로 한국 시각(+09:00)으로 주고받는다.
 * 예: 2026-12-10T10:00:00+09:00
 */
public final class TimeUtils {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeUtils() {
    }

    // 밀리초(소수점 3자리)까지만 내보냄. DB는 마이크로초(6자리)까지 저장하는데,
    // 일부 브라우저(사파리 등)는 소수점 3자리를 넘는 시각 문자열을 못 읽을 수 있음
    public static OffsetDateTime toKst(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MILLIS).atZone(KST).toOffsetDateTime();
    }

    public static Instant toInstant(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }
}
