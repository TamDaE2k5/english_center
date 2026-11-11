let currentGradesPage = 1;
let cachedGrades = [];

async function callGradeApi(path, options = {}) {
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

async function loadGradesList(page = 1) {
    currentGradesPage = page;
    const classId = document.getElementById('gradeFilterClassId').value;
    const studentId = document.getElementById('gradeFilterStudentId').value;

    const params = new URLSearchParams({ page: String(page), size: '20' });
    if (classId) params.set('class_id', classId);
    if (studentId) params.set('student_id', studentId);

    const tbody = document.getElementById('gradesTableBody');
    const pagDiv = document.getElementById('gradesPagination');

    const res = await callGradeApi(`/api/learning/grades?${params.toString()}`);
    if (!res.success) {
        tbody.innerHTML = `<tr><td colspan="9" style="color:var(--color-danger); text-align:center;">[${res.error?.code || 'ERR'}] ${res.error?.message || 'Không thể tải bảng điểm'}</td></tr>`;
        pagDiv.innerHTML = '';
        return;
    }

    const payload = res.data || {};
    const items = Array.isArray(payload) ? payload : (payload.items || []);
    cachedGrades = items;
    const total = payload.total ?? items.length;
    const totalPages = payload.total_pages ?? 1;

    if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;">Chưa có bản ghi điểm nào.</td></tr>`;
        pagDiv.innerHTML = `<span class="text-sm">Tổng: 0 bản ghi</span>`;
        return;
    }

    tbody.innerHTML = items
        .map((g) => {
        const pct = g.max_score > 0 ? ((g.score / g.max_score) * 100).toFixed(2) : '0.00';
        return `
            <tr>
            <td>#${g.id}</td>
            <td>Lớp ${g.class_id}</td>
            <td>HV #${g.student_id}</td>
            <td><strong>${g.assessment_name}</strong></td>
            <td><strong>${g.score}</strong> / ${g.max_score}</td>
            <td>${g.weight ?? 1.0}</td>
            <td>${pct}%</td>
            <td>${g.feedback || '<span style="color:var(--color-text-muted);">—</span>'}</td>
            <td>
                <button class="btn btn-secondary" style="padding:4px 10px;" onclick="openEditGradeModal(${g.id})">✏️ Sửa</button>
            </td>
            </tr>
        `;
        })
        .join('');

    pagDiv.innerHTML = `
        <span class="text-sm" style="color:var(--color-text-secondary);">
        Tổng: <strong>${total}</strong> đầu điểm (Trang ${page}/${Math.max(totalPages, 1)})
        </span>
        <div style="display:flex; gap:8px;">
        <button class="btn btn-secondary" ${page <= 1 ? 'disabled' : ''} onclick="loadGradesList(${page - 1})">Trang trước</button>
        <button class="btn btn-secondary" ${page >= totalPages ? 'disabled' : ''} onclick="loadGradesList(${page + 1})">Trang sau</button>
        </div>
    `;
}

function openCreateGradeModal() {
    document.getElementById('editGradeId').value = '';
    document.getElementById('gradeModalTitle').textContent = 'Nhập Điểm Đánh Giá Mới (POST)';
    document.getElementById('inpGradeClassId').disabled = false;
    document.getElementById('inpGradeStudentId').disabled = false;
    document.getElementById('inpAssessmentName').disabled = false;
    document.getElementById('inpAssessmentName').value = '';
    document.getElementById('inpScore').value = '85';
    document.getElementById('inpMaxScore').value = '100';
    document.getElementById('inpWeight').value = '1.0';
    document.getElementById('inpFeedback').value = '';
    document.getElementById('gradeModal').style.display = 'flex';
}

function openEditGradeModal(gradeId) {
    const grade = cachedGrades.find((item) => item.id === gradeId);
    if (!grade) return;

    document.getElementById('editGradeId').value = String(grade.id);
    document.getElementById('gradeModalTitle').textContent = `Cập Nhật Điểm #${grade.id} (PUT)`;
    document.getElementById('inpGradeClassId').value = grade.class_id;
    document.getElementById('inpGradeClassId').disabled = true;
    document.getElementById('inpGradeStudentId').value = grade.student_id;
    document.getElementById('inpGradeStudentId').disabled = true;
    document.getElementById('inpAssessmentName').value = grade.assessment_name;
    document.getElementById('inpAssessmentName').disabled = true;
    document.getElementById('inpScore').value = grade.score;
    document.getElementById('inpMaxScore').value = grade.max_score;
    document.getElementById('inpWeight').value = grade.weight ?? 1.0;
    document.getElementById('inpFeedback').value = grade.feedback || '';
    document.getElementById('gradeModal').style.display = 'flex';
}

function closeGradeModal() {
    document.getElementById('gradeModal').style.display = 'none';
}

async function submitSaveGrade() {
    const editId = document.getElementById('editGradeId').value;
    const score = Number(document.getElementById('inpScore').value);
    const maxScore = Number(document.getElementById('inpMaxScore').value);
    const weightRaw = document.getElementById('inpWeight').value;
    const weight = weightRaw !== '' ? Number(weightRaw) : null;
    const feedback = document.getElementById('inpFeedback').value.trim() || null;

    if (maxScore <= 0 || score < 0 || score > maxScore) {
        alert('[LRN_INVALID_SCORE] Điểm số phải thỏa mãn 0 <= score <= max_score và max_score > 0 (INV-LRN-01)');
        return;
    }
    if (weight !== null && (weight <= 0 || weight > 1)) {
        alert('[LRN_INVALID_WEIGHT] Trọng số weight phải nằm trong khoảng (0, 1]');
        return;
    }

    let res;
    if (editId) {
        res = await callGradeApi(`/api/learning/grades/${editId}`, {
        method: 'PUT',
        body: JSON.stringify({
            score,
            max_score: maxScore,
            weight,
            feedback,
        }),
        });
    } else {
        const payload = {
        class_id: Number(document.getElementById('inpGradeClassId').value),
        student_id: Number(document.getElementById('inpGradeStudentId').value),
        assessment_name: document.getElementById('inpAssessmentName').value.trim(),
        score,
        max_score: maxScore,
        weight,
        feedback,
        };
        res = await callGradeApi('/api/learning/grades', {
        method: 'POST',
        body: JSON.stringify(payload),
        });
    }

    if (!res.success) {
        alert(`[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Lỗi khi lưu điểm'}`);
        return;
    }

    closeGradeModal();
    loadGradesList(currentGradesPage);
}

document.addEventListener('DOMContentLoaded', () => loadGradesList(1));