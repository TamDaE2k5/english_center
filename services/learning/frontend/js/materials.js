let currentMaterialPage = 1;

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function sanitizeUrl(url) {
    const raw = String(url ?? '').trim();
    if (/^(https?:\/\/|\/)/i.test(raw)) return raw;
    return '#';
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

async function callLearningApi(path, options = {}) {
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
    const safeType = escapeHtml(type);
    return `<span class="badge ${badgeMap[type] || 'badge-info'}">${safeType}</span>`;
}

function applyMaterialRoleVisibility() {
    const student = isStudentRole();
    const btnCreate = document.getElementById('btnOpenMaterialModal');
    const actionHeader = document.getElementById('matActionHeader');
    if (btnCreate) btnCreate.style.display = student ? 'none' : '';
    if (actionHeader) actionHeader.style.display = student ? 'none' : '';
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
    const student = isStudentRole();
    const colSpan = student ? 7 : 8;

    try {
        const res = await callLearningApi(`/api/learning/materials?${params.toString()}`);
        if (!res.success) {
            const msg = `[${res.error?.code || 'ERR'}] ${res.error?.message || 'Lỗi tải học liệu'}`;
            tbody.innerHTML = `<tr><td colspan="${colSpan}" style="color:var(--color-danger); text-align:center;">${escapeHtml(msg)}</td></tr>`;
            pagDiv.innerHTML = '';
            return;
        }

        const payload = res.data || {};
        const items = Array.isArray(payload) ? payload : (payload.items || []);
        const total = payload.total ?? items.length;
        const totalPages = payload.total_pages ?? 1;

        if (items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="${colSpan}" style="text-align:center;">Chưa có học liệu nào phù hợp.</td></tr>`;
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
            <strong>${escapeHtml(m.title)}</strong>
            ${m.description ? `<div class="text-xs" style="color:var(--color-text-secondary);">${escapeHtml(m.description)}</div>` : ''}
            </td>
            <td>${renderMaterialBadge(m.material_type)}</td>
            <td><a href="${escapeHtml(sanitizeUrl(m.file_url))}" target="_blank" rel="noopener noreferrer">${escapeHtml(m.original_filename || m.file_url)}</a></td>
            <td>${formatBytes(m.file_size)}</td>
            <td>${new Date(m.updated_at || m.uploaded_at).toLocaleDateString('vi-VN')}</td>
            ${
                student
                    ? ''
                    : `<td><button class="btn btn-danger" style="padding:4px 10px;" onclick="handleDeleteMaterial(${m.id}, this)">Xóa</button></td>`
            }
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
        tbody.innerHTML = `<tr><td colspan="${colSpan}" style="color:var(--color-danger); text-align:center;">Không thể kết nối tới Learning Service</td></tr>`;
    }
}

function openMaterialModal() {
    document.getElementById('materialModal').style.display = 'flex';
}

function closeMaterialModal() {
    document.getElementById('materialModal').style.display = 'none';
}

async function submitCreateMaterial() {
    const classId = Number(document.getElementById('matClassId').value);
    const title = document.getElementById('matTitle').value.trim();
    const fileUrl = document.getElementById('matFileUrl').value.trim();

    if (!classId || classId <= 0) {
        notifyUser('error', 'ID lớp học phải là số nguyên dương');
        return;
    }
    if (!title) {
        notifyUser('error', 'Tiêu đề học liệu không được để trống');
        return;
    }
    if (!fileUrl) {
        notifyUser('error', '[LRN_EMPTY_FILE_URL] Đường dẫn tài liệu file_url không được để trống (INV-LRN-04)');
        return;
    }

    const payload = {
        class_id: classId,
        title,
        material_type: document.getElementById('matType').value,
        file_url: fileUrl,
        original_filename: document.getElementById('matFilename').value.trim() || null,
        file_size: document.getElementById('matFileSize').value
            ? Number(document.getElementById('matFileSize').value)
            : null,
        description: document.getElementById('matDesc').value.trim() || null,
    };

    const btn = document.getElementById('btnSubmitMaterial');
    const origText = btn ? btn.textContent : 'Lưu học liệu';
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Đang lưu...';
    }

    try {
        const res = await callLearningApi('/api/learning/materials', {
            method: 'POST',
            body: JSON.stringify(payload),
        });

        if (!res.success) {
            notifyUser('error', `[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Lỗi tạo học liệu'}`);
            return;
        }

        notifyUser('success', res.message || 'Đăng tải học liệu thành công');
        closeMaterialModal();
        loadMaterials(1);
    } catch {
        notifyUser('error', 'Không thể kết nối tới Learning Service');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = origText;
        }
    }
}

async function handleDeleteMaterial(materialId, btnEl) {
    if (!confirm(`Xác nhận xóa học liệu #${materialId}?`)) return;
    if (btnEl) {
        btnEl.disabled = true;
        btnEl.textContent = 'Đang xóa...';
    }
    try {
        const res = await callLearningApi(`/api/learning/materials/${materialId}`, {
            method: 'DELETE',
        });
        if (!res.success) {
            notifyUser('error', `[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Không thể xóa học liệu'}`);
            return;
        }
        notifyUser('success', res.message || 'Xóa học liệu thành công');
        loadMaterials(currentMaterialPage);
    } catch {
        notifyUser('error', 'Không thể kết nối tới Learning Service');
    } finally {
        if (btnEl) {
            btnEl.disabled = false;
            btnEl.textContent = 'Xóa';
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof requireAuth === 'function') requireAuth();
    applyMaterialRoleVisibility();
    loadMaterials(1);
});
