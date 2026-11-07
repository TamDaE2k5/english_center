let currentUser = null;

document.addEventListener('DOMContentLoaded', () => {
    requireAuth();
    currentUser = getCurrentUser();
    
    // Phân quyền UI
    if (['ADMIN', 'STAFF', 'TEACHER'].includes(currentUser.role)) {
        document.getElementById('adminActions').style.display = 'block';
        document.getElementById('filterSection').style.display = 'block';
    }

    loadGrades();
});

async function loadGrades() {
    let url = '/api/learning/grades';
    
    // Nếu là giáo viên/admin có thể lấy class_id từ bộ lọc
    if (['ADMIN', 'STAFF', 'TEACHER'].includes(currentUser.role)) {
        const classId = document.getElementById('filterClassId').value;
        if (classId) {
        url += `?class_id=${classId}`;
        }
    }
    // Nếu là STUDENT, backend tự động filter theo Token (INV-LRN-02), frontend ko cần truyền student_id

    try {
        const res = await apiGet(url);
        if (res && res.success) {
        renderTable(res.data);
        }
    } catch (err) {}
}

function renderTable(grades) {
    const tbody = document.getElementById('gradeTableBody');
    tbody.innerHTML = '';
    
    if (grades.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Chưa có dữ liệu điểm.</td></tr>';
        return;
    }

    grades.forEach(g => {
        // Đánh giá màu sắc dựa trên điểm
        let scoreColor = 'badge-success';
        if (g.score < g.max_score * 0.5) scoreColor = 'badge-danger';
        else if (g.score < g.max_score * 0.8) scoreColor = 'badge-warning';

        const tr = document.createElement('tr');
        tr.innerHTML = `
        <td>${g.id}</td>
        <td>${g.class_id}</td>
        <td>${g.student_id}</td>
        <td>${g.assessment_name}</td>
        <td><span class="badge ${scoreColor}">${g.score} / ${g.max_score}</span></td>
        <td>${g.feedback || '-'}</td>
        <td>${formatDate(g.graded_at)}</td>
        `;
        tbody.appendChild(tr);
    });
    }

function openGradeModal() {
    document.getElementById('modalAddGrade').style.display = 'flex';
}

function closeGradeModal() {
    document.getElementById('modalAddGrade').style.display = 'none';
    document.getElementById('formAddGrade').reset();
}

async function submitGrade() {
    const payload = {
        class_id: parseInt(document.getElementById('gradeClassId').value),
        student_id: parseInt(document.getElementById('gradeStudentId').value),
        assessment_name: document.getElementById('gradeAssessment').value,
        score: parseFloat(document.getElementById('gradeScore').value),
        max_score: parseFloat(document.getElementById('gradeMaxScore').value),
        feedback: document.getElementById('gradeFeedback').value
    };

    // Validate phía client (INV-LRN-01)
    if (payload.score < 0 || payload.score > payload.max_score) {
        showToast('error', 'LRN_GRADE_INVALID: Điểm số phải nằm trong khoảng 0 đến Điểm tối đa');
        return;
    }

    try {
        const res = await apiPost('/api/learning/grades', payload);
        if (res && res.success) {
        showToast('success', 'Nhập điểm thành công');
        closeGradeModal();
        loadGrades();
        }
    } catch (err) {
        // API sẽ trả về lỗi nếu có vấn đề (như Teacher không dạy lớp này)
    }
}

document.getElementById('btnAddGrade').addEventListener('click', openGradeModal);