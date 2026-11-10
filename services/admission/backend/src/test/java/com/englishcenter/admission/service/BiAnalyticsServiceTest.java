package com.englishcenter.admission.service;
import com.englishcenter.admission.dto.bi.BiWindow;
import com.englishcenter.admission.service.impl.BiAnalyticsServiceImpl;
import com.englishcenter.admission.repository.AdmissionBiRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/** Guards và contract BI; công thức dữ liệu được test riêng trên PostgreSQL. */
class BiAnalyticsServiceTest {
 @Test void emptyDenominatorIsNA(){assertNull(BiAnalyticsServiceImpl.rate(0,0));assertEquals(66.67,BiAnalyticsServiceImpl.rate(2,3));}
 @Test void invalidDatesAreRejected(){assertThrows(RuntimeException.class,()->BiWindow.of(LocalDate.of(2020,2,2),LocalDate.of(2020,1,1),null,null,null,null));}
 @Test void invalidFiltersAreRejected(){assertThrows(RuntimeException.class,()->BiWindow.of(null,null,null,"OTHER",null,0));}
 @Test void revenueIsUnavailableWithoutFinance(){var repo=mock(AdmissionBiRepository.class);when(repo.courses(any())).thenReturn(new ArrayList<>());var d=new BiAnalyticsServiceImpl(repo).courses(BiWindow.of(null,null,null,null,null,null));assertNull(d.get("realized_admission_revenue"));assertNull(d.get("pipeline_potential_revenue"));assertNull(d.get("average_occupancy_rate"));assertEquals("UNAVAILABLE",d.get("finance_status"));}
}
