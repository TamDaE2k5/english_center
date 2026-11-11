package com.englishcenter.admission.service;
import com.englishcenter.admission.client.StudentServiceClient;
import com.englishcenter.admission.repository.StudentSyncOutbox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import java.net.ConnectException;

/** Dispatcher có giới hạn; mặc định tắt tới khi endpoint và idempotency Student được kiểm chứng. */
@Service
public class StudentSyncRetryWorker {
 private final StudentSyncOutbox outbox;private final StudentServiceClient client;
 @Value("${admission.student-sync.enabled:false}") private boolean enabled;
 @Value("${admission.student-sync.idempotency-verified:false}") private boolean verified;
 public StudentSyncRetryWorker(StudentSyncOutbox outbox,StudentServiceClient client){this.outbox=outbox;this.client=client;}
 /** Mỗi tick một job, hỗ trợ nhiều instance bằng lease; không mở transaction quanh HTTP. */
 @Scheduled(fixedDelayString="${admission.student-sync.poll-ms:10000}")
 public void tick(){if(enabled)runOnce();}
 /** Điểm chạy có chủ đích cho vận hành/test; phải bật công tắc trước khi gửi. */
 public void runOnce(){runForUser(null);}
 /** Chạy user cụ thể để đối soát có kiểm soát; null lấy job bất kỳ đến hạn. */
 public void runForUser(Integer userId){
  if(!enabled)return;
  outbox.recover(verified);outbox.reconcile();
  var next=outbox.claim(userId);if(next.isEmpty())return;var j=next.get();
  try{
   var data=client.syncStudentProfile(j.userId(),j.fullName(),j.email(),j.phone(),j.birth());
   if(data==null||data.getStudentId()==null||data.getStudentId()<=0||data.getStudentCode()==null||data.getStudentCode().isBlank()){
    outbox.finish(j,"REVIEW",null,"INVALID_ACK");
   }else outbox.finish(j,"SUCCEEDED",data.getStudentId(),null);
  }catch(Exception ex){
   boolean retry=false;
   if(ex instanceof RestClientResponseException http) retry=verified&&(http.getStatusCode().value()==429||http.getStatusCode().is5xxServerError());
   else if(ex instanceof ResourceAccessException){
    Throwable cause=ex;while(cause!=null){if(cause instanceof ConnectException)retry=true;cause=cause.getCause();}
    retry=retry||verified;
   }
   outbox.finish(j,retry?(j.attempts()>=5?"EXHAUSTED":"RETRY"):"REVIEW",null,ex instanceof RestClientResponseException h?"HTTP_"+h.getStatusCode().value():"UNCERTAIN_TRANSPORT");
  }
  outbox.reconcile();
 }
}
