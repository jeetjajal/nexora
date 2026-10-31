package com.nexora.whatsapp.repository;

import com.nexora.whatsapp.entity.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {

    List<WhatsAppMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
