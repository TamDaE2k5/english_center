package com.englishcenter.admission.common.dto;

import java.util.List;

/**
 * Cấu trúc phân trang chuẩn cho các danh sách trả về trong Admission Service.
 * Tuân thủ mục 8.3 của MASTER_DB_AND_API_RULES.md.
 * 
 * @param <T> Kiểu dữ liệu của phần tử trong danh sách
 */
public class PageResponse<T> {

    /** Danh sách phần tử của trang hiện tại */
    private List<T> items;

    /** Tổng số bản ghi thỏa mãn điều kiện */
    private long total;

    /** Chỉ số trang hiện tại (0-based) */
    private int page;

    /** Số bản ghi trên mỗi trang */
    private int size;

    /** Tổng số trang */
    private int totalPages;

    public PageResponse() {
    }

    public PageResponse(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
