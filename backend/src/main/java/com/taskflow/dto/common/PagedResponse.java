package com.taskflow.dto.common;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public static <T> PagedResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        int totalPages = pageSize == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) pageSize);
        boolean isLast = pageNumber >= totalPages - 1;
        return PagedResponse.<T>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(isLast)
                .build();
    }
}
