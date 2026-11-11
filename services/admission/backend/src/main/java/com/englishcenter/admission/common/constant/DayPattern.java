package com.englishcenter.admission.common.constant;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Định nghĩa mẫu ngày học tiêu chuẩn trong tuần (admission.classes.day_pattern).
 * Phục vụ cho nghiệp vụ lập lịch và thuật toán phát hiện xung đột thời khóa biểu.
 */
public enum DayPattern {

    /** Thứ 2, Thứ 4, Thứ 6 */
    MON_WED_FRI("Thứ 2 - 4 - 6", EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),

    /** Thứ 3, Thứ 5, Thứ 7 */
    TUE_THU_SAT("Thứ 3 - 5 - 7", EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)),

    /** Thứ 7, Chủ Nhật */
    SAT_SUN("Thứ 7 - Chủ Nhật", EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),

    /** Hằng ngày (Thứ 2 đến Chủ Nhật) */
    DAILY("Hằng ngày", EnumSet.allOf(DayOfWeek.class)),

    /** Lịch tùy chỉnh (Custom) */
    CUSTOM("Tùy chỉnh", Collections.emptySet());

    private final String description;
    private final Set<DayOfWeek> daysOfWeek;

    DayPattern(String description, Set<DayOfWeek> daysOfWeek) {
        this.description = description;
        this.daysOfWeek = daysOfWeek;
    }

    public String getDescription() {
        return description;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    /**
     * Kiểm tra xem một ngày trong tuần (DayOfWeek) có thuộc mẫu ngày học này không.
     *
     * @param day Ngày trong tuần cần kiểm tra
     * @return true nếu ngày học thuộc mẫu này
     */
    public boolean contains(DayOfWeek day) {
        if (this == DAILY) {
            return true;
        }
        return daysOfWeek.contains(day);
    }

    /**
     * Kiểm tra xem 2 mẫu ngày có bất kỳ ngày nào trùng nhau không.
     *
     * @param other Mẫu ngày khác
     * @return true nếu có ít nhất 1 ngày trùng lặp
     */
    public boolean overlapsWith(DayPattern other) {
        if (this == DAILY || other == DAILY) {
            return true;
        }
        for (DayOfWeek day : this.daysOfWeek) {
            if (other.contains(day)) {
                return true;
            }
        }
        return false;
    }
}
