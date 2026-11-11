package com.englishcenter.admission.dto.bi;
import com.englishcenter.admission.common.exception.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
/** Kỳ cohort theo ngày lịch Việt Nam; outcome chỉ là snapshot hiện tại, không giả dựng lịch sử. */
public record BiWindow(LocalDate from,LocalDate to,Integer courseId,String channel,String role,int limit,OffsetDateTime asOf){
 public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
 /** Kiểm tra bộ lọc và giới hạn một năm; as_of do server cấp, không nhận từ client. */
 public static BiWindow of(LocalDate from,LocalDate to,Integer course,String channel,String role,Integer limit){
  var now=OffsetDateTime.now(ZONE);var end=to==null?now.toLocalDate():to;var start=from==null?end.minusDays(29):from;
  if(start.isAfter(end)||ChronoUnit.DAYS.between(start,end)>365||end.isAfter(now.toLocalDate())||course!=null&&course<=0||limit!=null&&(limit<1||limit>100)
    ||channel!=null&&!Set.of("ONLINE","AT_COUNTER").contains(channel)||role!=null&&!Set.of("ADMIN","STAFF","STUDENT","TEACHER").contains(role))throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,"Bộ lọc BI không hợp lệ; kỳ tối đa 366 ngày, không lấy ngày tương lai");
  return new BiWindow(start,end,course,channel,role,limit==null?20:limit,now);
 }
 /** Metadata tái lập kỳ lọc và hạn chế lịch sử/khóa học của cohort test. */
 public Map<String,Object> metadata(){var m=new LinkedHashMap<String,Object>();m.put("from_date",from);m.put("to_date",to);m.put("as_of",asOf);m.put("timezone",ZONE.getId());m.put("snapshot","CURRENT_STATE_ONLY");m.put("course_id",courseId);m.put("channel",channel);m.put("role",role);m.put("test_cohort_scope","GLOBAL_TESTS_NO_COURSE_LINK");m.put("conversion_window_days",14);m.put("policy_status","BA_V3_6_PROPOSED_14_DAYS");return m;}
}
