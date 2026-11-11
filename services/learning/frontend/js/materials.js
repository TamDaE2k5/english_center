async function requestLearningApi(path, options = {}) {
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

function formatBytes(bytes) {
    if (bytes === null || bytes === undefined) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    }

function badgeForType(type) {
    const map = {
        DOCUMENT: 'badge-info',
        SLIDE: 'badge-warning',
        VIDEO: 'badge-success',
        EXERCISE: 'badge-danger',
    };
    return `<span class="badge ${map[type] || 'badge-info'}">${type}</span>`;
}

async function loadMaterials() {
    const classId = document.getElementById('filterClassId').value;
    const type = document.getElementById('filterType').value;
    const search = document.getElementById('filterSearch').value.trim();

    const params = new URLSearchParams();
    if (classId) params.set('class_id', classId);
    if (type) params.set('material_type', type);
    if (search) params.set('search', search);

    const tbody = document.getElementById('materialsTableBody');
    try {
        const res = await requestLearningApi(`/api/learning/materials?${params.toString()}`);
        if (!res.success) {
        tbody.innerHTML = `<tr><td colspan="8" style="color:var(--color-danger); text-align:center;">${res.error?.message || 'Lỗi tải dữ liệu'}</td></tr>`;
        return;
        }
        const items = res.data || [];
        if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;">Chưa có học liệu nào cho bộ lọc này.</td></tr>`;
        return;
        }

        tbody.innerHTML = items
        .map(
            (m) => `
        <tr>
            <td>#${m.id}</td>
            <td>Lớp ${m.class_id}</td>
            <td>
            <strong>${m.title}</strong>
            ${m.description ? `<div class="text-xs" style="color:var(--color-text-secondary);">${m.description}</div>` : ''}
            </td>
            <td>${badgeForType(m.material_type)}</td>
            <td><a href="${m.file_url}" target="_blank" rel="noopener noreferrer">${m.original_filename || m.file_url}</a></td>
            <td>${formatBytes(m.file_size)}</td>
            <td>${new Date(m.uploaded_at).toLocaleDateString('vi-VN')}</td>
            <td>
            <button class="btn btn-danger" style="padding:4px 10px;" onclick="handleDeleteMaterial(${m.id})">🗑️ Xóa</button>
            </td>
        </tr>
        `
        )
        .join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" style="color:var(--color-danger); text-align:center;">Không thể kết nối tới Learning Service</td></tr>`;
    }
}

function openMaterialModal() {
    document.getElementById('materialModal').style.display = 'flex';
}

function closeMaterialModal() {
    document.getElementById('materialModal').style.display = 'none';
}

async function submitCreateMaterial() {
    const payload = {
        class_id: Number(document.getElementById('matClassId').value),
        title: document.getElementById('matTitle').value.trim(),
        material_type: document.getElementById('matType').value,
        file_url: document.getElementById('matFileUrl').value.trim(),
        original_filename: document.getElementById('matFilename').value.trim() || null,
        file_size: document.getElementById('matFileSize').value
        ? Number(document.getElementById('matFileSize').value)
        : null,
        description: document.getElementById('matDesc').value.trim() || null,
    };

    const res = await requestLearningApi('/api/learning/materials', {
        method: 'POST',
        body: JSON.stringify(payload),
    });

    if (!res.success) {
        alert(res.error?.message || 'Không thể tạo học liệu');
        return;
    }
    closeMaterialModal();
    loadMaterials();
}

async function handleDeleteMaterial(materialId) {
    if (!confirm(`Bạn có chắc chắn muốn xóa học liệu #${materialId}?`)) return;
    const res = await requestLearningApi(`/api/learning/materials/${materialId}`, {
        method: 'DELETE',
    });
    if (!res.success) {
        alert(res.error?.message || 'Không thể xóa học liệu');
        return;
    }
    loadMaterials();
    }

document.addEventListener('DOMContentLoaded', loadMaterials);