package com.portability.bot_service.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.portability.bot_service.model.dto.ByPhoneNumberRequest;
import com.portability.bot_service.model.dto.Imei;
import com.portability.bot_service.model.dto.OrderRequest;
import com.portability.bot_service.model.dto.OrderResponse;
import com.portability.bot_service.model.dto.PortabilityNip;
import com.portability.bot_service.model.dto.PortabilityRequest;
import com.portability.bot_service.model.dto.PortabilityResponse;

import io.swagger.v3.oas.annotations.Parameter;

@FeignClient(name = "PORTABILITIES-SERVICE")
@Component
public interface PortabilitiesInterface {

        @PostMapping("api/orders")
        public ResponseEntity<OrderResponse> createOrder(
                        @RequestBody OrderRequest request);

        @GetMapping("api/orders/customer/{customerId}")
        public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(
                        @Parameter(description = "Customer ID", required = true) @PathVariable Long customerId);

        @GetMapping("api/orders/{id}")
        public ResponseEntity<OrderResponse> getOrderById(
                        @PathVariable String id);

        @PatchMapping("api/orders/{id}/checkout/{checkoutId}")
        public ResponseEntity<OrderResponse> updateCheckoutId(
                        @PathVariable String id,
                        @PathVariable Long checkoutId);

        @PostMapping("api/portabilities")

        public ResponseEntity<PortabilityResponse> createPortability(
                        @RequestBody PortabilityRequest request);

        @PostMapping("api/portabilities/by-phone")
        public ResponseEntity<PortabilityResponse> getPortabilityByPhoneNumber(
                        @RequestBody ByPhoneNumberRequest phoneNumber);

        @PatchMapping("api/portabilities/{id}/imei")
        public ResponseEntity<PortabilityResponse> updateImei(
                        @PathVariable Long id,
                        @RequestBody Imei imei);

        @PatchMapping("api/portabilities/{id}/nip")
        public ResponseEntity<PortabilityResponse> updateNip(
                        @PathVariable Long id,
                        @RequestBody PortabilityNip nip);

}
