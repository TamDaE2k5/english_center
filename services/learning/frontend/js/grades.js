let currentGradesPage = 1;
let cachedGrades = [];

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

async function callGradeApi(path, options = {}) {
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

function applyGradesRoleVisibility() {
    const student = isStudentRole();
    const btnCreate = document.getElementById('btnOpenCreateGradeModal');
    const studentFilterGroup = document.getElementById('gradeFilterStudentGroup');
    const actionHeader = document.getElementById('gradeActionHeader');
    if (btnCreate) btnCreate.style.display = student ? 'none' : '';
    if (studentFilterGroup) studentFilterGroup.style.display = student ? 'none' : '';
    if (actionHeader) actionHeader.style.display = student ? 'none' : '';
}

async function loadGradesList(page = 1) {
    currentGradesPage = page;
    const classId = document.getElementById('gradeFilterClassId').value;
    const studentId = isStudentRole() ? '' : document.getElementById('gradeFilterStudentId').value;

    const params = new URLSearchParams({ page: String(page), size: '20' });
    if (classId) params.set('class_id', classId);
    if (studentId) params.set('student_id', studentId);

    const tbody = document.getElementById('gradesTableBody');
    const pagDiv = document.getElementById('gradesPagination');
    const student = isStudentRole();
    const colSpan = student ? 8 : 9;

    try {
        const res = await callGradeApi(`/api/learning/grades?${params.toString()}`);
        if (!res.success) {
            const msg = `[${res.error?.code || 'ERR'}] ${res.error?.message || 'Không thể tải bảng điểm'}`;
            tbody.innerHTML = `<tr><td colspan="${colSpan}" style="color:var(--color-danger); text-align:center;">${escapeHtml(msg)}</td></tr>`;
            pagDiv.innerHTML = '';
            return;
        }

        const payload = res.data || {};
        const items = Array.isArray(payload) ? payload : (payload.items || []);
        cachedGrades = items;
        const total = payload.total ?? items.length;
        const totalPages = payload.total_pages ?? 1;

        if (items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="${colSpan}" style="text-align:center;">Chưa có bản ghi điểm nào.</td></tr>`;
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
                <td><strong>${escapeHtml(g.assessment_name)}</strong></td>
                <td><strong>${g.score}</strong> / ${g.max_score}</td>
                <td>${g.weight ?? 1.0}</td>
                <td>${pct}%</td>
                <td>${g.feedback ? escapeHtml(g.feedback) : '<span style="color:var(--color-text-muted);">—</span>'}</td>
                ${
                    student
                        ? ''
                        : `<td><button class="btn btn-secondary" style="padding:4px 10px;" onclick="openEditGradeModal(${g.id})">Sửa</button></td>`
                }
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
    } catch {
        tbody.innerHTML = `<tr><td colspan="${colSpan}" style="color:var(--color-danger); text-align:center;">Không thể kết nối tới Learning Service</td></tr>`;
    }
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

    if (Number.isNaN(score) || Number.isNaN(maxScore) || maxScore <= 0 || score < 0 || score > maxScore) {
        notifyUser('error', '[LRN_INVALID_SCORE] Điểm số phải thỏa mãn 0 <= score <= max_score và max_score > 0 (INV-LRN-01)');
        return;
    }
    if (weight !== null && (Number.isNaN(weight) || weight <= 0 || weight > 1)) {
        notifyUser('error', '[LRN_INVALID_WEIGHT] Trọng số weight phải nằm trong khoảng (0, 1]');
        return;
    }

    const btn = document.getElementById('btnSubmitGrade');
    const origText = btn ? btn.textContent : 'Lưu điểm';

    if (!editId) {
        const classId = Number(document.getElementById('inpGradeClassId').value);
        const studentId = Number(document.getElementById('inpGradeStudentId').value);
        const assessmentName = document.getElementById('inpAssessmentName').value.trim();
        if (!classId || classId <= 0 || !studentId || studentId <= 0) {
            notifyUser('error', 'Mã lớp và mã học viên phải là số nguyên dương');
            return;
        }
        if (!assessmentName) {
            notifyUser('error', 'Tên đầu điểm (assessment_name) không được để trống');
            return;
        }
    }

    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Đang lưu...';
    }

    try {
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
            notifyUser('error', `[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Lỗi khi lưu điểm'}`);
            return;
        }

        notifyUser('success', res.message || 'Lưu điểm thành công');
        closeGradeModal();
        loadGradesList(currentGradesPage);
    } catch {
        notifyUser('error', 'Không thể kết nối tới Learning Service');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = origText;
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof requireAuth === 'function') requireAuth();
    applyGradesRoleVisibility();
    loadGradesList(1);
});
