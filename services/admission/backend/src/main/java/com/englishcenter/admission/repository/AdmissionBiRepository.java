package com.englishcenter.admission.repository;
import com.englishcenter.admission.dto.bi.BiWindow;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
/** Truy vấn thống kê chỉ schema Admission; thời gian luôn bind dạng instant có offset. */
@Repository
public class AdmissionBiRepository {
 private final NamedParameterJdbcTemplate jdbc;
 public AdmissionBiRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
 /** Tham số dùng chung, [from, to+1) theo Asia/Ho_Chi_Minh, không phụ thuộc timezone phiên PostgreSQL. */
 private Map<String,Object> params(BiWindow w){var p=new HashMap<String,Object>();p.put("start",w.from().atStartOfDay(BiWindow.ZONE).toOffsetDateTime());p.put("end",w.to().plusDays(1).atStartOfDay(BiWindow.ZONE).toOffsetDateTime());p.put("asof",w.asOf());p.put("course",w.courseId());p.put("channel",w.channel());p.put("role",w.role());p.put("limit",w.limit());p.put("month",w.to().withDayOfMonth(1).atStartOfDay(BiWindow.ZONE).toOffsetDateTime());p.put("day",w.to().atStartOfDay(BiWindow.ZONE).toOffsetDateTime());return p;}
 private static final String FILTER=" (:course::integer IS NULL OR c.course_id=:course) AND (:channel::text IS NULL OR e.enrollment_channel=:channel) ";
 private static final String COHORT=" e.enrolled_at>=:start AND e.enrolled_at<:end AND e.enrolled_at<=:asof ";
 /** Cùng tập STARTED; completion theo attempt, conversion theo user duy nhất, enrollment cohort riêng. */
 public Map<String,Object> funnel(BiWindow w){return jdbc.queryForMap("""
 WITH attempts AS (SELECT * FROM admission.test_attempts WHERE started_at>=:start AND started_at<:end AND started_at<=:asof),
 completed AS (SELECT * FROM attempts WHERE status='SUBMITTED' AND submitted_at>=started_at AND submitted_at<=:asof),
 converted AS (SELECT DISTINCT a.user_id FROM completed a WHERE EXISTS (SELECT 1 FROM admission.enrollments e JOIN admission.classes c ON c.id=e.class_id WHERE e.student_id=a.user_id AND e.enrolled_at>=a.submitted_at AND e.enrolled_at<=a.submitted_at+INTERVAL '14 days' AND e.enrolled_at<=:asof AND
 """+FILTER+"""
 )),approved_test AS (SELECT DISTINCT a.user_id FROM completed a WHERE EXISTS (SELECT 1 FROM admission.enrollments e JOIN admission.classes c ON c.id=e.class_id WHERE e.student_id=a.user_id AND e.enrolled_at>=a.submitted_at AND e.enrolled_at<=a.submitted_at+INTERVAL '14 days' AND e.enrolled_at<=:asof AND e.status='APPROVED' AND (e.approved_at IS NULL OR e.approved_at<=:asof) AND
 """+FILTER+"""
 )),enrs AS (SELECT e.*,c.status class_status FROM admission.enrollments e JOIN admission.classes c ON c.id=e.class_id WHERE
 """+COHORT+" AND "+FILTER+"""
 ) SELECT (SELECT COUNT(*) FROM attempts) started_attempts,(SELECT COUNT(*) FROM completed) submitted_attempts,
 (SELECT COUNT(DISTINCT user_id) FROM attempts) started_users,(SELECT COUNT(DISTINCT user_id) FROM completed) submitted_users,
 (SELECT COUNT(*) FROM converted) converted_users,(SELECT COUNT(*) FROM approved_test) approved_test_users,
 COUNT(*) total_enrollments,COUNT(*) FILTER(WHERE status='APPROVED' AND (approved_at IS NULL OR approved_at<=:asof)) approved_enrollments,
 COUNT(*) FILTER(WHERE status='EXPIRED' OR status='PENDING' AND hold_expires_at<=:asof) expired_enrollments,
 COUNT(*) FILTER(WHERE status='PENDING' AND class_status='OPEN' AND hold_expires_at>:asof) active_pending,
 COUNT(*) FILTER(WHERE status IN ('CANCELLED','CANCELLED_REFUND_PENDING')) cancelled_enrollments,
 COUNT(*) FILTER(WHERE NOT EXISTS(SELECT 1 FROM admission.test_attempts a WHERE a.user_id=enrs.student_id AND a.started_at<=enrs.enrolled_at)) no_test_enrollments,
 COUNT(*) FILTER(WHERE enrollment_channel='ONLINE') online_enrollments,COUNT(*) FILTER(WHERE enrollment_channel='AT_COUNTER') counter_enrollments
 FROM enrs
 """,params(w));}
 /** Classement theo tổng đơn tạo trong kỳ, gồm trạng thái kết thúc; occupancy là snapshot mọi đơn APPROVED ở lớp hoạt động. */
 public List<Map<String,Object>> courses(BiWindow w){return jdbc.queryForList("""
 WITH enrs AS (SELECT e.*,c.course_id,c.status class_status FROM admission.enrollments e JOIN admission.classes c ON c.id=e.class_id WHERE
 """+COHORT+" AND "+FILTER+"""
 ),vol AS (SELECT course_id,COUNT(*) enrolled_count,COUNT(*) FILTER(WHERE status='APPROVED' AND (approved_at IS NULL OR approved_at<=:asof)) approved_enrollments,
 COUNT(*) FILTER(WHERE status='PENDING' AND class_status='OPEN' AND hold_expires_at>:asof) pending_enrollments,
 COUNT(*) FILTER(WHERE status IN ('CANCELLED','CANCELLED_REFUND_PENDING')) cancelled_enrollments FROM enrs GROUP BY course_id),
 occ AS (SELECT c.course_id,SUM(c.max_students) capacity,SUM((SELECT COUNT(*) FROM admission.enrollments e WHERE e.class_id=c.id AND e.status='APPROVED' AND (e.approved_at IS NULL OR e.approved_at<=:asof))) approved_slots FROM admission.classes c WHERE c.status IN ('OPEN','IN_PROGRESS') GROUP BY c.course_id)
 SELECT co.id course_id,co.code course_code,co.name title,co.level,COALESCE(v.enrolled_count,0) enrolled_count,COALESCE(v.approved_enrollments,0) approved_enrollments,
 COALESCE(v.pending_enrollments,0) pending_enrollments,COALESCE(v.cancelled_enrollments,0) cancelled_enrollments,COALESCE(o.capacity,0) capacity,COALESCE(o.approved_slots,0) approved_slots
 FROM admission.courses co LEFT JOIN vol v ON co.id=v.course_id LEFT JOIN occ o ON co.id=o.course_id
 WHERE (:course::integer IS NULL OR co.id=:course) ORDER BY enrolled_count DESC,co.code,co.id
 """,params(w));}
 private static final String ROLE=" (:role::text IS NULL OR u.role=:role) ";
 /** DAU ngày lịch và MAU tháng lịch đến hết ngày chọn, không đếm refresh/password event. */
 public Map<String,Object> security(BiWindow w){return jdbc.queryForMap("""
 SELECT COUNT(DISTINCT a.user_id) FILTER(WHERE a.action='LOGIN_SUCCESS' AND a.created_at>=:day AND a.created_at<:end) dau,
 COUNT(DISTINCT a.user_id) FILTER(WHERE a.action='LOGIN_SUCCESS' AND a.created_at>=:month AND a.created_at<:end) mau,
 COUNT(*) FILTER(WHERE a.action='LOGIN_FAILED' AND a.created_at>=:start AND a.created_at<:end) failed_logins_count,
 COUNT(*) FILTER(WHERE a.action IN ('LOGIN_SUCCESS','LOGIN_FAILED') AND a.created_at>=:start AND a.created_at<:end) login_attempts
 FROM admission.auth_audit_logs a LEFT JOIN admission.users u ON u.id=a.user_id WHERE a.created_at<=:asof AND
 """+ROLE,params(w));}
 /** Xu hướng ngày/tháng và vai trò: đếm distinct user trong từng ô, không cộng DAU để suy MAU. */
 public List<Map<String,Object>> activity(BiWindow w,boolean monthly){String bucket=monthly?"month":"day";
 return jdbc.queryForList("SELECT date_trunc('"+bucket+"',a.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date period,u.role,COUNT(DISTINCT a.user_id) logged_in_users FROM admission.auth_audit_logs a JOIN admission.users u ON u.id=a.user_id WHERE a.action='LOGIN_SUCCESS' AND a.created_at>=:start AND a.created_at<:end AND a.created_at<=:asof AND "+ROLE+" GROUP BY period,u.role ORDER BY period,u.role",params(w));}
 /** Chỉ trả aggregate IP cho endpoint ADMIN; không trả email/token/nội dung audit. */
 public List<Map<String,Object>> suspiciousIps(BiWindow w){return jdbc.queryForList("SELECT a.ip_address,COUNT(*) failed_count,MAX(a.created_at) last_attempt FROM admission.auth_audit_logs a LEFT JOIN admission.users u ON u.id=a.user_id WHERE a.action='LOGIN_FAILED' AND a.ip_address IS NOT NULL AND a.created_at>=:start AND a.created_at<:end AND a.created_at<=:asof AND "+ROLE+" GROUP BY a.ip_address ORDER BY failed_count DESC,a.ip_address LIMIT 5",params(w));}
 /** Tín hiệu BA: hơn 5 LOGIN_FAILED liên tiếp trong 10 phút, reset bởi LOGIN_SUCCESS; chỉ aggregate ADMIN. */
 public List<Map<String,Object>> consecutiveFailures(BiWindow w){return jdbc.queryForList("SELECT a.user_id,COUNT(*) failed_count,MAX(a.created_at) last_attempt FROM admission.auth_audit_logs a JOIN admission.users u ON u.id=a.user_id WHERE a.action='LOGIN_FAILED' AND a.created_at>:asof-INTERVAL '10 minutes' AND a.created_at<=:asof AND "+ROLE+" AND NOT EXISTS(SELECT 1 FROM admission.auth_audit_logs success WHERE success.user_id=a.user_id AND success.action='LOGIN_SUCCESS' AND (success.created_at,success.id)>(a.created_at,a.id) AND success.created_at<=:asof) GROUP BY a.user_id HAVING COUNT(*)>5 ORDER BY failed_count DESC,a.user_id",params(w));}
}
