package com.portability.bot_service.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.portability.bot_service.exception.ToolExecutionException;
import com.portability.bot_service.feign.UsersInterface;
import com.portability.bot_service.model.dto.ByEmailRequest;
import com.portability.bot_service.model.dto.ByPhoneNumberRequest;
import com.portability.bot_service.model.dto.CustomerRequest;
import com.portability.bot_service.model.dto.CustomerResponse;

import feign.FeignException;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class CustomerTools {

    private final UsersInterface usersInterface;

    @Tool(
            name = "registerCustomer",
            description = """
                Registers a new customer in the user system with this data 
                String firstName ,
                String lastName ,
                String email ,
                String phoneNumber

                returns CustomerResponse with the registered customer details:
                - id: The unique ID of the registered customer
                - firstName: The first name of the customer
                - lastName: The last name of the customer
                - email: The email address of the customer
                - phoneNumber: The phone number of the customer
            """
    )
    public CustomerResponse registerCustomer(
            @ToolParam CustomerRequest request) {
        try {
            ResponseEntity<CustomerResponse> response = usersInterface.register(request);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "registerCustomer",
                        "No se pudo registrar el cliente. Verifica que el email o teléfono no estén ya registrados",
                        "HTTP Status: " + response.getStatusCode()
                );
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "registerCustomer",
                    "El servicio de registro no está disponible en este momento",
                    e
            );
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "registerCustomer",
                    "Ocurrió un error al registrar el cliente",
                    e
            );
        }
    }

    @Tool(
            name = "getCustomerById",
            description = "Get a customer information by their id"
    )
    public CustomerResponse getCustomerById(@ToolParam Long customerId) {
        try {
            ResponseEntity<CustomerResponse> response = usersInterface.getCustomerById(customerId);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "getCustomerById",
                        "No se encontró un cliente con ese ID",
                        "HTTP Status: " + response.getStatusCode()
                );
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "getCustomerById",
                    "El servicio de clientes no está disponible en este momento",
                    e
            );
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "getCustomerById",
                    "Ocurrió un error al buscar el cliente",
                    e
            );
        }
    }

    @Tool(
            name = "getCustomerByEmail",
            description = "You can use this tool to get a customer information with their email"
    )
    public CustomerResponse getCustomerByEmail(@ToolParam ByEmailRequest email) {
        try {
            ResponseEntity<CustomerResponse> response = usersInterface.getCustomerByEmail(email);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "getCustomerByEmail",
                        "No se encontró un cliente con ese email",
                        "HTTP Status: " + response.getStatusCode()
                );
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "getCustomerByEmail",
                    "El servicio de clientes no está disponible en este momento",
                    e
            );
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "getCustomerByEmail",
                    "Ocurrió un error al buscar el cliente por email",
                    e
            );
        }
    }

    @Tool(
            name = "getCustomerByPhoneNumber",
            description = """
                You can use this tool to get a customer information with their phone number 
                to know if they are registered instead of asking if they are registered or not.
                
                Returns:
                - CustomerResponse: If the customer is found (registered)
                - Throws exception with "No se encontró un cliente con ese número de teléfono": If customer is NOT registered (404)
                - Throws exception with "El servicio de clientes no está disponible": If there's a service error (500, 503, etc.)
                """
    )
    public CustomerResponse getCustomerByPhoneNumber(@ToolParam ByPhoneNumberRequest phoneNumber) {
        try {
            ResponseEntity<CustomerResponse> response = usersInterface.getCustomerByPhoneNumber(phoneNumber);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "getCustomerByPhoneNumber",
                        "No se encontró un cliente con ese número de teléfono",
                        "HTTP Status: " + response.getStatusCode()
                );
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            // 404 - Cliente no registrado (esto es esperado y normal)
            throw new ToolExecutionException(
                    "getCustomerByPhoneNumber",
                    "No se encontró un cliente con ese número de teléfono",
                    "Customer not registered - HTTP 404"
            );
        } catch (FeignException e) {
            // Otros errores de servicio (500, 503, etc.)
            throw new ToolExecutionException(
                    "getCustomerByPhoneNumber",
                    "El servicio de clientes no está disponible en este momento. Por favor intenta nuevamente.",
                    e
            );
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "getCustomerByPhoneNumber",
                    "Ocurrió un error inesperado al buscar el cliente por teléfono",
                    e
            );
        }
    }

}
