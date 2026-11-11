async function callReportApi(path, options = {}) {
    if (window.api && typeof window.api.request === 'function') {
        return window.api.request(path, options);
    }
    const token = localStorage.getItem('access_token') || localStorage.getItem('token') || '';
    const headers = Object.assign(
        { 'Content-Type': 'application/json' },
        token ? { Authorization: `Bearer ${token}` } : {},
        options.headers || {}
    );
    const response = await fetch(path, Object.assign({}, options, { headers }));
    return response.json();
}

function renderSummaryRows(list) {
    const tbody = document.getElementById('reportSummaryBody');
    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Chưa có dữ liệu tổng kết điểm.</td></tr>`;
        return;
    }

    tbody.innerHTML = list
        .map(
        (s) => `
        <tr>
            <td>Lớp ${s.class_id}</td>
            <td>HV #${s.student_id}</td>
            <td>${s.total_assessments} bài</td>
            <td>${s.weighted_avg_score}</td>
            <td><strong>${s.average_percentage}%</strong></td>
            <td>
            <span class="badge ${s.result === 'Pass' ? 'badge-success' : 'badge-danger'}">
                ${s.result === 'Pass' ? 'Pass (Đạt ≥ 50%)' : 'Fail (Chưa đạt)'}
            </span>
            </td>
        </tr>
        `
        )
        .join('');
}

async function loadClassSummary() {
    const classId = document.getElementById('repClassId').value || '1';
    const tbody = document.getElementById('reportSummaryBody');

    const res = await callReportApi(`/api/learning/reports/class-summary?class_id=${classId}`);
    if (!res.success) {
        // Nếu người dùng đang đăng nhập là STUDENT thì tự động chuyển sang xem kết quả cá nhân
        if (res.error?.code === 'LRN_FORBIDDEN_CLASS') {
        await loadStudentSummary();
        return;
        }
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--color-danger); text-align:center;">[${res.error?.code || 'ERR'}] ${res.error?.message || 'Không thể tải báo cáo lớp'}</td></tr>`;
        return;
    }

    const report = res.data || {};
    document.getElementById('kpiTotalStudents').textContent = report.total_students ?? 0;
    document.getElementById('kpiPassCount').textContent = report.pass_count ?? 0;
    document.getElementById('kpiFailCount').textContent = report.fail_count ?? 0;
    document.getElementById('kpiPassRate').textContent = `${report.pass_rate_percentage ?? 0}%`;

    renderSummaryRows(report.student_summaries || []);
}

async function loadStudentSummary() {
    const classId = document.getElementById('repClassId').value;
    const studentId = document.getElementById('repStudentId').value;
    const tbody = document.getElementById('reportSummaryBody');

    const params = new URLSearchParams();
    if (classId) params.set('class_id', classId);
    if (studentId) params.set('student_id', studentId);

    const res = await callReportApi(`/api/learning/reports/student-summary?${params.toString()}`);
    if (!res.success) {
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--color-danger); text-align:center;">[${res.error?.code || 'ERR'}] ${res.error?.message || 'Không thể tải kết quả cá nhân'}</td></tr>`;
        return;
    }

    const summaries = res.data || [];
    renderSummaryRows(summaries);
}

document.addEventListener('DOMContentLoaded', loadClassSummary);