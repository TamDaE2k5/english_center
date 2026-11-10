package com.englishcenter.admission.controller;
import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.bi.BiWindow;
import com.englishcenter.admission.service.BiAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
/** API BI và alias BA, giữ báo cáo an ninh riêng ADMIN. Không nhận as_of lịch sử vì chưa có state history. */
@RestController
@RequestMapping({"/api/admission/bi","/api/admission/analytics"})
public class BiAnalyticsController {
 private final BiAnalyticsService service;
 public BiAnalyticsController(BiAnalyticsService service){this.service=service;}
 /** Bộ lọc dùng chung cho ba báo cáo; days hỗ trợ alias BA khi không có from_date. */
 @ModelAttribute("window") public BiWindow window(
 @RequestParam(name="from_date",required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
 @RequestParam(name="to_date",required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
 @RequestParam(name="course_id",required=false) Integer course,
 @RequestParam(name="channel",required=false) String channel,@RequestParam(name="role",required=false) String role,
 @RequestParam(name="as_of",required=false) String asOf, @RequestParam(name="limit",required=false) Integer limit,@RequestParam(name="days",required=false) Integer days){
 if(asOf!=null)throw new com.englishcenter.admission.common.exception.BusinessException(com.englishcenter.admission.common.exception.ErrorCode.ADM_VALIDATION_ERROR,"Chưa có state history để truy vấn as_of quá khứ");
 if(days!=null){if(days<1||days>366)throw new com.englishcenter.admission.common.exception.BusinessException(com.englishcenter.admission.common.exception.ErrorCode.ADM_VALIDATION_ERROR);if(from==null)from=(to==null?LocalDate.now(BiWindow.ZONE):to).minusDays(days-1);}
 return BiWindow.of(from,to,course,channel,role,limit);}
 /** Phễu cohort cho STAFF/ADMIN; không trộn tiền thực thu vào APPROVED. */
 @GetMapping("/funnel") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public ApiResponse<Map<String,Object>> funnel(@ModelAttribute("window") BiWindow w){return ApiResponse.success(service.funnel(w),"Phễu cohort Admission");}
 /** Xếp hạng số đơn, occupancy và lý do dữ liệu Finance chưa khả dụng. */
 @GetMapping({"/courses-revenue","/courses-ranking"}) @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public ApiResponse<Map<String,Object>> courses(@ModelAttribute("window") BiWindow w){return ApiResponse.success(service.courses(w),"Xếp hạng và lấp đầy; tiền Finance chưa khả dụng");}
 /** Chỉ ADMIN được xem xu hướng đăng nhập và IP/tài khoản bất thường. */
 @GetMapping({"/security-engagement","/user-activity"}) @PreAuthorize("hasRole('ADMIN')")
 public ApiResponse<Map<String,Object>> security(@ModelAttribute("window") BiWindow w){return ApiResponse.success(service.security(w),"Thống kê đăng nhập ADMIN");}
 /** Lỗi parse query BI là 400, không lộ stacktrace hoặc trả 500 cho ngày sai định dạng. */
 @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
 public org.springframework.http.ResponseEntity<ApiResponse<Object>> badQuery(){return org.springframework.http.ResponseEntity.badRequest().body(ApiResponse.error(com.englishcenter.admission.common.exception.ErrorCode.ADM_VALIDATION_ERROR,"Định dạng bộ lọc BI không hợp lệ"));}
}
