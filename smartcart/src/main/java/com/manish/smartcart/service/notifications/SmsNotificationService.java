package com.manish.smartcart.service.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub service representing integration with an SMS provider (Twilio, AWS SNS, Msg91).
 * In a real production environment, this would call the provider's REST API.
 */

@Slf4j
@Service
public class SmsNotificationService {

    public void sendSms(String phoneNumber, String message) {
        if(phoneNumber == null || phoneNumber.isEmpty()){
            log.warn("Cannot send SMS : Phone number is missing.");
            return;
        }
        // Mocking the external API call
        log.info("📱 [MOCK TWILIO SMS] To: {} | Message: '{}'", phoneNumber, message);
    }
}
