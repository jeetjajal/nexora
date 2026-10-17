# Phase 18 — Search Engine Integration (Elasticsearch / OpenSearch)

## Overview

In **Phase 18**, Nexora integrates a distributed search and analytics engine (**Elasticsearch / OpenSearch**) to deliver ultra-fast, read-optimized search capabilities.

While **MySQL remains the single source of truth** (ACID transactional guarantees for orders, payments, carts, and inventory), the search engine enables:
1. **Full-Text Multi-Field Search**: Searching product name and description with relevance ranking (BM25 algorithm).
2. **Typo Tolerance & Fuzzy Matching**: Matches misspelled queries (e.g., "piza" -> "Pizza", "burgur" -> "Burger").
3. **Multi-Faceted Filtering & Sorting**: Composable filters by category, price range (`minPrice`, `maxPrice`), availability, and store ID with sorting and pagination.
4. **Instant Prefix Autocomplete**: Real-time typeahead suggestions for customer search bars.
5. **Dual-Write Synchronization**: Continuous index updates triggered on product and store write/update/delete operations, paired with a bulk re-indexing API (`POST /api/v1/search/sync`).
6. **Graceful High-Availability Fallback**: If the search cluster is unreachable, offline, or disabled, search calls automatically fall back to MySQL Criteria/Specifications (`ProductSpecifications` / `StoreSpecifications`) without throwing 500 errors.

---

## Architecture

```
                          +------------------------+
                          |   Customer / Frontend  |
                          +-----------+------------+
                                      |
                     GET /api/v1/search/products, stores, autocomplete
                                      |
                                      v
                          +------------------------+
                          |    SearchController    |
                          +-----------+------------+
                                      |
                                      v
                        +----------------------------+
                        |    ProductSearchService    |
                        +--------------+-------------+
                                       |
                   +-------------------+-------------------+
                   | (Primary: Read)                       | (Circuit-Breaker Fallback)
                   v                                       v
     +--------------------------+             +-------------------------+
     |   Elasticsearch Cluster  |             |      MySQL Database     |
     | (nexora_products,        |             | (products, stores,      |
     |  nexora_stores indexes)  |             |  categories tables)     |
     +--------------------------+             +-------------------------+
                   ^                                       ^
                   |                                       |
                   +-------------------+-------------------+
                                       |
                            Dual-Write Sync / Bulk Sync
                                       |
                          +------------+-----------+
                          |    SearchSyncService   |
                          +------------------------+
```

---

## API Endpoints

### 1. Product Search
- **Endpoint**: `GET /api/v1/search/products`
- **Access**: Public
- **Query Parameters**:
  - `query` (optional): search term (fuzzy-matched against name and description)
  - `category` (optional): category name filter
  - `minPrice` (optional): minimum price
  - `maxPrice` (optional): maximum price
  - `available` (optional): boolean availability filter
  - `storeId` (optional): filter products of a specific store
  - `page` (default 0): page number
  - `size` (default 20): items per page
  - `sortBy` (default "id"): sort field
  - `sortDir` (default "asc" / "desc"): sort order

**Sample Response**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Margherita Pizza",
      "description": "Classic tomato and mozzarella pizza",
      "price": 299.00,
      "discount": 10.00,
      "imageUrl": "https://example.com/pizza.jpg",
      "available": true,
      "storeId": 5,
      "storeName": "Domino's Pizza",
      "categories": ["Pizza", "Italian"],
      "source": "ELASTICSEARCH"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 1,
  "totalPages": 1
}
```

### 2. Store Search
- **Endpoint**: `GET /api/v1/search/stores`
- **Access**: Public
- **Query Parameters**:
  - `query` (optional): search term (store name or cuisine)
  - `category` (optional): category filter
  - `page` (default 0), `size` (default 20)

### 3. Autocomplete / Typeahead
- **Endpoint**: `GET /api/v1/search/autocomplete`
- **Access**: Public
- **Query Parameters**:
  - `prefix` or `q`: prefix characters (e.g. `bur`)
  - `limit` (default 10): max suggestions

**Sample Response**:
```json
{
  "query": "bur",
  "suggestions": [
    "Burger King Whopper",
    "Burger Joint"
  ],
  "products": [
    { "id": 10, "name": "Burger King Whopper", "price": 199.00 }
  ],
  "stores": [
    { "id": 4, "name": "Burger Joint", "status": "OPEN" }
  ]
}
```

### 4. Bulk Catalog Re-Index
- **Endpoint**: `POST /api/v1/search/sync`
- **Access**: Permitted / Admin
- **Description**: Reads all products and stores from MySQL and bulk saves documents into Elasticsearch.

**Sample Response**:
```json
{
  "status": "SUCCESS",
  "message": "Elasticsearch index synchronization complete.",
  "productsSynced": 50,
  "storesSynced": 12,
  "durationMs": 85
}
```

---

## Configuration

In `src/main/resources/application.properties`:
```properties
# ELASTICSEARCH / OPENSEARCH (Phase 18)
spring.elasticsearch.uris=${ELASTICSEARCH_URIS:http://localhost:9200}
spring.elasticsearch.connection-timeout=2s
spring.elasticsearch.socket-timeout=3s
nexora.search.enabled=${NEXORA_SEARCH_ENABLED:true}
nexora.search.elasticsearch.enabled=${NEXORA_ES_ENABLED:true}
```

### Running Elasticsearch Locally (Docker)
```bash
docker run -d --name nexora-elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.13.4
```
