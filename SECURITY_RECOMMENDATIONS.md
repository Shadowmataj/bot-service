# Recomendaciones de Seguridad para Context Data

## 🚨 Resumen Ejecutivo

El sistema actual almacena datos personales y sensibles (PII) en texto plano en PostgreSQL. Los riesgos identificados son ALTOS para:
- NIP de portabilidad
- IMEI de dispositivos
- URLs de checkout de Stripe
- Email, teléfono, dirección

## 📋 Plan de Acción por Prioridad

### 🔥 CRÍTICO (Implementar YA)

#### 1. Encriptar Datos Sensibles en Almacenamiento

**Problema**: NIP, IMEI, checkout URLs en texto plano

**Solución**: Encriptar campos específicos antes de almacenar

```java
@Service
public class SensitiveDataEncryptor {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    @Value("${security.encryption.key}")
    private String encryptionKey; // Debe estar en variable de entorno, NO en código
    
    public String encrypt(String plaintext) {
        try {
            SecretKey key = getKeyFromString(encryptionKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Concatenar IV + ciphertext
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new SecurityException("Encryption failed", e);
        }
    }
    
    public String decrypt(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            
            byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);
            
            SecretKey key = getKeyFromString(encryptionKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("Decryption failed", e);
        }
    }
    
    private SecretKey getKeyFromString(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}
```

**Uso en ContextDataManager**:
```java
@Service
public class ContextDataManager {
    
    @Autowired
    private SensitiveDataEncryptor encryptor;
    
    // Lista de campos que DEBEN ser encriptados
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "portability_nip",           // CRÍTICO
        "portability_imei",          // CRÍTICO
        "checkout_session_url",      // CRÍTICO
        "customer_email",            // PII
        "customer_phone",            // PII
        "address_full"               // PII
    );
    
    private Map<String, Object> extractRelevantData(String toolName, Object toolResponse) {
        Map<String, Object> data = new HashMap<>();
        
        // ... extracción normal ...
        
        // Encriptar campos sensibles antes de retornar
        return encryptSensitiveFields(data);
    }
    
    private Map<String, Object> encryptSensitiveFields(Map<String, Object> data) {
        Map<String, Object> encrypted = new HashMap<>(data);
        
        for (String field : SENSITIVE_FIELDS) {
            if (encrypted.containsKey(field) && encrypted.get(field) != null) {
                String value = encrypted.get(field).toString();
                encrypted.put(field, encryptor.encrypt(value));
                encrypted.put(field + "_encrypted", true); // Marcador
            }
        }
        
        return encrypted;
    }
    
    // Método para desencriptar cuando se necesite leer
    public String getSensitiveData(String conversationId, String fieldName) {
        Object value = stateService.getContextData(conversationId, fieldName);
        if (value != null && stateService.getContextData(conversationId, fieldName + "_encrypted") != null) {
            return encryptor.decrypt(value.toString());
        }
        return value != null ? value.toString() : null;
    }
}
```

**Variables de Entorno**:
```bash
# .env o application.properties (NO COMMITEAR)
security.encryption.key=YOUR_BASE64_ENCODED_256_BIT_KEY_HERE
```

**Generar clave**:
```bash
# Generar clave AES-256 segura
openssl rand -base64 32
```

---

#### 2. Sanitizar Logs (No Exponer PII)

**Problema**: Logs muestran emails, teléfonos, etc.

**Solución**: Enmascarar datos sensibles en logs

```java
@Service
public class LogSanitizer {
    
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
    
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
    
    public static String maskNip(String nip) {
        return "****"; // Nunca mostrar NIPs
    }
    
    public static String maskImei(String imei) {
        if (imei == null || imei.length() < 4) return "***";
        return "***" + imei.substring(imei.length() - 4);
    }
}
```

**Actualizar Logs**:
```java
// ❌ ANTES (INSEGURO)
logger.debug("Extracted customer data: ID={}, email={}", customer.id(), customer.email());

// ✅ DESPUÉS (SEGURO)
logger.debug("Extracted customer data: ID={}, email={}", customer.id(), LogSanitizer.maskEmail(customer.email()));

// ❌ ANTES
logger.debug("Extracted portability data: ID={}, phone={}", portability.getId(), portability.getPhoneNumber());

// ✅ DESPUÉS
logger.debug("Extracted portability data: ID={}, phone={}", portability.getId(), LogSanitizer.maskPhone(portability.getPhoneNumber()));
```

---

#### 3. Implementar Auto-Limpieza de Datos Sensibles

**Problema**: Datos almacenados indefinidamente

**Solución**: Borrar datos sensibles después de X días

```java
@Service
public class ContextDataCleanupService {
    
    @Autowired
    private ChatConversationRepository conversationRepository;
    
    @Autowired
    private ConversationStateService stateService;
    
    // Ejecutar cada día a las 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupSensitiveData() {
        logger.info("Starting sensitive data cleanup task...");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // 30 días
        
        List<ChatConversation> oldConversations = conversationRepository
            .findByUpdatedAtBeforeAndIsActiveTrue(cutoffDate);
        
        for (ChatConversation conversation : oldConversations) {
            removeSensitiveFields(conversation);
        }
        
        logger.info("Cleaned up {} old conversations", oldConversations.size());
    }
    
    private void removeSensitiveFields(ChatConversation conversation) {
        Map<String, Object> context = conversation.getContextData();
        
        if (context != null) {
            // Remover datos sensibles pero mantener IDs para referencias
            context.remove("customer_email");
            context.remove("customer_phone");
            context.remove("address_full");
            context.remove("address_street");
            context.remove("address_district");
            context.remove("address_reference");
            context.remove("portability_nip");
            context.remove("portability_imei");
            context.remove("checkout_session_url");
            
            // Marcar como limpiado
            context.put("_cleaned_at", LocalDateTime.now().toString());
            
            conversation.setContextData(context);
            conversationRepository.save(conversation);
        }
    }
}
```

**Agregar al Repository**:
```java
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    // ... métodos existentes ...
    
    List<ChatConversation> findByUpdatedAtBeforeAndIsActiveTrue(LocalDateTime date);
}
```

---

### ⚠️ ALTO (Implementar en Sprint Actual)

#### 4. Validar Propiedad de Conversación

**Problema**: Cualquiera con el ID puede acceder al contexto

**Solución**: Verificar que el usuario es el dueño

```java
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    
    @GetMapping("/{conversationId}/context")
    @Operation(summary = "Get conversation context (requires authentication)")
    public ResponseEntity<Map<String, Object>> getConversationContext(
            @PathVariable String conversationId,
            @RequestHeader(value = "X-Phone-Number", required = false) String phoneNumber) {
        
        // Validar que el conversationId coincide con el phone_number del request
        if (phoneNumber == null || !conversationId.equals(phoneNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied"));
        }
        
        Map<String, Object> context = stateService.getAllContextData(conversationId);
        
        // Filtrar datos ultra-sensibles antes de retornar
        context.remove("portability_nip");
        context.remove("portability_imei");
        
        return ResponseEntity.ok(Map.of(
            "conversationId", conversationId,
            "context", context
        ));
    }
}
```

#### 5. Revocar URLs de Checkout Usadas

**Problema**: URLs de Stripe permanecen válidas

**Solución**: Limpiar URL después de pago completado

```java
@Service
public class ContextDataManager {
    
    public void markPaymentCompleted(String conversationId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("payment_completed", true);
        
        // Remover URL de checkout (ya no es necesaria)
        updates.put("checkout_session_url", null);
        
        // Mantener solo el ID de sesión para referencia
        stateService.storeContextData(conversationId, updates);
        
        logger.info("Payment marked as completed and checkout URL removed for conversation: {}", conversationId);
    }
}
```

---

### 📊 MEDIO (Considerar para Siguientes Sprints)

#### 6. Auditoría de Acceso a Datos Sensibles

```java
@Aspect
@Component
public class DataAccessAuditAspect {
    
    @Around("execution(* com.portability.bot_service.service.ConversationStateService.getContextData(..))")
    public Object auditContextAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String conversationId = (String) joinPoint.getArgs()[0];
        String key = (String) joinPoint.getArgs()[1];
        
        // Loguear acceso a campos sensibles
        if (SENSITIVE_FIELDS.contains(key)) {
            logger.warn("Sensitive field '{}' accessed for conversation: {}", key, conversationId);
            // Aquí podrías enviar a un sistema de auditoría externo
        }
        
        return joinPoint.proceed();
    }
}
```

#### 7. Encriptación a Nivel de Base de Datos

**PostgreSQL Transparent Data Encryption (TDE)** o **pgcrypto extension**:

```sql
-- Alternativa: usar pgcrypto de PostgreSQL
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Encriptar al insertar
INSERT INTO chat_conversations (context_data) 
VALUES (pgp_sym_encrypt('{"nip": "1234"}', 'encryption_key'));

-- Desencriptar al leer
SELECT pgp_sym_decrypt(context_data::bytea, 'encryption_key') 
FROM chat_conversations;
```

---

## 🎯 Implementación Recomendada

### Fase 1 (Esta semana):
1. ✅ Sanitizar logs
2. ✅ Remover checkout URLs después de pago

### Fase 2 (Próxima semana):
3. ✅ Encriptar NIP, IMEI, checkout URLs
4. ✅ Implementar limpieza automática (30 días)

### Fase 3 (Siguiente sprint):
5. ✅ Validación de propiedad en endpoints
6. ✅ Auditoría de acceso

---

## 📝 Cumplimiento Legal

### GDPR (Europa) / LGPD (Brasil)
- ✅ **Minimización de datos**: Solo almacenar lo necesario
- ✅ **Derecho al olvido**: Limpieza automática + endpoint de borrado
- ✅ **Seguridad**: Encriptación de PII
- ✅ **Transparencia**: Documentar qué se almacena y por cuánto tiempo

### Política Recomendada
```
Datos almacenados temporalmente durante la conversación:
- Email, teléfono: Encriptados, eliminados después de 30 días
- Dirección: Encriptada, eliminada después de 30 días
- NIP/IMEI: Encriptados, eliminados inmediatamente después de uso
- URLs de pago: Eliminadas inmediatamente después de completar pago

Datos permanentes:
- IDs de referencia (customer_id, order_id, etc.)
- Estado de conversación
- Timestamps
```

---

## 🚀 Comandos Útiles

### Generar clave de encriptación:
```bash
openssl rand -base64 32
```

### Verificar logs por datos sensibles:
```bash
grep -r "email.*@" logs/
grep -r "[0-9]\{10,\}" logs/ # Buscar posibles teléfonos/IMEIs
```

### Revisar datos en BD:
```sql
SELECT conversation_id, 
       context_data->>'customer_email' as email,
       context_data->>'portability_nip' as nip,
       updated_at
FROM chat_conversations
WHERE updated_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
```

---

## ✅ Checklist de Seguridad

- [ ] NIPs encriptados en BD
- [ ] IMEIs encriptados en BD
- [ ] Checkout URLs removidas después de pago
- [ ] Logs no muestran PII en texto plano
- [ ] Política de retención implementada (30 días)
- [ ] Validación de propiedad en endpoints
- [ ] Auditoría de acceso a datos sensibles
- [ ] Documentación de privacidad actualizada
- [ ] Variables de entorno para claves de encriptación
- [ ] Endpoint de borrado de datos (GDPR compliance)
