package com.englishcenter.admission.service;
import com.englishcenter.admission.dto.bi.BiWindow;
import java.util.Map;
/** Báo cáo snapshot Admission, không tự nhận sở hữu doanh thu Finance. */
public interface BiAnalyticsService {
 Map<String,Object> funnel(BiWindow window);
 Map<String,Object> courses(BiWindow window);
 Map<String,Object> security(BiWindow window);
}
