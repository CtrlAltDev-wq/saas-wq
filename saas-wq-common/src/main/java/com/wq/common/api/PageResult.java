package com.wq.common.api;

import java.util.List;

/**
 * Pagination payload shared by every list endpoint.
 *
 * @param records current page records
 * @param total   total number of matched records
 * @param page    one based page index
 * @param size    page size
 * @param pages   total number of pages
 * @param <T>     record type
 */
public record PageResult<T>(List<T> records, long total, long page, long size, long pages) {

    /**
     * Creates a page result and derives the total page count.
     *
     * @param records current page records
     * @param total   total number of matched records
     * @param page    one based page index
     * @param size    page size
     * @param <T>     record type
     * @return page result
     */
    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        long pages = 0L;
        if (size > 0L) {
            pages = (total + size - 1L) / size;
        }
        return new PageResult<>(records, total, page, size, pages);
    }
}
