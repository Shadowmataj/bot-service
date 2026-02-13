# Sistema de Manejo de Excepciones para Tools del Bot

## Descripción General

Este sistema proporciona un manejo de excepciones consistente y centralizado para todas las `@Tool` utilizadas por el chatbot, permitiendo:

1. **Identificar fallos** en las tools de forma clara
2. **Comunicar errores al usuario** sin detalles técnicos
3. **Logging completo** de errores para debugging
4. **Mensajes amigables** que el LLM puede entender y comunicar

## Componentes del Sistema

### 1. ToolExecutionException (`exception/ToolExecutionException.java`)

Excepción personalizada que encapsula:
- **toolName**: Nombre de la tool que falló
- **userFriendlyMessage**: Mensaje claro para el usuario/LLM
- **technicalDetails**: Detalles técnicos para logging

```java
throw new ToolExecutionException(
    "createAddress",
    "No se pudo registrar la dirección. Verifica que todos los datos sean correctos",
    "HTTP Status: 400"
);
```

### 2. ToolExceptionHandlingAspect (`aspect/ToolExceptionHandlingAspect.java`)

Aspect que intercepta todas las `@Tool` para:
- Registrar ejecución de tools
- Capturar y registrar excepciones con detalles técnicos
- Re-lanzar excepciones con mensajes amigables
- Convertir excepciones inesperadas en `ToolExecutionException`

### 3. ChatService Actualizado (`service/ChatService.java`)

Maneja las excepciones a nivel de servicio:
- Captura `ToolExecutionException` y retorna mensaje amigable al LLM
- Registra errores para debugging
- Proporciona fallback para errores inesperados

## Flujo de Manejo de Errores

```
┌─────────────────┐
│   User Query    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  ChatService    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Tool Method   │◄─── Interceptado por ToolExceptionHandlingAspect
└────────┬────────┘
         │
         ▼
    ¿Éxito?
         │
    ┌────┴────┐
    │         │
   Sí        No
    │         │
    │         ▼
    │  ┌──────────────────────┐
    │  │ ToolExecutionException│
    │  └──────────┬────────────┘
    │            │
    │            ▼
    │  ┌──────────────────────┐
    │  │  Log detalles técnicos│
    │  └──────────┬────────────┘
    │            │
    ▼            ▼
┌──────────────────────────┐
│ Retorna mensaje amigable │
└────────┬─────────────────┘
         │
         ▼
┌─────────────────┐
│   LLM recibe    │
│  y comunica     │
│   al usuario    │
└─────────────────┘
```

## Tipos de Mensajes de Error

### Errores de Validación
```java
"El número de teléfono debe tener entre 10 y 15 dígitos"
"Se requiere el ID del cliente para crear la sesión de pago"
```

### Errores de Servicio
```java
"El servicio de direcciones no está disponible en este momento"
"El servicio de portabilidad no está disponible en este momento"
```

### Errores de Negocio
```java
"No se encontró un cliente con ese email"
"No se pudo registrar el cliente. Verifica que el email o teléfono no estén ya registrados"
```

### Errores Genéricos
```java
"Ocurrió un error al registrar la dirección"
"Ocurrió un error inesperado. Por favor, intenta nuevamente"
```

## Ejemplo de Uso en una Tool

```java
@Tool(name = "registerCustomer", description = "...")
public String registerCustomer(CustomerRequest request) {
    try {
        // Validación de entrada
        if (request.getEmail() == null) {
            throw new ToolExecutionException(
                "registerCustomer",
                "El email es requerido para registrar un cliente",
                "Email is null"
            );
        }
        
        // Llamada al servicio
        ResponseEntity<String> response = usersInterface.register(request);
        
        // Verificación de respuesta
        if (response.getStatusCode().isError() || response.getBody() == null) {
            throw new ToolExecutionException(
                "registerCustomer",
                "No se pudo registrar el cliente. Verifica que el email o teléfono no estén ya registrados",
                "HTTP Status: " + response.getStatusCode()
            );
        }
        
        return response.getBody();
        
    } catch (ToolExecutionException e) {
        // Re-lanzar nuestras excepciones personalizadas
        throw e;
        
    } catch (FeignException e) {
        // Manejar errores de Feign (servicios externos)
        throw new ToolExecutionException(
            "registerCustomer",
            "El servicio de registro no está disponible en este momento",
            e
        );
        
    } catch (Exception e) {
        // Catch-all para errores inesperados
        throw new ToolExecutionException(
            "registerCustomer",
            "Ocurrió un error al registrar el cliente",
            e
        );
    }
}
```

## Ventajas del Sistema

### Para el Usuario
- ✅ Mensajes claros y accionables
- ✅ Sin jerga técnica
- ✅ Instrucciones de qué hacer a continuación

### Para el Desarrollador
- ✅ Logging completo con stack traces
- ✅ Detalles técnicos preservados
- ✅ Fácil debugging
- ✅ Código consistente y mantenible

### Para el LLM
- ✅ Mensajes estructurados y comprensibles
- ✅ Contexto claro del error
- ✅ Puede sugerir acciones alternativas
- ✅ Mantiene la conversación fluida

## Logs Generados

Cuando ocurre un error, se generan logs como:

```
ERROR c.p.b.a.ToolExceptionHandlingAspect - Tool execution failed - 
    Tool: CustomerTools.registerCustomer, 
    User Message: No se pudo registrar el cliente. Verifica que el email o teléfono no estén ya registrados, 
    Technical Details: HTTP Status: 409 CONFLICT
    
ERROR c.p.b.s.ChatService - Tool execution failed: registerCustomer - HTTP Status: 409 CONFLICT
```

## Mantenimiento

### Agregar una Nueva Tool

1. Crea el método con `@Tool`
2. Implementa try-catch con `ToolExecutionException`
3. Define mensajes amigables para cada tipo de error
4. El aspect se encargará automáticamente del logging

### Personalizar Mensajes

Para personalizar mensajes para casos específicos:

```java
if (phoneNumber.INITIALsWith("55")) {
    throw new ToolExecutionException(
        "createPortability",
        "Los números que inician con 55 requieren documentación adicional",
        "Phone starts with 55, special handling needed"
    );
}
```

## Consideraciones de Seguridad

- ✅ Nunca exponer información sensible en mensajes de usuario
- ✅ No incluir IDs de sistema o referencias internas
- ✅ Mantener detalles técnicos solo en logs
- ✅ Sanitizar mensajes de error de servicios externos

## Testing

Para probar el manejo de excepciones:

```java
@Test
public void testToolExecutionException() {
    // Simular error
    when(service.call()).thenThrow(new RuntimeException("Service down"));
    
    // Verificar excepción personalizada
    ToolExecutionException exception = assertThrows(
        ToolExecutionException.class,
        () -> tool.executeOperation()
    );
    
    // Verificar mensajes
    assertEquals("operationName", exception.getToolName());
    assertTrue(exception.getUserFriendlyMessage().contains("no está disponible"));
}
```

## Monitoreo

Se recomienda monitorear:
- Frecuencia de `ToolExecutionException` por tool
- Tipos de errores más comunes
- Tiempos de respuesta de tools
- Tasa de reintentos del usuario después de un error
