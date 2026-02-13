package com.portability.bot_service.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.portability.bot_service.exception.ToolExecutionException;
import com.portability.bot_service.feign.AddressesInterface;
import com.portability.bot_service.model.dto.AddressBotRequest;
import com.portability.bot_service.model.dto.AddressRequest;
import com.portability.bot_service.model.dto.AddressResponse;
import com.portability.bot_service.model.enm.AddressType;

import feign.FeignException;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class AddressesTools {

    private final AddressesInterface addressesInterface;

    @Tool(
            name = "createAddress",
            description = """
                Registers a new customer address in the address system with this data
                AddressBotRequest parameters:
                    Long customerId: Customer ID get it from createCustomer tool
                    String street: Street cannot be empty and max 200 characters
                    String district: District max 100 characters 
                    String number: Street number of the address
                    String postalCode: Postal code cannot be empty and max 20 characters
                    String reference: Additional information max 45 characters
            """
    )
    public AddressResponse createAddress(
            @ToolParam AddressBotRequest request) {
        try {
            AddressRequest addressRequest = new AddressRequest(
                    request.getCustomerId(),
                    request.getStreet(),
                    request.getDistrict(),
                    request.getNumber(),
                    request.getPostalCode(),
                    request.getReference(),
                    AddressType.CLIENT // AddressType is not provided in AddressBotRequest
            );

            ResponseEntity<AddressResponse> response = addressesInterface.createAddress(addressRequest);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "createAddress",
                        "No se pudo registrar la dirección. Verifica que todos los datos sean correctos",
                        "HTTP Status: " + response.getStatusCode()
                );
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "createAddress",
                    "El servicio de direcciones no está disponible en este momento",
                    e
            );
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "createAddress",
                    "Ocurrió un error al registrar la dirección",
                    e
            );
        }
    }
}
