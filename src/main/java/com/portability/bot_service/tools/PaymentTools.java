package com.portability.bot_service.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.portability.bot_service.exception.ToolExecutionException;
import com.portability.bot_service.feign.PaymentsInterface;
import com.portability.bot_service.model.dto.CheckoutSessionBotRequest;
import com.portability.bot_service.model.dto.CheckoutSessionRequest;
import com.portability.bot_service.model.dto.CheckoutSessionResponse;

import feign.FeignException;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class PaymentTools {

    private final PaymentsInterface paymentsInterface;

    @Tool(
            name = "Create_checkout_session",
            description = """
            Create a Stripe checkout session for processing payments.
            This tool requires a CheckoutSessionBotRequest object with three fields:
            Long payment_link_id: The payment link ID from the Payments service use the one for the product being purchased
            Long customer_id: CRITICAL - Must be the SAME customer ID get from creating the user.
            String order_id: The order ID (String/UUID format) returned by createNewOrderForSimCardPurchase or createOrderForSimCardPortabilityPurchase tool
            
            Returns a CheckoutSessionResponse containing:
            - message: Status message about the session creation
            - stripe_session_url: The URL where the customer should be redirected to complete payment
            - checkout_session_id: The checkout session ID that can be used to update the order
            
            IMPORTANT NOTES:
            - This tool must be called AFTER creating the order
            - The customer_id MUST match the customer ID used in the order creation
            - The order_id comes from the order creation response
            - Save the checkout_session_id returned by this tool
            - The checkout session URL must be provided to the customer to complete their purchase
            """
    )
    public CheckoutSessionResponse createCheckoutSession(
            @ToolParam CheckoutSessionBotRequest request) {
        try {
            System.out.println("Creating checkout session with request: " + request);

            if (request == null) {
                throw new ToolExecutionException(
                        "Create_checkout_session",
                        "La solicitud de pago no puede estar vacía",
                        "CheckoutSessionBotRequest is null"
                );
            }
            if (request.payment_link_id() == null || request.payment_link_id() <= 0) {
                throw new ToolExecutionException(
                        "Create_checkout_session",
                        "Se requiere un ID de enlace de pago válido",
                        "payment_link_id is invalid: " + request.payment_link_id()
                );
            }
            if (request.customer_id() == null) {
                throw new ToolExecutionException(
                        "Create_checkout_session",
                        "Se requiere el ID del cliente para crear la sesión de pago",
                        "customer_id is null"
                );
            }
            if (request.order_id() == null) {
                throw new ToolExecutionException(
                        "Create_checkout_session",
                        "Se requiere el ID de la orden para crear la sesión de pago",
                        "order_id is null"
                );
            }

            CheckoutSessionRequest dtoRequest = new CheckoutSessionRequest(
                    request.payment_link_id(),
                    request.customer_id(),
                    request.order_id(),
                    "https://example.com/success",
                    "https://example.com/cancel"
            );

            ResponseEntity<CheckoutSessionResponse> response = paymentsInterface.createCheckoutSession(dtoRequest);

            if (response.getStatusCode().isError() || response.getBody() == null) {
                throw new ToolExecutionException(
                        "Create_checkout_session",
                        "No se pudo crear la sesión de pago. Verifica que los datos sean correctos",
                        "HTTP Status: " + response.getStatusCode()
                );
            }

            return response.getBody();

        } catch (ToolExecutionException e) {
            throw e;
        } catch (FeignException e) {
            throw new ToolExecutionException(
                    "Create_checkout_session",
                    "El servicio de pagos no está disponible en este momento",
                    e
            );
        } catch (Exception e) {
            throw new ToolExecutionException(
                    "Create_checkout_session",
                    "Ocurrió un error al crear la sesión de pago",
                    e
            );
        }
    }
}
