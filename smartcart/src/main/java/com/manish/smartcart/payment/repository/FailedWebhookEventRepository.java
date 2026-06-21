package com.manish.smartcart.payment.repository;

import com.manish.smartcart.shared.enums.DlqStatus;
import com.manish.smartcart.payment.model.FailedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedWebhookEventRepository extends JpaRepository<FailedWebhookEvent, Long> {
    // An Admin will use this to find all webhooks that are currently broken
    List<FailedWebhookEvent> findByStatusOrderByCreatedAtDesc(DlqStatus status);
}
