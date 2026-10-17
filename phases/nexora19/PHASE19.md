# Phase 19 — AI Assistant (Nexora AI)

## Overview

In **Phase 19**, Nexora introduces **Nexora AI** — an intelligent conversational customer assistant powered by Large Language Models (**Google Gemini** / Anthropic Claude) with **Safe Function Calling (Tools)**.

Nexora AI enables customers to interact in natural language to:
1. **Track live order status** (e.g., *"Where is my order #12?"*).
2. **Review itemized order details** (e.g., *"What did I order in order #5?"*).
3. **Browse order history** (e.g., *"Show my recent orders"*).
4. **Discover open stores and cuisines** (e.g., *"Which pizza stores are open right now?"*).
5. **Search products with budgets** (e.g., *"Find veg food under ₹300"*).

---

## 🛡️ Security Architecture (Principle of Least Privilege)

> [!IMPORTANT]
> **AI never accesses the MySQL database directly.**  
> The model cannot generate raw SQL queries or execute arbitrary database lookups.

### Communication Flow:
```
Customer / Frontend
       |
       |  POST /api/v1/ai/chat (Authenticated with JWT Bearer Token)
       v
+------------------------+
|    AiChatController    |
+-----------+------------+
            |
            v
+------------------------+
|     AiChatService      |
+-----------+------------+
            |
            v
+------------------------+      Function Call Request      +---------------------+
|    GeminiAiClient /    | ------------------------------> |  Google Gemini API  |
|      StubAiClient      | <------------------------------ | (gemini-1.5-flash)  |
+-----------+------------+      Invokes function_call      +---------------------+
            |
            | Executes approved Java tools with authenticated UserPrincipal
            v
+--------------------------------------------------------------+
|                    Approved Backend Tools                    |
| - OrderAiTools.getOrderStatus(orderId, caller)               |
| - OrderAiTools.getOrderDetails(orderId, caller)              |
| - OrderAiTools.getUserOrderHistory(caller)                   |
| - StoreAiTools.getAvailableStores(category)                  |
| - ProductAiTools.searchProducts(query, category, maxPrice)   |
+------------------------------+-------------------------------+
                               |
                               v
+--------------------------------------------------------------+
|                 Existing Core Service Layer                  |
| - OrderService (enforces user ownership & access control)    |
| - ProductSearchService (Elasticsearch / Database fallback)   |
+------------------------------+-------------------------------+
                               |
                               v
+--------------------------------------------------------------+
|                   MySQL Database (ACID)                      |
+--------------------------------------------------------------+
```

---

## 🔑 Safe API Key Setup (Zero Git Leak Risk)

You do **NOT** need to write or commit your actual API key to Git. Two secure methods are supported:

### Method 1: System / Shell Environment Variable (Recommended)
Set the key in your terminal or IDE run configuration:
```powershell
$env:GEMINI_API_KEY="YOUR_REAL_GEMINI_API_KEY"
```
*(The key exists solely in your machine's session memory, never in any file)*

### Method 2: Gitignored Local File
1. Copy `application-local.properties.example` to `application-local.properties` (or `src/main/resources/application-local.properties`).
2. Add your key inside:
   ```properties
   GEMINI_API_KEY=YOUR_REAL_GEMINI_API_KEY
   ```
3. `application-local.properties` is strictly declared in `.gitignore` and can **never** be committed or pushed to GitHub.

---

## 📡 REST API & Web UI

### Endpoint:
- **URL**: `POST /api/v1/ai/chat`
- **Security**: Requires JWT Bearer Token (`Authorization: Bearer <token>`)

**Request Payload**:
```json
{
  "message": "Where is my order #12?",
  "history": [
    { "role": "user", "content": "Hi!" },
    { "role": "assistant", "content": "Hello! How can I assist you with your orders today?" }
  ]
}
```

**Response Payload**:
```json
{
  "reply": "Order #12 from 'Pizza Palace' is currently 'OUT_FOR_DELIVERY'. Placed on 2026-10-17. Total amount: ₹350.00.",
  "intent": "getOrderStatus",
  "toolsExecuted": ["getOrderStatus"],
  "provider": "GEMINI",
  "success": true
}
```

### Interactive Web Chat UI:
- Open `http://localhost:8080/chat.html` in your browser.
- Paste your JWT access token to authenticate.
- Chat with Nexora AI with instant tool badges and conversational history.
