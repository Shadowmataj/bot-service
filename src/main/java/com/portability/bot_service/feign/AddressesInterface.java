package com.portability.bot_service.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.portability.bot_service.model.dto.AddressRequest;
import com.portability.bot_service.model.dto.AddressResponse;



@FeignClient(name = "ADDRESSES-SERVICE")
@Component
public interface AddressesInterface {


    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(
            @PathVariable Long id);

    @PostMapping("api/addresses")
    public ResponseEntity<AddressResponse> createAddress(
        @RequestBody AddressRequest addressRequest); 

    
    @PutMapping("api/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
        @PathVariable Long id,
        @RequestBody AddressRequest addressRequest);
    

    @GetMapping("api/addresses/customer/{customerId}")
    public ResponseEntity<List<AddressResponse>> getAddressByCustomerId(
            @PathVariable Long customerId);
}
