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
 * Read-optimized Elasticsearch document representing a Product.
 * Provides full-text search across name, description, categories, and store name,
 * with typo tolerance (fuzzy matching), filtering, sorting, and autocomplete.
 */
@Document(indexName = "nexora_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long productId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Double)
    private Double discount;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Boolean)
    private Boolean available;

    @Field(type = FieldType.Long)
    private Long storeId;

    @Field(type = FieldType.Text)
    private String storeName;

    @Field(type = FieldType.Keyword)
    private List<String> categories;
}
