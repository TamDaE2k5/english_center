let currentUser = null;

document.addEventListener('DOMContentLoaded', () => {
    requireAuth();
    currentUser = getCurrentUser();
    
    // Phân quyền UI
    if (['ADMIN', 'STAFF', 'TEACHER'].includes(currentUser.role)) {
        document.getElementById('btnAddMaterial').style.display = 'block';
    }

    loadMaterials();
});

async function loadMaterials() {
    try {
        const res = await apiGet('/api/learning/materials');
        if (res && res.success) {
        renderTable(res.data);
        }
    } catch (err) {}
}

function renderTable(materials) {
    const tbody = document.getElementById('materialTableBody');
    tbody.innerHTML = '';
    
    if (materials.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Chưa có học liệu nào.</td></tr>';
        return;
    }

    materials.forEach(m => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
        <td>${m.id}</td>
        <td>${m.title}</td>
        <td>${m.description || '-'}</td>
        <td><span class="badge badge-info">${m.material_type}</span></td>
        <td>${formatDate(m.uploaded_at)}</td>
        <td>
            ${m.file_url ? `<a href="${m.file_url}" target="_blank" class="btn btn-ghost btn-sm">📥 Tải về</a>` : '-'}
            ${['ADMIN', 'STAFF', 'TEACHER'].includes(currentUser.role) ? 
            `<button class="btn-icon text-danger" onclick="deleteMaterial(${m.id})" title="Xóa">🗑️</button>` : ''}
        </td>
        `;
        tbody.appendChild(tr);
    });
}

function openModal() {
    document.getElementById('modalAddMaterial').style.display = 'flex';
}

function closeModal() {
    document.getElementById('modalAddMaterial').style.display = 'none';
    document.getElementById('formAddMaterial').reset();
}

async function submitMaterial() {
    const payload = {
        class_id: parseInt(document.getElementById('matClassId').value),
        title: document.getElementById('matTitle').value,
        description: document.getElementById('matDesc').value,
        material_type: document.getElementById('matType').value,
        file_url: document.getElementById('matUrl').value
    };

    if (!payload.class_id || !payload.title) {
        showToast('warning', 'Vui lòng nhập đủ thông tin bắt buộc');
        return;
    }

    try {
        const res = await apiPost('/api/learning/materials', payload);
        if (res && res.success) {
        showToast('success', 'Thêm học liệu thành công');
        closeModal();
        loadMaterials();
        }
    } catch (err) {}
    }

    async function deleteMaterial(id) {
    if (!confirm('Bạn có chắc muốn xóa học liệu này?')) return;
    try {
        await apiDelete(`/api/learning/materials/${id}`);
        showToast('success', 'Đã xóa học liệu');
        loadMaterials();
    } catch (err) {}
}

// Gắn sự kiện
document.getElementById('btnAddMaterial').addEventListener('click', openModal);