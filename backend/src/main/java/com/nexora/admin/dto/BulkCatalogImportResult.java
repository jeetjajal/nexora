package com.nexora.admin.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCatalogImportResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private int duplicateCount;
    private List<Long> importedProductIds;
    private List<String> validationErrors;
}
