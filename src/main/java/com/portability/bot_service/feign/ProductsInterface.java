package com.portability.bot_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.portability.bot_service.model.dto.SimCardResponse;

@FeignClient(name = "PRODUCTS-SERVICE")
@Component
public interface ProductsInterface {
    @GetMapping("api/simcards/{id}")
    public ResponseEntity<SimCardResponse> getSimCardById(
            @PathVariable Long id);
}
