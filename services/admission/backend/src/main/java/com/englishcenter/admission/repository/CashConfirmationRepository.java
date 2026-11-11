package com.englishcenter.admission.repository;

import com.englishcenter.admission.dto.enrollment.CashConfirmationResponse;
import com.englishcenter.admission.dto.enrollment.ManualApproveRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.Optional;

/** Chỉ truy cập schema Admission; không ghi payments/invoices của Finance. */
@Repository
public class CashConfirmationRepository {
    private final JdbcTemplate jdbc;

    /** Nhận kết nối DB tham gia cùng transaction với JPA Admission. */
    public CashConfirmationRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    /** Tra yêu cầu theo đơn và mã hóa đơn hiện tại; trả rỗng nếu chưa có. */
    public Optional<CashConfirmationResponse> find(Integer enrollmentId,String invoiceCode) {
        return jdbc.query("SELECT * FROM admission.cash_confirmation_requests WHERE enrollment_id=? AND invoice_code=?",
                (rs,n)->new CashConfirmationResponse(rs.getLong("id"),rs.getString("status"),rs.getString("invoice_code"),
                        rs.getString("receipt_number"),rs.getBigDecimal("amount"),rs.getInt("requested_by"),
                        rs.getObject("requested_at",OffsetDateTime.class)),enrollmentId,invoiceCode).stream().findFirst();
    }

    /** UNIQUE chống trùng chứng từ giữa các lớp; không làm transaction hỏng khi gặp conflict. */
    public boolean insert(Integer enrollmentId,String invoiceCode,Integer actor,ManualApproveRequest request) {
        return jdbc.update("INSERT INTO admission.cash_confirmation_requests(enrollment_id,invoice_code,receipt_number,amount,requested_by,notes) VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                enrollmentId,invoiceCode,request.getReceiptNumber().trim(),request.getAmountReceived(),actor,request.getNotes())==1;
    }

    /** Ghi audit tối thiểu, không ghi JWT/secret/nội dung note; hỗ trợ cả dữ liệu bị từ chối. */
    public void audit(Integer enrollmentId,Integer actor,ManualApproveRequest request,String outcome,String reason) {
        jdbc.update("INSERT INTO admission.cash_confirmation_audit(enrollment_id,actor_id,outcome,reason_code,receipt_number,amount_text) VALUES (?,?,?,?,?,?)",
                enrollmentId,actor,outcome,reason,limit(request==null?null:request.getReceiptNumber()),
                limit(request==null||request.getAmountReceived()==null?null:request.getAmountReceived().toString()));
    }

    /** Cắt dữ liệu đầu vào audit quá dài để lần từ chối validation vẫn ghi được. */
    private String limit(String value) {return value==null?null:value.substring(0,Math.min(80,value.length()));}
}
