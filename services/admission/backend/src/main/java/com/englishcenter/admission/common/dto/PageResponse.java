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

    /**
     * Phương thức nhà máy tạo đối tượng PageResponse từ các tham số cơ bản.
     *
     * @param items Danh sách phần tử
     * @param page  Chỉ số trang (0-based)
     * @param size  Số phần tử trên trang
     * @param total Tổng số phần tử
     * @param <T>   Kiểu dữ liệu
     * @return Đối tượng PageResponse
     */
    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        return new PageResponse<>(items, total, page, size);
    }

    /**
     * Phương thức nhà máy tạo đối tượng PageResponse từ Spring Data Page.
     *
     * @param springPage Trang dữ liệu của Spring Data
     * @param <T>        Kiểu dữ liệu
     * @return Đối tượng PageResponse
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> springPage) {
        return new PageResponse<>(springPage.getContent(), springPage.getTotalElements(), springPage.getNumber(), springPage.getSize());
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
