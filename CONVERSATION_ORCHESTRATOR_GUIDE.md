# Sistema de Orquestación de Conversaciones con State Machine

## Cambios Implementados

### 1. Estados de Conversación Actualizados

Se actualizó `ConversationState.java` con los estados basados en el flujo del chatbot:

```java
INITIAL                     -> Inicio de la conversación
CUSTOMER_REGISTRATION       -> Registro del cliente
INTENT_SELECTION           -> Selección de intención (comprar/portabilidad/rastrear)
PRODUCT_SELECTED           -> Producto seleccionado
IMEI_REQUIRED              -> Solicitar IMEI
IMEI_VALIDATED             -> IMEI validado y compatible
ADDRESS_REQUIRED           -> Solicitar dirección de envío
PAYMENT_PENDING            -> Link de pago enviado
PAYMENT_CONFIRMED          -> Pago confirmado
SIM_SHIPPED                -> SIM enviado al cliente
PORTABILITY_WAIT_SIM       -> Esperando recepción de SIM para portabilidad
PORTABILITY_NIP_REQUIRED   -> Solicitar NIP de portabilidad
PORTABILITY_SIM_ACTIVATION -> Activación de SIM
PORTABILITY_IN_PROGRESS    -> Proceso de portabilidad en curso
PORTABILITY_COMPLETED      -> Portabilidad completada
COMPLETED                  -> Flujo completado
BLOCKED                    -> Flujo bloqueado (ej: IMEI incompatible)
ABANDONED                  -> Conversación abandonada
ERROR_STATE                -> Estado de error
```

### 2. Mapa de Transiciones Implementado

En `ConversationStateService.java`, se implementó el método `isValidTransition()` con las transiciones permitidas según el flujo:

```
INITIAL → CUSTOMER_REGISTRATION → INTENT_SELECTION → PRODUCT_SELECTED
    ↓
IMEI_REQUIRED → IMEI_VALIDATED → ADDRESS_REQUIRED
    ↓
PAYMENT_PENDING → PAYMENT_CONFIRMED → SIM_SHIPPED
    ↓
[Sin Portabilidad]           [Con Portabilidad]
    ↓                              ↓
COMPLETED                    PORTABILITY_WAIT_SIM
                                  ↓
                            PORTABILITY_NIP_REQUIRED
                                  ↓
                            PORTABILITY_SIM_ACTIVATION
                                  ↓
                            PORTABILITY_IN_PROGRESS
                                  ↓
                            PORTABILITY_COMPLETED
                                  ↓
                            COMPLETED
```

### 3. ChatOrchestratorService - Gestor de Tool Calls

**Archivo:** `ChatOrchestratorService.java`

Servicio principal que maneja el flujo de conversación con control de herramientas:

#### Características:
- **Loop de Tool Calls:** Máximo 3 iteraciones para evitar loops infinitos
- **Integración RAG:** Búsqueda semántica en vector store
- **Gestión de Estados:** Transiciones automáticas basadas en contenido
- **Manejo de Errores:** ToolExecutionException integrado
- **Memoria Persistente:** Usa PostgresChatMemory

#### Flujo de Ejecución:

```java
handleMessage(userMessage, phoneNumber)
    ↓
1. Obtener estado actual de conversación
    ↓
2. Construir prompt con RAG context + estado
    ↓
3. Enviar a ChatClient con tools
    ↓
4. Loop (máximo 5 iteraciones):
   - Si hay tool calls:
     - Extraer resultados
     - Enviar resultados de vuelta al ChatClient
     - Obtener nueva respuesta
   - Sino: salir del loop
    ↓
5. Actualizar estado basado en respuesta
    ↓
6. Retornar respuesta final
```

#### Métodos Principales:

```java
// Punto de entrada principal
String handleMessage(String userMessage, String phoneNumber)

// Procesa mensaje con loop de tools
String processWithToolLoop(String systemPrompt, String userMessage, String phoneNumber)

// Construye prompt con RAG
String buildSystemPrompt(String phoneNumber, String userQuery, ConversationState currentState)

// Búsqueda semántica
String fetchSemanticContext(String userQuery)

// Determina siguiente estado
ConversationState determineNextState(ConversationState current, String user, String assistant)
```

### 4. ChatService Simplificado

**Archivo:** `ChatService.java`

Ahora es un wrapper simple que delega al orquestador:

```java
@Service
public class ChatService {
    @Autowired
    private ChatOrchestratorService orchestrator;

    public String getBotResponse(String message, String phoneNumber) {
        return orchestrator.handleMessage(message, phoneNumber);
    }
}
```

**Ventajas:**
- ✅ Mantiene compatibilidad con API existente
- ✅ Separación de responsabilidades
- ✅ Fácil testing y mantenimiento

## Ejemplo de Uso

### Flujo Completo de Compra con Portabilidad:

```
Usuario: "Hola, quiero comprar una SIM con portabilidad"
Estado: INITIAL → INTENT_SELECTION → PRODUCT_SELECTED

Usuario: "Mi IMEI es 123456789012345"
Estado: PRODUCT_SELECTED → IMEI_REQUIRED → IMEI_VALIDATED
Tool: scrapeImeiCompatibility(imei)

Usuario: "Mi dirección es Calle Falsa 123"
Estado: IMEI_VALIDATED → ADDRESS_REQUIRED
Tool: createAddress(addressRequest)

Usuario: "Proceder al pago"
Estado: ADDRESS_REQUIRED → PAYMENT_PENDING
Tool: createCheckoutSession(request)
Respuesta: "Link de pago: https://checkout.stripe.com/xyz"

[Cliente paga]
Estado: PAYMENT_PENDING → PAYMENT_CONFIRMED → SIM_SHIPPED

[Cliente recibe SIM]
Usuario: "Ya recibí la SIM"
Estado: SIM_SHIPPED → PORTABILITY_WAIT_SIM

Usuario: "Mi NIP es 1234"
Estado: PORTABILITY_WAIT_SIM → PORTABILITY_NIP_REQUIRED
Tool: updatePortabilityNip(id, nip)

Usuario: "Ya activé la SIM"
Estado: PORTABILITY_NIP_REQUIRED → PORTABILITY_SIM_ACTIVATION → PORTABILITY_IN_PROGRESS

[Sistema ejecuta portabilidad]
Estado: PORTABILITY_IN_PROGRESS → PORTABILITY_COMPLETED → COMPLETED
```

## Limitaciones y Control

### Control de Iteraciones de Tools:

```java
private static final int MAX_TOOL_ITERATIONS = 5;
```

Evita loops infinitos si las tools siguen generando llamadas.

### Ventajas del Límite:
1. **Prevención de costos:** Evita consumo excesivo de tokens
2. **Timeouts:** Evita respuestas muy lentas
3. **Debugging:** Facilita identificar problemas de loop
4. **UX:** Respuestas más rápidas

### Monitoreo:

```java
logger.warn("Reached maximum tool iterations ({}) for conversation {}", 
    MAX_TOOL_ITERATIONS, phoneNumber);
```

## Transiciones Automáticas de Estado

El orquestador detecta palabras clave para transiciones:

```java
case PRODUCT_SELECTED -> {
    if (lowerAssistant.contains("imei")) {
        yield ConversationState.IMEI_REQUIRED;
    } else if (lowerAssistant.contains("dirección")) {
        yield ConversationState.ADDRESS_REQUIRED;
    }
    yield null;
}
```

## Validación de Transiciones

El `ConversationStateService` valida que las transiciones sean válidas:

```java
case PRODUCT_SELECTED -> 
    to == ConversationState.IMEI_REQUIRED ||
    to == ConversationState.ADDRESS_REQUIRED;
```

Si una transición no es válida, se registra un warning y se rechaza.

## Integración con Tools

Las tools se ejecutan automáticamente cuando el LLM decide usarlas:

```java
.tools(customerTools, orderTools, paymentTools, scraperTools, addressesTools)
```

Resultados se procesan en el loop:

```java
while (hasToolCalls(response) && iteration < MAX_TOOL_ITERATIONS) {
    String toolResults = extractToolResults(response);
    // Enviar resultados de vuelta al ChatClient
}
```

## Manejo de Errores

### ToolExecutionException:
```java
catch (ToolExecutionException e) {
    logger.error("Tool failed: {}", e.getToolName());
    stateService.transitionTo(phoneNumber, ConversationState.ERROR_STATE);
    return e.getMessageForLLM();
}
```

### Errores Generales:
```java
catch (Exception e) {
    logger.error("Unexpected error", e);
    stateService.transitionTo(phoneNumber, ConversationState.ERROR_STATE);
    return "Lo siento, ocurrió un error inesperado...";
}
```

## Configuración Necesaria

### application.properties

Ya configurado previamente:
```properties
# PostgreSQL para memoria persistente
spring.datasource.url=jdbc:postgresql://localhost:5433/bot_db
spring.jpa.hibernate.ddl-auto=update

# OpenAI para ChatClient
spring.ai.openai.api-key=sk-...
spring.ai.openai.chat.options.model=gpt-4o
```

## Testing del Sistema

### Probar Estado Inicial:
```bash
curl -X POST http://localhost:8090/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hola", "phoneNumber": "1234567890"}'
```

### Verificar Estado:
```bash
curl http://localhost:8090/api/conversations/1234567890/state
```

### Ver Contexto:
```bash
curl http://localhost:8090/api/conversations/1234567890/context
```

## Logs Importantes

```
INFO  ChatOrchestratorService - Processing message for user 1234567890 in state: INITIAL
INFO  ChatOrchestratorService - Tool execution iteration 1 for conversation 1234567890
INFO  ConversationStateService - State transition for 1234567890: INITIAL -> INTENT_SELECTION
```

## Próximos Pasos

### Para State Machine Completa:

1. **Agregar Spring State Machine:**
```xml
<dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-core</artifactId>
</dependency>
```

2. **Definir Eventos:**
```java
public enum ConversationEvent {
    USER_REGISTERED,
    PRODUCT_SELECTED,
    PAYMENT_COMPLETED,
    // ...
}
```

3. **Configurar State Machine:**
```java
@Configuration
@EnableStateMachine
public class ConversationStateMachineConfig {
    // Configuración de estados y transiciones
}
```

## Ventajas del Sistema Actual

✅ **Control de Loop:** Máximo 5 iteraciones evita problemas  
✅ **Estados Claros:** Basados en el flujo real del negocio  
✅ **Validación:** Transiciones validadas automáticamente  
✅ **Persistencia:** Todo guardado en PostgreSQL  
✅ **Manejo de Errores:** Integrado en todos los niveles  
✅ **RAG Integrado:** Contexto semántico automático  
✅ **Logging:** Trazabilidad completa  
✅ **Extensible:** Fácil agregar nuevos estados/tools  

## Diferencias con el Ejemplo Original

### Ejemplo Original:
```java
while (response.getResult().getOutput().hasToolCall()) {
    ToolCall toolCall = response.getResult().getOutput().getToolCall();
    Object result = toolExecutor.execute(toolCall);
    // ...
}
```

### Nuestra Implementación:
```java
while (hasToolCalls(response) && iteration < MAX_TOOL_ITERATIONS) {
    iteration++;
    String toolResults = extractToolResults(response);
    response = chatClient.prompt()
        .system(systemPrompt)
        .user("Resultado de las herramientas: " + toolResults)
        .advisors(...)
        .tools(...)
        .call()
        .chatResponse();
}
```

**Mejoras:**
- ✅ Límite de iteraciones
- ✅ Logging de cada iteración
- ✅ Manejo de múltiples tool calls
- ✅ Integración con memoria persistente
- ✅ Gestión de estados automática
