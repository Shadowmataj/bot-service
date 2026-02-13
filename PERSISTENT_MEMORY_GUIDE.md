# Sistema de Memoria Persistente con PostgreSQL y Gestión de Estados

## Descripción General

Este sistema implementa memoria persistente para el ChatClient usando PostgreSQL, permitiendo:

1. **Almacenamiento permanente** de conversaciones entre sesiones
2. **Gestión de estados** para controlar el flujo de la conversación
3. **Contexto persistente** para datos de la conversación
4. **Base para State Machine** futura para control avanzado del flujo

## Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                        ChatService                          │
│  (Orquesta la conversación y actualiza estados)            │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│                 PostgresChatMemory                          │
│  (Implementación de ChatMemory con PostgreSQL)             │
└───────────┬─────────────────────────────────────────────────┘
            │
            ├──────────────────┬──────────────────┐
            ▼                  ▼                  ▼
┌─────────────────┐  ┌──────────────────┐  ┌───────────────────┐
│   Conversation  │  │   Chat Message   │  │  Conversation     │
│   Repository    │  │   Repository     │  │  State Service    │
└────────┬────────┘  └────────┬─────────┘  └─────────┬─────────┘
         │                    │                       │
         └────────────────────┴───────────────────────┘
                              │
                              ▼
                   ┌──────────────────────┐
                   │   PostgreSQL DB      │
                   │  - chat_conversations│
                   │  - chat_messages     │
                   └──────────────────────┘
```

## Componentes Principales

### 1. Entidades JPA

#### ChatConversation
- Almacena metadata de la conversación
- Estados actuales (ConversationState)
- Contexto en formato JSONB
- Timestamps de creación y actualización

```java
@Entity
@Table(name = "chat_conversations")
public class ChatConversation {
    private Long id;
    private String conversationId;
    private String phoneNumber;
    private ConversationState currentState;
    private Map<String, Object> contextData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}
```

#### ChatMessage
- Almacena cada mensaje individual
- Tipo de mensaje (USER, ASSISTANT, SYSTEM, TOOL)
- Orden secuencial para reconstrucción
- Metadata adicional en JSONB

```java
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    private Long id;
    private String conversationId;
    private MessageType messageType;
    private String content;
    private Map<String, Object> metadata;
    private Integer messageOrder;
    private LocalDateTime createdAt;
}
```

### 2. Estados de Conversación

```java
public enum ConversationState {
    INITIAL,                    // Estado inicial
    GREETING,                   // Usuario saludado
    IDENTIFYING_USER,           // Identificando usuario
    USER_IDENTIFIED,            // Usuario identificado
    PRODUCT_INQUIRY,            // Consultando productos
    PRODUCT_SELECTED,           // Producto seleccionado
    PORTABILITY_INQUIRY,        // Consultando portabilidad
    COLLECTING_PORTABILITY_INFO,// Recolectando datos portabilidad
    COLLECTING_ADDRESS,         // Recolectando dirección
    COLLECTING_PAYMENT,         // Configurando pago
    PAYMENT_PENDING,            // Pago pendiente
    ORDER_CREATED,              // Orden creada
    COLLECTING_IMEI,            // Recolectando IMEI
    COLLECTING_NIP,             // Recolectando NIP
    ORDER_COMPLETE,             // Proceso completado
    ERROR_STATE,                // Estado de error
    ABANDONED                   // Conversación abandonada
}
```

### 3. PostgresChatMemory

Implementación de `ChatMemory` con persistencia en PostgreSQL:

```java
@Service
public class PostgresChatMemory implements ChatMemory {
    
    // Métodos estándar de ChatMemory
    void add(String conversationId, List<Message> messages);
    List<Message> get(String conversationId, int lastN);
    void clear(String conversationId);
    
    // Métodos extendidos para gestión de estados
    void updateConversationState(String conversationId, ConversationState state);
    ConversationState getConversationState(String conversationId);
    void addContextData(String conversationId, String key, Object value);
    Object getContextData(String conversationId, String key);
}
```

### 4. ConversationStateService

Servicio dedicado para gestión de estados y transiciones:

```java
@Service
public class ConversationStateService {
    ConversationState getCurrentState(String conversationId);
    boolean transitionTo(String conversationId, ConversationState newState);
    void storeContextData(String conversationId, String key, Object value);
    void resetConversation(String conversationId);
    Map<String, Object> getConversationStats(String conversationId);
}
```

## Flujo de Conversación

```
Usuario envía mensaje
         │
         ▼
ChatService.getBotResponse()
         │
         ├─── Obtiene estado actual
         ├─── Carga contexto RAG
         ├─── Construye prompt con estado
         │
         ▼
ChatClient procesa con tools
         │
         ▼
PostgresChatMemory guarda mensajes
         │
         ▼
ChatService actualiza estado
         │
         ▼
Respuesta al usuario
```

## Tablas de Base de Datos

### chat_conversations
```sql
CREATE TABLE chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    current_state VARCHAR(50) DEFAULT 'INITIAL',
    context_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);
```

### chat_messages
```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    message_order INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation FOREIGN KEY (conversation_id) 
        REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE
);
```

## API Endpoints para Gestión de Conversaciones

### 1. Obtener Estado
```http
GET /api/conversations/{conversationId}/state
```

### 2. Actualizar Estado
```http
POST /api/conversations/{conversationId}/state
Content-Type: application/json

{
  "state": "PRODUCT_SELECTED"
}
```

### 3. Obtener Contexto
```http
GET /api/conversations/{conversationId}/context
```

### 4. Guardar Contexto
```http
POST /api/conversations/{conversationId}/context
Content-Type: application/json

{
  "customerId": 123,
  "selectedProduct": "plan-100gb",
  "needsPortability": true
}
```

### 5. Obtener Estadísticas
```http
GET /api/conversations/{conversationId}/stats
```

### 6. Resetear Conversación
```http
DELETE /api/conversations/{conversationId}
```

## Uso en el Código

### Ejemplo: Guardar Contexto Durante la Conversación

```java
// En una Tool o Service
@Autowired
private ConversationStateService stateService;

public void handleCustomerIdentification(String phoneNumber, Long customerId) {
    // Guardar datos del cliente en el contexto
    stateService.storeContextData(phoneNumber, "customerId", customerId);
    
    // Transicionar al siguiente estado
    stateService.transitionTo(phoneNumber, ConversationState.USER_IDENTIFIED);
}
```

### Ejemplo: Recuperar Contexto

```java
// Recuperar customerId guardado anteriormente
Object customerIdObj = stateService.getContextData(phoneNumber, "customerId");
Long customerId = customerIdObj != null ? ((Number) customerIdObj).longValue() : null;
```

### Ejemplo: Verificar Estado Antes de Acción

```java
ConversationState currentState = stateService.getCurrentState(phoneNumber);

if (currentState == ConversationState.PAYMENT_PENDING) {
    // El usuario ya tiene un pago pendiente
    return "Ya tienes un proceso de pago en curso. ¿Deseas continuarlo?";
}
```

## Validación de Transiciones de Estado

El sistema incluye validación de transiciones para evitar flujos inválidos:

```java
// Transiciones válidas desde PRODUCT_SELECTED:
PRODUCT_SELECTED -> COLLECTING_ADDRESS ✓
PRODUCT_SELECTED -> PORTABILITY_INQUIRY ✓
PRODUCT_SELECTED -> ORDER_COMPLETE ✗ (inválido)
```

## Migración desde Memoria In-Memory

Si estabas usando memoria en RAM, el cambio es transparente:

**Antes:**
```java
@Bean
ChatMemory chatMemory() {
    return new InMemoryChatMemory();
}
```

**Ahora:**
```java
@Bean
ChatMemory chatMemory(PostgresChatMemory postgresChatMemory) {
    return postgresChatMemory;
}
```

## Preparación para State Machine

El sistema está diseñado para facilitar la integración con Spring State Machine:

### Estructura actual (lista para state machine):
- ✅ Estados definidos (ConversationState enum)
- ✅ Validación de transiciones
- ✅ Persistencia de estados
- ✅ Contexto de conversación
- ✅ Eventos de transición

### Próximos pasos para State Machine completa:

1. **Agregar dependencia:**
```xml
<dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-core</artifactId>
</dependency>
```

2. **Definir eventos:**
```java
public enum ConversationEvent {
    USER_GREETED,
    USER_IDENTIFIED,
    PRODUCT_SELECTED,
    ADDRESS_PROVIDED,
    PAYMENT_INITIATED,
    // ...
}
```

3. **Configurar State Machine:**
```java
@Configuration
@EnableStateMachine
public class StateMachineConfig extends StateMachineConfigurerAdapter<ConversationState, ConversationEvent> {
    
    @Override
    public void configure(StateMachineStateConfigurer<ConversationState, ConversationEvent> states) {
        states
            .withStates()
            .initial(ConversationState.INITIAL)
            .states(EnumSet.allOf(ConversationState.class));
    }
    
    @Override
    public void configure(StateMachineTransitionConfigurer<ConversationState, ConversationEvent> transitions) {
        transitions
            .withExternal()
                .source(ConversationState.GREETING)
                .target(ConversationState.USER_IDENTIFIED)
                .event(ConversationEvent.USER_IDENTIFIED)
            // ... más transiciones
    }
}
```

## Monitoreo y Debugging

### Queries útiles para PostgreSQL:

```sql
-- Ver todas las conversaciones activas
SELECT conversation_id, current_state, updated_at 
FROM chat_conversations 
WHERE is_active = true 
ORDER BY updated_at DESC;

-- Contar mensajes por conversación
SELECT conversation_id, COUNT(*) as message_count
FROM chat_messages
GROUP BY conversation_id
ORDER BY message_count DESC;

-- Ver distribución de estados
SELECT current_state, COUNT(*) as count
FROM chat_conversations
WHERE is_active = true
GROUP BY current_state;

-- Conversaciones en error
SELECT conversation_id, context_data, updated_at
FROM chat_conversations
WHERE current_state = 'ERROR_STATE'
ORDER BY updated_at DESC;
```

### Logs importantes:

```
INFO  c.p.b.s.ChatService - Processing message for user 1234567890 in state: PRODUCT_INQUIRY
INFO  c.p.b.s.ConversationStateService - State transition for 1234567890: PRODUCT_INQUIRY -> PRODUCT_SELECTED
DEBUG c.p.b.s.PostgresChatMemory - Adding 2 messages to conversation: 1234567890
```

## Ventajas del Sistema

### ✅ Persistencia
- Conversaciones sobreviven a reinicios del servidor
- Historial completo disponible para análisis
- Recuperación de contexto entre sesiones

### ✅ Estado Controlado
- Flujo de conversación predecible
- Validación de transiciones
- Prevención de estados inválidos

### ✅ Contexto Rico
- Almacenamiento flexible con JSONB
- Metadatos estructurados
- Fácil extensión

### ✅ Escalabilidad
- PostgreSQL maneja millones de registros
- Índices optimizados para queries rápidos
- Limpieza automática con CASCADE

### ✅ Debugging Mejorado
- Trazabilidad completa de conversaciones
- Análisis de flujos problemáticos
- Estadísticas de uso

## Mejores Prácticas

1. **Limpiar conversaciones antiguas:**
```sql
DELETE FROM chat_conversations 
WHERE updated_at < NOW() - INTERVAL '30 days' 
AND current_state IN ('ORDER_COMPLETE', 'ABANDONED');
```

2. **Limitar mensajes cargados:**
```java
// El sistema ya limita a 50 mensajes por defecto
private static final int MAX_MESSAGES_TO_LOAD = 50;
```

3. **Usar transacciones:**
```java
@Transactional
public void processOrder(...) {
    // Todas las operaciones en una transacción
}
```

4. **Monitorear estados de error:**
```java
if (currentState == ConversationState.ERROR_STATE) {
    // Lógica de recuperación
    stateService.transitionTo(conversationId, ConversationState.INITIAL);
}
```

## Troubleshooting

### Problema: Los mensajes no persisten
**Solución:** Verificar que las tablas existan y que `spring.sql.init.mode=always` esté configurado.

### Problema: Estado no se actualiza
**Solución:** Verificar que las transacciones estén habilitadas y que no haya excepciones silenciadas.

### Problema: Conversaciones duplicadas
**Solución:** El `conversation_id` debe ser único (generalmente el `phoneNumber`).

### Problema: Performance lento
**Solución:** Verificar que los índices estén creados correctamente.
