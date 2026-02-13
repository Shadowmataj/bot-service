package com.portability.bot_service.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.portability.bot_service.exception.ToolExecutionException;
import com.portability.bot_service.feign.PortabilitiesInterface;
import com.portability.bot_service.feign.ProductsInterface;
import com.portability.bot_service.model.dto.ByPhoneNumberRequest;
import com.portability.bot_service.model.dto.Imei;
import com.portability.bot_service.model.dto.OrderRequest;
import com.portability.bot_service.model.dto.OrderResponse;
import com.portability.bot_service.model.dto.PortabilityNip;
import com.portability.bot_service.model.dto.PortabilityRequest;
import com.portability.bot_service.model.dto.PortabilityResponse;
import com.portability.bot_service.model.dto.SimCardResponse;

import feign.FeignException;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class OrderTools {

    private final PortabilitiesInterface portabilitiesInterface;
    private final ProductsInterface productsInterface;

    @Tool(name = "createNewOrderForSimCardPurchase", description = """
                Creates a new order with customer id
                OrderRequest parameters:
                    Long customerId: REQUIRED - Customer ID from registerCustomer or getCustomerByPhoneNumber tool. NEVER use a hardcoded value like 1.
                    Long productId: REQUIRED - Product ID from the products list
                    Long addressId: REQUIRED - Address ID from createAddress tool
                    Long checkoutId: MUST BE NULL - The checkout session is created AFTER the order, so always pass null here

                IMPORTANT: The order must be created BEFORE the checkout session. After creating the order, use the returned order ID to create the checkout session.
            """)
    public OrderResponse createNewOrderForSimCardPurchase(OrderRequest request, Boolean needsPortability) {
        try {
            ResponseEntity<OrderResponse> response = portabilitiesInterface.createOrder(request);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "createNewOrderForSimCardPurchase",
                        "No se pudo crear la orden. Verifica que todos los datos sean correctos",
                        "HTTP Status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "createNewOrderForSimCardPurchase",
                    "El servicio de órdenes no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "createNewOrderForSimCardPurchase",
                    "Ocurrió un error al crear la orden",
                    e);
        }
    }

    @Tool(name = "createOrderForSimCardPortabilityPurchase", description = """
            In case the selected product needs a portability it creates a new order and a portability for that order.
            OrderRequest parameters:
                Long customerId: REQUIRED - Customer ID from registerCustomer or getCustomerByPhoneNumber tool. NEVER use a hardcoded value like 1.
                Long productId: REQUIRED - Product ID from the products list
                Long addressId: REQUIRED - Address ID from createAddress tool
                Long checkoutId: MUST BE NULL - The checkout session is created AFTER the order, so always pass null here
            String phoneNumber: REQUIRED - The phone number for portability (must be between 10 and 15 digits)

            IMPORTANT: The order must be created BEFORE the checkout session. After creating the order, use the returned order ID to create the checkout session.
            """)
    public OrderResponse createOrderForSimCardPortabilityPurchase(
            @ToolParam OrderRequest orderRequest,
            @ToolParam String phoneNumber) {

        try {
            if (phoneNumber == null || phoneNumber.length() < 10 || phoneNumber.length() > 15) {
                throw new ToolExecutionException(
                        "createOrderForSimCardPortabilityPurchase",
                        "El número de teléfono debe tener entre 10 y 15 dígitos",
                        "Invalid phone number length: " + (phoneNumber != null ? phoneNumber.length() : "null"));
            }

            ResponseEntity<OrderResponse> response = portabilitiesInterface.createOrder(orderRequest);

            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "createOrderForSimCardPortabilityPurchase",
                        "No se pudo crear la orden. Verifica que todos los datos sean correctos",
                        "Order creation failed - HTTP Status: " + response.getStatusCode());
            }

            OrderResponse orderResponse = response.getBody();

            PortabilityRequest portabilityRequest = new PortabilityRequest(phoneNumber, orderResponse.id());
            ResponseEntity<PortabilityResponse> portabilityResponse = portabilitiesInterface
                    .createPortability(portabilityRequest);

            if (portabilityResponse.getStatusCode().isError() || portabilityResponse.getBody() == null) {
                throw new ToolExecutionException(
                        "createOrderForSimCardPortabilityPurchase",
                        "La orden se creó pero no se pudo procesar la portabilidad. El número podría estar ya en proceso de portabilidad",
                        "Portability creation failed - HTTP Status: " + portabilityResponse.getStatusCode());
            }

            return orderResponse;

        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "createOrderForSimCardPortabilityPurchase",
                    "El servicio de portabilidad no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "createOrderForSimCardPortabilityPurchase",
                    "Ocurrió un error al crear la orden con portabilidad",
                    e);
        }
    }

    @Tool(name = "getOrdersByOrderId", description = "Get orders information by order id (UUID string format)")
    public OrderResponse getOrderById(
            @ToolParam String id) {
        try {
            ResponseEntity<OrderResponse> response = portabilitiesInterface.getOrderById(id);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "getOrdersByOrderId",
                        "No se encontró una orden con ese ID",
                        "HTTP Status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "getOrdersByOrderId",
                    "El servicio de órdenes no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "getOrdersByOrderId",
                    "Ocurrió un error al buscar la orden",
                    e);
        }
    }

    @Tool(name = "getOrdersByCustomerId", description = "Get orders information by customer id")
    public List<OrderResponse> getOrdersByCustomerId(
            @ToolParam Long customerId) {
        try {
            ResponseEntity<List<OrderResponse>> response = portabilitiesInterface.getOrdersByCustomerId(customerId);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "getOrdersByCustomerId",
                        "No se encontraron órdenes para ese cliente",
                        "HTTP Status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "getOrdersByCustomerId",
                    "El servicio de órdenes no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "getOrdersByCustomerId",
                    "Ocurrió un error al buscar las órdenes del cliente",
                    e);
        }
    }

    @Tool(name = "getPortabilityByPhoneNumber", description = "Get portability information by phone number, including customer ID, portability ID, checkout session id and other details, in")
    public PortabilityResponse getPortabilityByPhoneNumber(
            @ToolParam ByPhoneNumberRequest phoneNumber) {
        try {
            ResponseEntity<PortabilityResponse> response = portabilitiesInterface
                    .getPortabilityByPhoneNumber(phoneNumber);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "getPortabilityByPhoneNumber",
                        "No se encontró información de portabilidad para ese número",
                        "HTTP Status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "getPortabilityByPhoneNumber",
                    "El servicio de portabilidad no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "getPortabilityByPhoneNumber",
                    "Ocurrió un error al buscar la portabilidad",
                    e);
        }
    }

    @Tool(name = "getSimIcc", description = """
            Get the SIM ICC (Integrated Circuit Card Identifier) number for a specific portability request.
            This tool requires the order ID to retrieve the associated SIM card information, including the ICC number, which is essential for the portability process.

            Parameters:
            - id: String. The unique identifier of the order associated with the portability request. This ID is used to fetch the order details and subsequently retrieve the SIM card information, including the ICC number.
            """)
    public SimCardResponse getSimIcc(
            @ToolParam String id) {
        try {
            ResponseEntity<OrderResponse> response = portabilitiesInterface.getOrderById(id);
            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "getOrdersByOrderId",
                        "No se encontró una orden con ese ID",
                        "HTTP Status: " + response.getStatusCode());
            }

            ResponseEntity<SimCardResponse> simCard = productsInterface.getSimCardById(response.getBody().simId());
            if (simCard == null) {
                throw new ToolExecutionException(
                        "getSimIcc",
                        "No se encontró una tarjeta SIM con ese ID",
                        "SIM Card ID: " + response.getBody().simId());
            }

            return simCard.getBody();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "getSimIcc",
                    "El servicio de portabilidad no está disponible en este momento",
                    e);
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "getSimIcc",
                    "Ocurrió un error al obtener el ICC de la SIM",
                    e);
        }
    }

}
