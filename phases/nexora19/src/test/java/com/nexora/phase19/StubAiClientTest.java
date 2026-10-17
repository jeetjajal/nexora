package com.nexora.phase19;

import com.nexora.ai.client.StubAiClient;
import com.nexora.ai.tool.OrderAiTools;
import com.nexora.ai.tool.ProductAiTools;
import com.nexora.ai.tool.StoreAiTools;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StubAiClientTest {

    @Mock
    private OrderAiTools orderAiTools;

    @Mock
    private StoreAiTools storeAiTools;

    @Mock
    private ProductAiTools productAiTools;

    @InjectMocks
    private StubAiClient stubAiClient;

    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .name("Bob")
                .email("bob@nexora.test")
                .roles(Set.of())
                .build();
        testUser = new UserPrincipal(user);
    }

    @Test
    @DisplayName("Routes order status query and executes getOrderStatus tool")
    void chat_OrderStatusIntent() {
        when(orderAiTools.getOrderStatus(eq(42L), eq(testUser)))
                .thenReturn("Order #42 is OUT_FOR_DELIVERY");

        List<String> tools = new ArrayList<>();
        String reply = stubAiClient.chat("Where is my order #42?", List.of(), testUser, tools);

        assertThat(tools).contains("getOrderStatus");
        assertThat(reply).contains("Order #42 is OUT_FOR_DELIVERY");
        verify(orderAiTools).getOrderStatus(42L, testUser);
    }

    @Test
    @DisplayName("Routes open stores query and executes getAvailableStores tool")
    void chat_StoreDiscoveryIntent() {
        when(storeAiTools.getAvailableStores(eq("Pizza")))
                .thenReturn("Open Stores:\n- Pizza Palace");

        List<String> tools = new ArrayList<>();
        String reply = stubAiClient.chat("Which pizza stores are currently open?", List.of(), testUser, tools);

        assertThat(tools).contains("getAvailableStores");
        assertThat(reply).contains("Pizza Palace");
        verify(storeAiTools).getAvailableStores("Pizza");
    }

    @Test
    @DisplayName("Routes food discovery query with budget and executes searchProducts tool")
    void chat_FoodSearchIntent() {
        when(productAiTools.searchProducts(anyString(), any(), eq(250.0)))
                .thenReturn("Found: Farmhouse Pizza ₹220");

        List<String> tools = new ArrayList<>();
        String reply = stubAiClient.chat("Find pizza under 250", List.of(), testUser, tools);

        assertThat(tools).contains("searchProducts");
        assertThat(reply).contains("Farmhouse Pizza");
        verify(productAiTools).searchProducts(eq("pizza"), eq("Pizza"), eq(250.0));
    }

    @Test
    @DisplayName("Greets user warmly with user's name")
    void chat_GreetingIntent() {
        List<String> tools = new ArrayList<>();
        String reply = stubAiClient.chat("Hi there!", List.of(), testUser, tools);

        assertThat(tools).isEmpty();
        assertThat(reply).contains("Hello Bob!");
        assertThat(reply).contains("Nexora AI Assistant");
    }
}
