package com.englishcenter.admission.common.constant;

import java.time.LocalTime;

/**
 * Định nghĩa 6 ca học tiêu chuẩn tại Trung tâm Anh ngữ.
 * Dùng làm mốc tham chiếu và gợi ý khung giờ chuẩn khi tạo lớp học.
 */
public enum StandardShift {

    CA_1("Ca 1", LocalTime.of(8, 0), LocalTime.of(9, 30)),
    CA_2("Ca 2", LocalTime.of(9, 45), LocalTime.of(11, 15)),
    CA_3("Ca 3", LocalTime.of(14, 0), LocalTime.of(15, 30)),
    CA_4("Ca 4", LocalTime.of(15, 45), LocalTime.of(17, 15)),
    CA_5("Ca 5", LocalTime.of(17, 45), LocalTime.of(19, 15)),
    CA_6("Ca 6", LocalTime.of(19, 30), LocalTime.of(21, 0));

    private final String label;
    private final LocalTime startTime;
    private final LocalTime endTime;

    StandardShift(String label, LocalTime startTime, LocalTime endTime) {
        this.label = label;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getLabel() {
        return label;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Kiểm tra xem 2 khung giờ có bị giao cắt (overlap) nhau không.
     *
     * @param start1 Giờ bắt đầu 1
     * @param end1 Giờ kết thúc 1
     * @param start2 Giờ bắt đầu 2
     * @param end2 Giờ kết thúc 2
     * @return true nếu có khoảng thời gian giao cắt
     */
    public static boolean isTimeOverlapping(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
