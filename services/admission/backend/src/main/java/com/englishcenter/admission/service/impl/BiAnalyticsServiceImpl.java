package com.englishcenter.admission.service.impl;
import com.englishcenter.admission.dto.bi.BiWindow;
import com.englishcenter.admission.repository.AdmissionBiRepository;
import com.englishcenter.admission.service.BiAnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.math.*;
/** Mỗi báo cáo đọc cùng snapshot PostgreSQL, tách rõ attempt/user/enrollment và tiền Finance. */
@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class BiAnalyticsServiceImpl implements BiAnalyticsService {
 private final AdmissionBiRepository repo;
 public BiAnalyticsServiceImpl(AdmissionBiRepository repo){this.repo=repo;}
 /** Mẫu số 0 trả null = N/A, không tạo 0%/100% giả. */
 public static Double rate(long n,long d){return d==0?null:BigDecimal.valueOf(n*100.0/d).setScale(2,RoundingMode.HALF_UP).doubleValue();}
 private static long n(Map<String,Object> m,String k){return ((Number)m.get(k)).longValue();}
 private static Map<String,Object> map(Object... kv){var m=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)m.put((String)kv[i],kv[i+1]);return m;}
 /** KPI FUN-01/02 khác đơn vị và mẫu số với các tầng funnel user, metadata công khai điều đó. */
 public Map<String,Object> funnel(BiWindow w){var c=repo.funnel(w);long total=n(c,"total_enrollments");
 var k=map("test_completion_rate",rate(n(c,"submitted_attempts"),n(c,"started_attempts")),"test_to_enrollment_rate",rate(n(c,"converted_users"),n(c,"submitted_users")),"paid_conversion_rate",rate(n(c,"approved_enrollments"),total),"expired_hold_rate",rate(n(c,"expired_enrollments"),total));
 var stages=new ArrayList<Map<String,Object>>();String[] keys={"started_users","submitted_users","converted_users","approved_test_users"};String[] names={"Bắt đầu test (user)","Hoàn thành test (user)","Đăng ký trong 14 ngày (user)","Đơn còn APPROVED (user)"};
 for(int i=0;i<4;i++)stages.add(map("stage_id",i+1,"name",names[i],"count",c.get(keys[i]),"conversion_rate",i==0?rate(n(c,keys[0]),n(c,keys[0])):rate(n(c,keys[i]),n(c,keys[i-1]))));
 return map("metadata",w.metadata(),"counts",c,"stages",stages,"kpis",k,"summary",map("bottleneck_warning",total>0&&n(c,"expired_enrollments")*100.0/total>25?"Quá hạn trên mục tiêu đề xuất 25%":null,"benchmark_status","PROPOSED_NOT_INDUSTRY_STANDARD"));}
 /** Xếp hạng volume, occupancy có trọng số, không lấy tuition làm khoản thu hoặc dư nợ. */
 public Map<String,Object> courses(BiWindow w){var rows=repo.courses(w);long capacity=0,approved=0;var groups=new TreeMap<String,long[]>();
 for(var r:rows){capacity+=n(r,"capacity");approved+=n(r,"approved_slots");r.put("occupancy_rate",rate(n(r,"approved_slots"),n(r,"capacity")));r.put("pipeline_revenue",null);r.put("realized_revenue",null);var g=groups.computeIfAbsent(String.valueOf(r.get("level")),x->new long[2]);g[0]+=n(r,"approved_slots");g[1]+=n(r,"capacity");}
 var categories=new ArrayList<Map<String,Object>>();groups.forEach((key,g)->categories.add(map("level",key,"approved_slots",g[0],"capacity",g[1],"occupancy_rate",rate(g[0],g[1]))));
 return map("metadata",w.metadata(),"pipeline_potential_revenue",null,"realized_admission_revenue",null,"finance_status","UNAVAILABLE","finance_reason","Finance chưa cung cấp hợp đồng dữ liệu dư nợ/thu tiền/hoàn phí theo tuyển sinh","average_occupancy_rate",rate(approved,capacity),"approved_slots",approved,"capacity",capacity,"occupancy_by_level",categories,"top_courses",rows.stream().limit(w.limit()).toList());}
 /** Chỉ số đăng nhập, không gắn nhãn mọi hoạt động là active; controller giới hạn ADMIN. */
 public Map<String,Object> security(BiWindow w){var stats=repo.security(w);stats.put("failed_login_rate",rate(n(stats,"failed_logins_count"),n(stats,"login_attempts")));return map("metadata",w.metadata(),"engagement",stats,"daily_by_role",repo.activity(w,false),"monthly_by_role",repo.activity(w,true),"suspicious_ips",repo.suspiciousIps(w),"consecutive_failed_accounts",repo.consecutiveFailures(w));}
}
