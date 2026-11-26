function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function notifyUser(type, message) {
    if (typeof showToast === 'function') {
        showToast(type, message);
    } else {
        alert(message);
    }
}

function getAuthToken() {
    if (typeof getToken === 'function' && getToken()) return getToken();
    return localStorage.getItem('ec_token') || localStorage.getItem('access_token') || localStorage.getItem('token') || '';
}

function getUserRole() {
    if (typeof getCurrentUser === 'function') {
        const u = getCurrentUser();
        if (u && u.role) return String(u.role).toUpperCase();
    }
    return '';
}

function isStudentRole() {
    return getUserRole() === 'STUDENT';
}

async function callReportApi(path, options = {}) {
    const token = getAuthToken();
    const headers = Object.assign(
        { 'Content-Type': 'application/json' },
        token ? { Authorization: `Bearer ${token}` } : {},
        options.headers || {}
    );
    const response = await fetch(path, Object.assign({}, options, { headers }));
    if (response.status === 401) {
        notifyUser('error', 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        if (typeof logout === 'function') {
            setTimeout(() => logout(), 1200);
        }
        return { success: false, error: { code: 'AUTH_UNAUTHORIZED', message: 'Phiên đăng nhập đã hết hạn' } };
    }
    const text = await response.text();
    try {
        return text ? JSON.parse(text) : { success: response.ok };
    } catch {
        return {
            success: false,
            error: { code: `HTTP_${response.status}`, message: text || `Lỗi HTTP ${response.status}` },
        };
    }
}

function applyReportsRoleVisibility() {
    if (isStudentRole()) {
        const btnClass = document.getElementById('btnClassSummary');
        const studentGroup = document.getElementById('repStudentGroup');
        const classInput = document.getElementById('repClassId');
        if (btnClass) btnClass.style.display = 'none';
        if (studentGroup) studentGroup.style.display = 'none';
        if (classInput) classInput.value = '';
    }
}

function updateKpiFromSummaries(summaries) {
    const list = summaries || [];
    const total = list.length;
    const passCount = list.filter((s) => s.result === 'Pass').length;
    const failCount = list.filter((s) => s.result === 'Fail').length;
    const passRate = total > 0 ? ((passCount / total) * 100).toFixed(2) : '0';

    document.getElementById('kpiTotalStudents').textContent = total;
    document.getElementById('kpiPassCount').textContent = passCount;
    document.getElementById('kpiFailCount').textContent = failCount;
    document.getElementById('kpiPassRate').textContent = `${passRate}%`;
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
    const btn = document.getElementById('btnClassSummary');
    if (btn) btn.disabled = true;

    try {
        const res = await callReportApi(`/api/learning/reports/class-summary?class_id=${encodeURIComponent(classId)}`);
        if (!res.success) {
            if (res.error?.code === 'LRN_FORBIDDEN_CLASS' && isStudentRole()) {
                await loadStudentSummary();
                return;
            }
            const msg = `[${res.error?.code || 'ERR'}] ${res.error?.message || 'Không thể tải báo cáo lớp'}`;
            tbody.innerHTML = `<tr><td colspan="6" style="color:var(--color-danger); text-align:center;">${escapeHtml(msg)}</td></tr>`;
            return;
        }

        const report = res.data || {};
        document.getElementById('kpiTotalStudents').textContent = report.total_students ?? 0;
        document.getElementById('kpiPassCount').textContent = report.pass_count ?? 0;
        document.getElementById('kpiFailCount').textContent = report.fail_count ?? 0;
        document.getElementById('kpiPassRate').textContent = `${report.pass_rate_percentage ?? 0}%`;

        renderSummaryRows(report.student_summaries || []);
    } catch {
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--color-danger); text-align:center;">Không thể kết nối tới Learning Service</td></tr>`;
    } finally {
        if (btn) btn.disabled = false;
    }
}

async function loadStudentSummary() {
    const classId = document.getElementById('repClassId').value;
    const studentId = isStudentRole() ? '' : document.getElementById('repStudentId').value;
    const tbody = document.getElementById('reportSummaryBody');
    const btn = document.getElementById('btnStudentSummary');
    if (btn) btn.disabled = true;

    const params = new URLSearchParams();
    if (classId) params.set('class_id', classId);
    if (studentId) params.set('student_id', studentId);

    try {
        const query = params.toString();
        const url = query ? `/api/learning/reports/student-summary?${query}` : '/api/learning/reports/student-summary';
        const res = await callReportApi(url);
        if (!res.success) {
            const msg = `[${res.error?.code || 'ERR'}] ${res.error?.message || 'Không thể tải kết quả cá nhân'}`;
            tbody.innerHTML = `<tr><td colspan="6" style="color:var(--color-danger); text-align:center;">${escapeHtml(msg)}</td></tr>`;
            return;
        }

        const summaries = res.data || [];
        updateKpiFromSummaries(summaries);
        renderSummaryRows(summaries);
    } catch {
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--color-danger); text-align:center;">Không thể kết nối tới Learning Service</td></tr>`;
    } finally {
        if (btn) btn.disabled = false;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof requireAuth === 'function') requireAuth();
    applyReportsRoleVisibility();
    if (isStudentRole()) {
        loadStudentSummary();
    } else {
        loadClassSummary();
    }
});
