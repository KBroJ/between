package com.wb.between.common.util.pagination;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PaginationInfo {
    private final Page<?> page;
    private final String baseUrl;
    private final Map<String, Object> additionalParams;
    private final int displayWindowSize;

    private int currentPage; // 0-based
    private int totalPages;
    private int startPage;   // 0-based
    private int endPage;     // 0-based
    private List<Integer> pageNumbers; // 0-based page numbers to display

    public PaginationInfo(Page<?> page, String baseUrl, Map<String, Object> additionalParams, int displayWindowSize) {
        this.page = page;
        this.baseUrl = baseUrl;
        this.additionalParams = (additionalParams != null) ? new HashMap<>(additionalParams) : new HashMap<>();
        this.displayWindowSize = displayWindowSize;
        calculate();
    }

    private void calculate() {
        this.currentPage = page.getNumber();
        this.totalPages = page.getTotalPages();

        if (totalPages == 0) {
            this.startPage = 0;
            this.endPage = 0;
            this.pageNumbers = Collections.emptyList();
            return;
        }

        if (totalPages <= displayWindowSize) {
            this.startPage = 0;
            this.endPage = totalPages - 1;
        } else {
            int halfWindow = displayWindowSize / 2;
            this.startPage = Math.max(0, currentPage - halfWindow);
            this.endPage = startPage + displayWindowSize - 1;
            if (this.endPage >= totalPages) {
                this.endPage = totalPages - 1;
                this.startPage = Math.max(0, this.endPage - displayWindowSize + 1);
            }
        }
        this.pageNumbers = IntStream.rangeClosed(this.startPage, this.endPage).boxed().collect(Collectors.toList());
    }

    // Getters for page, baseUrl, additionalParams, currentPage, totalPages, startPage, endPage, pageNumbers
    // Getters for isFirst, isLast, hasPrevious, hasNext
    // Method to build URL for a specific page number
    public String getUrlForPage(int pageNumber) {
        // UriComponentsBuilder 사용 추천
        // return UriComponentsBuilder.fromPath(baseUrl)
        //         .queryParam("page", pageNumber)
        //         .queryParams(new LinkedMultiValueMap<>(additionalParams.entrySet().stream()
        //             .collect(Collectors.toMap(Map.Entry::getKey, e -> Collections.singletonList(String.valueOf(e.getValue()))))))
        //         .toUriString();
        // Thymeleaf @{} 구문을 프래그먼트에서 쓰려면 baseUrl과 additionalParams를 프래그먼트에 직접 전달하는 것이 더 간단할 수 있습니다.
        // 이 클래스는 주로 계산 로직을 캡슐화하는 데 중점을 둡니다.
        return ""; // 실제 URL 생성 로직 필요
    }

    public Page<?> getPage() { return page; }
    public String getBaseUrl() { return baseUrl; }
    public Map<String, Object> getAdditionalParams() { return additionalParams; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public int getStartPage() { return startPage; } // 이 값과 아래 endPage를 Thymeleaf의 #numbers.sequence에 사용 가능
    public int getEndPage() { return endPage; }
    public List<Integer> getPageNumbers() { return pageNumbers; } // 또는 이 리스트를 직접 순회

    public boolean isFirst() { return page.isFirst(); }
    public boolean isLast() { return page.isLast(); }
    public int getPreviousPageNumber() { return page.getNumber() - 1; }
    public int getNextPageNumber() { return page.getNumber() + 1; }
    public int getFirstPageLink() { return 0; }
    public int getLastPageLink() { return totalPages > 0 ? totalPages - 1 : 0; }
}
