package com.portability.bot_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.portability.bot_service.model.dto.ByEmailRequest;
import com.portability.bot_service.model.dto.ByPhoneNumberRequest;
import com.portability.bot_service.model.dto.CustomerRequest;
import com.portability.bot_service.model.dto.CustomerResponse;

import io.swagger.v3.oas.annotations.Parameter;

@FeignClient(name = "USERS-SERVICE")
@Component
public interface UsersInterface {

    @PostMapping("/api/customer/register")
    public ResponseEntity<CustomerResponse> register(
            @RequestBody CustomerRequest customerRequest);

    @GetMapping("api/customers/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @Parameter(description = "Customer ID", example = "1")
            @PathVariable Long id);

    @PostMapping("api/customers/by-email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(
            @RequestBody ByEmailRequest email);

    @PostMapping("api/customers/by-phone")
    public ResponseEntity<CustomerResponse> getCustomerByPhoneNumber(
            @RequestBody ByPhoneNumberRequest phoneNumber);

}
