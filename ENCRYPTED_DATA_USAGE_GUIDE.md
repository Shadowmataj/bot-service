# Guía de Uso de Datos Encriptados

## 🔐 Resumen

A partir de ahora, los siguientes datos se almacenan **ENCRIPTADOS** en `context_data`:
- ✅ `portability_nip` - NIP de portabilidad
- ✅ `portability_imei` - IMEI del dispositivo
- ✅ `checkout_session_url` - URL de sesión de pago de Stripe

**Encriptación:** AES-256-GCM con IV aleatorio
**Clave:** Configurada en `application.properties` como `security.encryption.key`

---

## ✅ Cómo Usar Datos Encriptados

### ❌ FORMA INCORRECTA (Devuelve dato encriptado)

```java
// NO HAGAS ESTO - obtendrás basura ilegible Base64
String nip = (String) stateService.getContextData(conversationId, "portability_nip");
// Resultado: "dGVzdC1pdi1zdHJpbmc6ZW5jcnlwdGVkLWRhdGEtaGVyZQ==" (NO es el NIP real)
```

### ✅ FORMA CORRECTA (Desencripta automáticamente)

```java
@Autowired
private ContextDataManager contextDataManager;

// Obtener NIP desencriptado
String nip = contextDataManager.getDecryptedPortabilityNip(conversationId);
// Resultado: "1234" (el NIP real en texto plano)

// Obtener IMEI desencriptado
String imei = contextDataManager.getDecryptedPortabilityImei(conversationId);
// Resultado: "354234567890123" (el IMEI real)

// Obtener Checkout URL desencriptada
String checkoutUrl = contextDataManager.getDecryptedCheckoutUrl(conversationId);
// Resultado: "https://checkout.stripe.com/c/pay/cs_test_..." (la URL real)
```

---

## 📦 Métodos Disponibles en ContextDataManager

| Método | Retorno | Descripción |
|--------|---------|-------------|
| `getDecryptedPortabilityNip(conversationId)` | `String` | Desencripta y devuelve el NIP de portabilidad |
| `getDecryptedPortabilityImei(conversationId)` | `String` | Desencripta y devuelve el IMEI |
| `getDecryptedCheckoutUrl(conversationId)` | `String` | Desencripta y devuelve la URL de Stripe |

**Manejo de Errores:**
- Si el dato no existe: retorna `null`
- Si falla la desencriptación: retorna `null` y loguea error
- Nunca lanza excepciones (fail-safe)

---

## 🔧 Ejemplo Práctico: Tool que Necesita NIP

### Caso: Enviar NIP a API de portabilidad

```java
@Service
public class PortabilityTools {
    
    @Autowired
    private ContextDataManager contextDataManager;
    
    @Autowired
    private PortabilitiesInterface portabilitiesApi;

    public void submitPortabilityNip(String conversationId, Long portabilityId) {
        // ✅ Correcto: usar método de desencriptación
        String nip = contextDataManager.getDecryptedPortabilityNip(conversationId);
        
        if (nip == null) {
            throw new RuntimeException("NIP not found in context");
        }
        
        // Enviar NIP a la API
        portabilitiesApi.updateNip(portabilityId, nip);
        
        logger.info("NIP submitted for portability: {}", portabilityId);
        // Nota: NO logueamos el NIP en texto plano
    }
}
```

---

## 🔄 Flujo Completo: Almacenamiento → Recuperación

### 1. Almacenamiento (Automático en ContextStorageAspect)

```java
// Tool response contiene NIP en texto plano
PortabilityResponse response = portabilitiesApi.updateNip(id, "1234");

// ContextDataManager.extractRelevantData() automáticamente:
String encryptedNip = encryptor.encrypt("1234");
data.put("portability_nip", encryptedNip);

// Se almacena en DB:
// context_data: {"portability_nip": "dGVzdC1pdi1zdHJpbmc6ZW5jcnlwdGVkLWRhdGEtaGVyZQ=="}
```

### 2. Recuperación (Manual en tus Tools)

```java
// Cuando necesites el NIP:
String nip = contextDataManager.getDecryptedPortabilityNip(conversationId);

// Internamente ejecuta:
// 1. stateService.getContextData() → obtiene string encriptado
// 2. encryptor.decrypt() → desencripta a texto plano
// 3. return "1234" → devuelve NIP original
```

---

## 🛡️ Seguridad: Qué Está Protegido

### En Base de Datos:
```sql
-- Antes (inseguro):
portability_nip: "1234"

-- Ahora (seguro):
portability_nip: "iNzXyZ9K/3Jq... [base64 encriptado]"
```

### En Logs:
```log
# Antes (inseguro):
DEBUG - Extracted portability data: ID=123, phone=9461234567, nip=1234

# Ahora (seguro):
DEBUG - Extracted portability data: ID=123, phone=946***4567, has_nip=true
```

### En Memoria (Runtime):
- ✅ Dato encriptado en `context_data` JSONB
- ✅ Desencriptado solo cuando se llama método `getDecrypted*()`
- ✅ No se cachea en texto plano
- ✅ GC de Java limpia memoria después de uso

---

## ⚠️ Consideraciones Importantes

### 1. Datos Legacy (Pre-Encriptación)
Si tienes conversaciones con datos almacenados **antes** de activar encriptación:

```java
// Detectar si dato está encriptado o en texto plano:
String nip = contextDataManager.getDecryptedPortabilityNip(conversationId);

if (nip == null) {
    // Fallback: intentar obtener directamente (dato legacy)
    nip = (String) stateService.getContextData(conversationId, "portability_nip");
    
    if (nip != null && !nip.matches("^[A-Za-z0-9+/=]+$")) {
        // Es dato legacy en texto plano, re-encriptar:
        String encrypted = encryptor.encrypt(nip);
        Map<String, Object> data = new HashMap<>();
        data.put("portability_nip", encrypted);
        stateService.storeContextData(conversationId, data);
    }
}
```

### 2. Testing en Desarrollo

Para testing, puedes verificar que la encriptación funciona:

```java
@Test
public void testNipEncryption() {
    // 1. Simular tool response
    PortabilityResponse response = new PortabilityResponse();
    response.setPortabilityNip("1234");
    
    // 2. Procesar y almacenar
    contextDataManager.processToolResponse(conversationId, "updatePortabilityNip", response);
    
    // 3. Verificar que está encriptado en DB
    String encryptedNip = (String) stateService.getContextData(conversationId, "portability_nip");
    assertNotEquals("1234", encryptedNip); // No debe ser texto plano
    assertTrue(encryptedNip.length() > 20); // Base64 es más largo
    
    // 4. Verificar que desencriptación funciona
    String decryptedNip = contextDataManager.getDecryptedPortabilityNip(conversationId);
    assertEquals("1234", decryptedNip); // Debe desencriptar correctamente
}
```

### 3. Rotación de Clave (Cada 90 Días Recomendado)

Si rotas la clave de encriptación:

1. Datos existentes NO podrán desencriptarse con nueva clave
2. Opciones:
   - **Opción A:** Dejar que expire por política 30 días
   - **Opción B:** Mantener ambas claves (`old.key` + `new.key`) y desencriptar con fallback
   - **Opción C:** Script de migración que desencripta con old key y re-encripta con new key

```properties
# application.properties (ejemplo multi-key)
security.encryption.key=nueva_clave_aqui
security.encryption.key.old=vieja_clave_aqui
```

---

## 🔍 Debugging: Verificar Estado de Encriptación

### Query SQL para ver datos encriptados:

```sql
-- Ver NIP encriptado (debe ser base64 largo)
SELECT 
    conversation_id,
    context_data->'portability_nip' as nip_encrypted,
    LENGTH(context_data->>'portability_nip') as nip_length
FROM chat_conversations
WHERE context_data ? 'portability_nip'
LIMIT 5;

-- Resultado esperado:
-- nip_encrypted: "iNzXyZ9K/3Jq2Lm4Vp8Wq..." (base64 largo)
-- nip_length: 44+ caracteres

-- ❌ Si ves esto, NO está encriptado:
-- nip_encrypted: "1234"
-- nip_length: 4 caracteres
```

### Logs para verificar extracción:

```bash
tail -f logs/bot-service.log | grep "Extracted portability"

# ✅ Correcto (no muestra NIP):
INFO - Extracted portability data: ID=123, phone=946***4567, has_nip=true

# ❌ Incorrecto (muestra NIP en plano):
INFO - Extracted portability data: ID=123, nip=1234
```

---

## 📞 FAQ

**P: ¿Puedo acceder a datos encriptados desde SQL directamente?**  
R: No. Los datos están encriptados con clave en `application.properties`. Solo Java puede desencriptar.

**P: ¿Qué pasa si pierdo la clave de encriptación?**  
R: Los datos encriptados serán irrecuperables. **BACKUP DE LA CLAVE ES CRÍTICO**.

**P: ¿Puedo desactivar encriptación temporalmente?**  
R: Sí, pero tendrías que modificar `ContextDataManager` para *no* encriptar. NO recomendado.

**P: ¿La encriptación afecta performance?**  
R: Mínimamente. AES-256-GCM es muy rápido (~1ms por operación en hardware moderno).

**P: ¿Los datos en EnrichContext están encriptados?**  
R: No. `ContextEnricher` genera resumen legible para OpenAI. Los datos sensibles NO se incluyen en el prompt.

---

## 🎯 Checklist de Integración

Al agregar un nuevo tool que use datos sensibles:

- [ ] Identificar qué datos son sensibles (PII, credenciales, tokens)
- [ ] Verificar que `ContextDataManager` los encripta en `extractRelevantData()`
- [ ] Usar métodos `getDecrypted*()` en lugar de `getContextData()` directo
- [ ] NO loguear datos sensibles en texto plano
- [ ] Testing: verificar dato encriptado en DB
- [ ] Testing: verificar desencriptación funcional

---

**Contacto:** Equipo de Seguridad  
**Última actualización:** 2026-02-09  
**Versión:** 1.0
