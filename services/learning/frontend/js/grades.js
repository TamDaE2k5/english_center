async function requestGradeApi(path, options = {}) {
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

async function loadGradesList() {
    const classId = document.getElementById('gradeFilterClassId').value;
    const studentId = document.getElementById('gradeFilterStudentId').value;

    const params = new URLSearchParams();
    if (classId) params.set('class_id', classId);
    if (studentId) params.set('student_id', studentId);

    const tbody = document.getElementById('gradesTableBody');
    const res = await requestGradeApi(`/api/learning/grades?${params.toString()}`);

    if (!res.success) {
        tbody.innerHTML = `<tr><td colspan="8" style="color:var(--color-danger); text-align:center;">${res.error?.message || 'Không thể tải bảng điểm'}</td></tr>`;
        return;
    }

    const items = res.data || [];
    if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;">Chưa có đầu điểm nào được ghi nhận.</td></tr>`;
        return;
    }

    tbody.innerHTML = items
        .map((g) => {
        const pct = g.max_score > 0 ? ((g.score / g.max_score) * 100).toFixed(1) : '0.0';
        return `
            <tr>
            <td>#${g.id}</td>
            <td>Lớp ${g.class_id}</td>
            <td>HV #${g.student_id}</td>
            <td><strong>${g.assessment_name}</strong></td>
            <td><strong>${g.score}</strong> / ${g.max_score}</td>
            <td>${g.weight ?? 1.0}</td>
            <td>${pct}%</td>
            <td>${g.feedback || '<span style="color:var(--color-text-muted);">Chưa có nhận xét</span>'}</td>
            </tr>
        `;
        })
        .join('');
}

async function loadClassSummaryReport() {
    const classId = document.getElementById('gradeFilterClassId').value || 1;
    const summaryBody = document.getElementById('summaryTableBody');

    const res = await requestGradeApi(`/api/learning/reports/class-summary?class_id=${classId}`);
    if (!res.success || !res.data) {
        summaryBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">${res.error?.message || 'Chỉ Giảng viên/Giáo vụ/Admin mới xem được thống kê toàn lớp.'}</td></tr>`;
        return;
    }

    const report = res.data;
    document.getElementById('statTotalStudents').textContent = report.total_students;
    document.getElementById('statPassCount').textContent = report.pass_count;
    document.getElementById('statFailCount').textContent = report.fail_count;
    document.getElementById('statPassRate').textContent = `${report.pass_rate_percentage}%`;

    const list = report.student_summaries || [];
    if (list.length === 0) {
        summaryBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Chưa có dữ liệu tổng kết cho lớp ${classId}.</td></tr>`;
        return;
    }

    summaryBody.innerHTML = list
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
                ${s.result === 'Pass' ? 'ĐẠT (Pass)' : 'CHƯA ĐẠT (Fail)'}
            </span>
            </td>
        </tr>
        `
        )
        .join('');
}

function openGradeModal() {
    document.getElementById('gradeModal').style.display = 'flex';
}

function closeGradeModal() {
    document.getElementById('gradeModal').style.display = 'none';
}

async function submitCreateGrade() {
    const score = Number(document.getElementById('inpScore').value);
    const maxScore = Number(document.getElementById('inpMaxScore').value);
    const weight = Number(document.getElementById('inpWeight').value);

    if (score < 0 || score > maxScore || maxScore <= 0) {
        alert('Vi phạm INV-LRN-01: Điểm số phải nằm trong khoảng 0 <= score <= max_score!');
        return;
    }

    const payload = {
        class_id: Number(document.getElementById('inpGradeClassId').value),
        student_id: Number(document.getElementById('inpGradeStudentId').value),
        assessment_name: document.getElementById('inpAssessmentName').value.trim(),
        score: score,
        max_score: maxScore,
        weight: weight,
        feedback: document.getElementById('inpFeedback').value.trim() || null,
    };

    const res = await requestGradeApi('/api/learning/grades', {
        method: 'POST',
        body: JSON.stringify(payload),
    });

    if (!res.success) {
        alert(`[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Lỗi khi lưu điểm'}`);
        return;
    }

    closeGradeModal();
    refreshAllGradeData();
}

function refreshAllGradeData() {
    loadGradesList();
    loadClassSummaryReport();
}

document.addEventListener('DOMContentLoaded', refreshAllGradeData);