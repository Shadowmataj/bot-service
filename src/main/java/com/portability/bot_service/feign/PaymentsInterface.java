package com.portability.bot_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.portability.bot_service.model.dto.CheckoutSessionRequest;
import com.portability.bot_service.model.dto.CheckoutSessionResponse;

@FeignClient(name = "PAYMENTS-SERVICE")
@Component
public interface PaymentsInterface {

    @PostMapping("/api/checkout-sessions")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
        @RequestBody CheckoutSessionRequest request);
}
