let currentMaterialPage = 1;

async function callLearningApi(path, options = {}) {
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

function renderMaterialBadge(type) {
    const badgeMap = {
        DOCUMENT: 'badge-info',
        SLIDE: 'badge-warning',
        VIDEO: 'badge-success',
        EXERCISE: 'badge-danger',
    };
    return `<span class="badge ${badgeMap[type] || 'badge-info'}">${type}</span>`;
}

async function loadMaterials(page = 1) {
    currentMaterialPage = page;
    const classId = document.getElementById('filterClassId').value;
    const type = document.getElementById('filterType').value;
    const search = document.getElementById('filterSearch').value.trim();

    const params = new URLSearchParams({ page: String(page), size: '20' });
    if (classId) params.set('class_id', classId);
    if (type) params.set('material_type', type);
    if (search) params.set('search', search);

    const tbody = document.getElementById('materialsTableBody');
    const pagDiv = document.getElementById('materialsPagination');

    try {
        const res = await callLearningApi(`/api/learning/materials?${params.toString()}`);
        if (!res.success) {
        tbody.innerHTML = `<tr><td colspan="8" style="color:var(--color-danger); text-align:center;">[${res.error?.code || 'ERR'}] ${res.error?.message || 'Lỗi tải học liệu'}</td></tr>`;
        pagDiv.innerHTML = '';
        return;
        }

        const payload = res.data || {};
        const items = Array.isArray(payload) ? payload : (payload.items || []);
        const total = payload.total ?? items.length;
        const totalPages = payload.total_pages ?? 1;

        if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;">Chưa có học liệu nào phù hợp.</td></tr>`;
        pagDiv.innerHTML = `<span class="text-sm">Tổng số: 0 học liệu</span>`;
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
            <td>${renderMaterialBadge(m.material_type)}</td>
            <td><a href="${m.file_url}" target="_blank" rel="noopener noreferrer">${m.original_filename || m.file_url}</a></td>
            <td>${formatBytes(m.file_size)}</td>
            <td>${new Date(m.updated_at || m.uploaded_at).toLocaleDateString('vi-VN')}</td>
            <td>
            <button class="btn btn-danger" style="padding:4px 10px;" onclick="handleDeleteMaterial(${m.id})"> Xóa</button>
            </td>
        </tr>
        `
        )
        .join('');

        pagDiv.innerHTML = `
        <span class="text-sm" style="color:var(--color-text-secondary);">
            Tổng: <strong>${total}</strong> học liệu (Trang ${page}/${Math.max(totalPages, 1)})
        </span>
        <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary" ${page <= 1 ? 'disabled' : ''} onclick="loadMaterials(${page - 1})">Trang trước</button>
            <button class="btn btn-secondary" ${page >= totalPages ? 'disabled' : ''} onclick="loadMaterials(${page + 1})">Trang sau</button>
        </div>
        `;
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
    const fileUrl = document.getElementById('matFileUrl').value.trim();
    if (!fileUrl) {
        alert('[LRN_EMPTY_FILE_URL] Đường dẫn tài liệu file_url không được để trống (INV-LRN-04)');
        return;
    }

    const payload = {
        class_id: Number(document.getElementById('matClassId').value),
        title: document.getElementById('matTitle').value.trim(),
        material_type: document.getElementById('matType').value,
        file_url: fileUrl,
        original_filename: document.getElementById('matFilename').value.trim() || null,
        file_size: document.getElementById('matFileSize').value
        ? Number(document.getElementById('matFileSize').value)
        : null,
        description: document.getElementById('matDesc').value.trim() || null,
    };

    const res = await callLearningApi('/api/learning/materials', {
        method: 'POST',
        body: JSON.stringify(payload),
    });

    if (!res.success) {
        alert(`[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Lỗi tạo học liệu'}`);
        return;
    }

    closeMaterialModal();
    loadMaterials(1);
}

async function handleDeleteMaterial(materialId) {
    if (!confirm(`Xác nhận xóa học liệu #${materialId}?`)) return;
    const res = await callLearningApi(`/api/learning/materials/${materialId}`, {
        method: 'DELETE',
    });
    if (!res.success) {
        alert(`[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Không thể xóa học liệu'}`);
        return;
    }
    loadMaterials(currentMaterialPage);
}

document.addEventListener('DOMContentLoaded', () => loadMaterials(1));