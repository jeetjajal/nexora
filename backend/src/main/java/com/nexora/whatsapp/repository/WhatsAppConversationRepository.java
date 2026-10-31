package com.nexora.whatsapp.repository;

import com.nexora.whatsapp.entity.WhatsAppConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppConversationRepository extends JpaRepository<WhatsAppConversation, Long> {

    Optional<WhatsAppConversation> findByWaPhoneNumber(String waPhoneNumber);

    List<WhatsAppConversation> findAllByOrderByLastActiveAtDesc();
}
