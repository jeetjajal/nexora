package com.nexora.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * Read-optimized Elasticsearch document representing a Store/Restaurant/Shop.
 * Enables fuzzy store discovery by name, cuisine, category, and status.
 */
@Document(indexName = "nexora_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long storeId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Keyword)
    private List<String> categories;
}
