package com.manish.smartcart.repository;

import com.manish.smartcart.enums.DlqStatus;
import com.manish.smartcart.model.admin.FailedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedWebhookEventRepository extends JpaRepository<FailedWebhookEvent, Long> {
    // An Admin will use this to find all webhooks that are currently broken
    List<FailedWebhookEvent> findByStatusOrderByCreatedAtDesc(DlqStatus status);
}
