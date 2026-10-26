package com.nexora.pricing.repository;

import com.nexora.pricing.entity.SurgePricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurgePricingRepository extends JpaRepository<SurgePricingRule, Long> {
    Optional<SurgePricingRule> findByHubId(Long hubId);
    List<SurgePricingRule> findAllByOrderByHubIdAsc();
}
