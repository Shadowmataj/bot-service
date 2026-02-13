# Bot Service - Sistema Inteligente de Portabilidad 🤖

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-with%20pgvector-blue)
![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o-purple)

## 📋 Descripción

**Bot Service** es un servicio de chatbot inteligente para gestionar el proceso completo de portabilidad telefónica y venta de tarjetas SIM. Utiliza **Spring AI** integrado con **OpenAI GPT-4o** para proporcionar una experiencia conversacional natural y fluida.

El sistema implementa un **flujo de estado conversacional** con gestión automática de contexto, integración con WhatsApp, almacenamiento vectorial para RAG (Retrieval-Augmented Generation), y múltiples herramientas (tools) que interactúan con microservicios externos.

### 🎯 Características Principales

- **🤖 Chatbot Conversacional Avanzado**: Basado en OpenAI GPT-4o con capacidades de function calling
- **📱 Integración WhatsApp**: Soporte completo para mensajes de WhatsApp con buffer inteligente
- **🎭 Gestión de Estados**: Sistema de máquina de estados para controlar el flujo conversacional
- **💾 Memoria Persistente**: Almacenamiento de conversaciones en PostgreSQL con historial completo
- **🧠 Sistema RAG**: Vector Store con PGVector para búsqueda semántica de documentos
- **🔧 Function Calling Tools**: 5 categorías de herramientas para operaciones especializadas
- **📊 Gestión de Contexto**: Almacenamiento automático de datos del usuario para evitar preguntas repetitivas
- **🔐 Seguridad**: Sanitización de logs y encriptación de datos sensibles
- **🌐 Microservicios**: Integración con servicios externos mediante OpenFeign
- **📝 Swagger UI**: Documentación interactiva de la API

---

## 🏗️ Arquitectura

### Componentes Principales

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                              │
│                    (Autenticación JWT)                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────────┐
│                      Bot Service                               │
│                                                                │
│  ┌─────────────────┐    ┌──────────────────┐                   │
│  │  Controllers    │    │   Orchestrator   │                   │
│  │  - Chat         │───▶│  - State Machine │                   │
│  │  - Conversation │    │  - Context Mgmt  │                   │
│  │  - Documents    │    └───────┬──────────┘                   │
│  └─────────────────┘            │                              │
│                                 │                              │
│  ┌──────────────────────────────▼──────────────────┐           │
│  │           Spring AI ChatClient                  │           │
│  │  - GPT-4o (OpenAI)                              │           │
│  │  - Function Calling                             │           │
│  │  - Chat Memory                                  │           │
│  └────────┬──────────────────────┬─────────────────┘           │
│           │                      │                             │
│  ┌────────▼────────┐    ┌────────▼────────┐                    │
│  │   Tools (5)     │    │  Vector Store   │                    │
│  │  - Customer     │    │   (PGVector)    │                    │
│  │  - Address      │    │   RAG Context   │                    │
│  │  - Order        │    └─────────────────┘                    │
│  │  - Payment      │                                           │
│  │  - Scraper      │                                           │
│  └────────┬────────┘                                           │
│           │                                                    │
└───────────┼────────────────────────────────────────────────────┘
            │
┌───────────▼──────────────────────────────────────────────────────┐
│                  External Microservices                          │
│  - Users Service     - Products Service    - Scraper Service     │
│  - Addresses Service - Payments Service    - Portabilities Svc   │
└──────────────────────────────────────────────────────────────────┘
```

### Stack Tecnológico

#### Backend Core
- **Java 25**
- **Spring Boot 3.5.9**
- **Spring AI 1.1.2**
- **Spring Cloud 2025.0.1**

#### Inteligencia Artificial
- **OpenAI GPT-4o** (Chat Model)
- **text-embedding-ada-002** (Embeddings)
- **Spring AI Advisors** (Memory & Vector Store)

#### Base de Datos
- **PostgreSQL** con extensiones:
  - `pgvector` - Almacenamiento vectorial para embeddings
  - `hstore` - Datos clave-valor

#### Integración
- **OpenFeign** - Cliente HTTP declarativo
- **Apache HttpClient 5** - Soporte PATCH
- **OkHttp3** - Cliente HTTP adicional

#### Documentación
- **SpringDoc OpenAPI 3** (Swagger UI)

#### Monitoreo
- **Spring Boot AOP** - Logging y aspectos transversales
- **Eureka Client** - Registro de servicios

---

## 📁 Estructura del Proyecto

```
bot-service/
├── src/main/java/com/portability/bot_service/
│   ├── BotServiceApplication.java          # Punto de entrada
│   │
│   ├── aspect/                              # Aspectos AOP
│   │   ├── ContextStorageAspect.java        # Almacenamiento automático de contexto
│   │   ├── ServiceLoggingAspect.java        # Logging transversal
│   │   └── ToolExceptionHandlingAspect.java # Manejo de excepciones tools
│   │
│   ├── config/                              # Configuración
│   │   ├── AppConfig.java                   # Beans principales
│   │   ├── OkHttpConfig.java                # Cliente HTTP
│   │   └── SwaggerConfig.java               # Documentación API
│   │
│   ├── controller/                          # Controladores REST
│   │   ├── ChatController.java              # Endpoints chat y WhatsApp
│   │   ├── ConversationController.java      # Gestión de conversaciones
│   │   └── DocumentController.java          # Gestión de documentos
│   │
│   ├── service/                             # Lógica de negocio
│   │   ├── ChatOrchestratorService.java     # Orquestador principal
│   │   ├── ChatService.java                 # Lógica de chat
│   │   ├── PostgresChatMemory.java          # Memoria persistente
│   │   ├── ConversationStateService.java    # Máquina de estados
│   │   ├── ContextDataManager.java          # Gestión de contexto
│   │   ├── ContextEnricher.java             # Enriquecedor de contexto
│   │   ├── DocumentService.java             # Gestión de documentos
│   │   ├── WhatsAppMessageBufferService.java # Buffer de mensajes WhatsApp
│   │   └── ContextDataCleanupService.java   # Limpieza programada
│   │
│   ├── tools/                               # Function Calling Tools
│   │   ├── CustomerTools.java               # Registro y búsqueda de clientes
│   │   ├── AddressesTools.java              # Gestión de direcciones
│   │   ├── OrderTools.java                  # Creación de órdenes
│   │   ├── PaymentTools.java                # Procesamiento de pagos
│   │   └── ScraperTools.java                # Validación IMEI
│   │
│   ├── feign/                               # Clientes Feign
│   │   ├── UsersInterface.java
│   │   ├── AddressesInterface.java
│   │   ├── ProductsInterface.java
│   │   ├── PortabilitiesInterface.java
│   │   ├── PaymentsInterface.java
│   │   └── ScraperInterface.java
│   │
│   ├── model/                               # Modelos de datos
│   │   ├── dto/                             # Data Transfer Objects
│   │   ├── entity/                          # Entidades JPA
│   │   └── enm/                             # Enumeraciones
│   │
│   ├── repository/                          # Repositorios JPA
│   │   ├── ChatConversationRepository.java
│   │   └── ChatMessageRepository.java
│   │
│   ├── security/                            # Seguridad
│   │   ├── LogSanitizer.java                # Sanitización de logs
│   │   └── SensitiveDataEncryptor.java      # Encriptación AES-256
│   │
│   └── exception/                           # Excepciones
│       └── ToolExecutionException.java
│
├── src/main/resources/
│   ├── application.properties               # Configuración principal
│   ├── init/schema.sql                      # Script de inicialización DB
│   └── prompts/chatbot-rag-prompt.st        # Plantilla de prompt
│
└── Documentación adicional/
    ├── CONTEXT_MANAGEMENT_GUIDE.md          # Guía de gestión de contexto
    ├── CONVERSATION_ORCHESTRATOR_GUIDE.md   # Guía del orquestador
    ├── SECURITY_IMPLEMENTATION_STATUS.md    # Estado de seguridad
    ├── PERSISTENT_MEMORY_GUIDE.md           # Guía de memoria persistente
    └── DEPLOYMENT_READY.md                  # Guía de despliegue
```

---

## 🚀 Instalación y Configuración

### Requisitos Previos

- ☕ **Java 25** o superior
- 🐳 **Docker** (para PostgreSQL con pgvector)
- 📦 **Maven 3.8+**
- 🔑 **API Key de OpenAI**
- 🌐 **Servicios de microservicio externos** desplegados

### 1. Clonar el Repositorio

```bash
git clone <repository-url>
cd bot-service
```

### 2. Configurar PostgreSQL con pgvector

Opción A: **Docker Compose** (Recomendado)

```bash
docker run -d \
  --name bot-postgres \
  -e POSTGRES_PASSWORD=1234 \
  -e POSTGRES_DB=bot_db \
  -p 5433:5432 \
  pgvector/pgvector:pg16
```

Opción B: **PostgreSQL Local**
```sql
-- Instalar extensión pgvector
CREATE EXTENSION vector;
CREATE EXTENSION hstore;
```

### 3. Configurar Variables de Entorno

Crear o editar `src/main/resources/application.properties`:

```properties
# Application
spring.application.name=bot-service
server.port=8090

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5433/bot_db
spring.datasource.username=postgres
spring.datasource.password=1234

# OpenAI API
spring.ai.openai.api-key=<TU_API_KEY>
spring.ai.openai.chat.options.model=gpt-4o
spring.ai.openai.embedding.options.model=text-embedding-ada-002

# WhatsApp (Opcional)
whatsapp.verify-token=<TU_VERIFY_TOKEN>
whatsapp.access-identifier=<TU_ACCESS_TOKEN>
whatsapp.api-url=https://graph.facebook.com/v22.0/

# Seguridad - Encriptación
security.encryption.key=<BASE64_KEY_256_BITS>

# Data Retention
context.data.retention.days=30
cleanup.schedule.cron=0 0 2 * * *
```

### 4. Compilar y Ejecutar

```bash
# Compilar el proyecto
./mvnw clean install

# Ejecutar la aplicación
./mvnw spring-boot:run
```

La aplicación estará disponible en:
- 🌐 **API**: http://localhost:8090
- 📚 **Swagger UI**: http://localhost:8090/swagger-ui.html
- 🔍 **API Docs**: http://localhost:8090/v3/api-docs

---

## 📡 Endpoints Principales

### 1. Chat Endpoints

#### **GET** `/api/chat/ask`
Endpoint simple para consultas directas al bot.

**Parámetros:**
- `message` (String): Mensaje del usuario
- `phoneNumber` (String): Número de teléfono del usuario

**Ejemplo:**
```bash
curl -X GET "http://localhost:8090/api/chat/ask?message=Hola&phoneNumber=525555555555"
```

#### **POST** `/api/chat/whatsapp`
Endpoint webhook para integración con WhatsApp Business API.

**Body:**
```json
{
  "entry": [{
    "changes": [{
      "value": {
        "metadata": {
          "phone_number_id": "123456789"
        },
        "messages": [{
          "from": "525555555555",
          "type": "text",
          "text": {
            "body": "Hola, quisiera información"
          }
        }]
      }
    }]
  }]
}
```

#### **GET** `/api/chat/whatsapp` 
Verificación de webhook de WhatsApp.

**Parámetros:**
- `hub.mode`
- `hub.challenge`
- `hub.verify_token`

### 2. Conversation Management

#### **GET** `/api/conversations/{conversationId}/state`
Obtener el estado actual de una conversación.

**Respuesta:**
```json
{
  "conversationId": "525555555555",
  "currentState": "PAYMENT_PENDING"
}
```

#### **POST** `/api/conversations/{conversationId}/state`
Actualizar el estado de una conversación manualmente.

**Body:**
```json
{
  "state": "PAYMENT_CONFIRMED"
}
```

#### **GET** `/api/conversations/{conversationId}/context`
Obtener todos los datos de contexto almacenados.

**Respuesta:**
```json
{
  "conversationId": "525555555555",
  "context": {
    "customer_id": "123",
    "customer_name": "Juan Pérez",
    "customer_email": "j***@correo.com",
    "order_id": "456",
    "payment_completed": true
  }
}
```

#### **DELETE** `/api/conversations/{conversationId}`
Eliminar una conversación y todos sus datos.

### 3. Document Management

#### **POST** `/api/documents`
Almacenar un nuevo documento en el vector store.

**Body:**
```json
{
  "content": "Contenido del documento...",
  "metadata": {
    "source": "manual",
    "category": "FAQ",
    "title": "Preguntas Frecuentes"
  }
}
```

**Respuesta:**
```json
{
  "success": true,
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Document stored successfully"
}
```

#### **PUT** `/api/documents`
Actualizar un documento existente.

**Body:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Contenido actualizado...",
  "metadata": {
    "updated": true
  }
}
```

#### **DELETE** `/api/documents/{documentId}`
Eliminar un documento del vector store.

---

## 🔄 Sistema de Estados Conversacionales

El bot utiliza una **máquina de estados** para controlar el flujo de la conversación:

### Estados Disponibles

| Estado | Descripción |
|--------|-------------|
| `INITIAL` | Estado inicial de la conversación |
| `CUSTOMER_REGISTRATION` | Registro del cliente |
| `INTENT_SELECTION` | Selección de intención (comprar/portabilidad/rastrear) |
| `PRODUCT_SELECTED` | Producto seleccionado |
| `IMEI_REQUIRED` | Solicitando IMEI del dispositivo |
| `IMEI_VALIDATED` | IMEI validado y compatible |
| `ADDRESS_REQUIRED` | Solicitando dirección de envío |
| `PAYMENT_PENDING` | Esperando confirmación de pago |
| `PAYMENT_CONFIRMED` | Pago confirmado |
| `SIM_SHIPPED` | SIM enviado al cliente |
| `PORTABILITY_WAIT_SIM` | Esperando recepción de SIM |
| `PORTABILITY_NIP_REQUIRED` | Solicitando NIP de portabilidad |
| `PORTABILITY_SIM_ACTIVATION` | Activando SIM |
| `PORTABILITY_IN_PROGRESS` | Proceso de portabilidad en curso |
| `PORTABILITY_COMPLETED` | Portabilidad completada |
| `COMPLETED` | Flujo completado exitosamente |
| `BLOCKED` | Flujo bloqueado (ej: IMEI incompatible) |
| `ABANDONED` | Conversación abandonada |
| `ERROR_STATE` | Estado de error |

### Flujo de Estados

```
INITIAL
  ↓
CUSTOMER_REGISTRATION
  ↓
INTENT_SELECTION
  ↓
PRODUCT_SELECTED
  ↓
IMEI_REQUIRED
  ↓
IMEI_VALIDATED
  ↓
ADDRESS_REQUIRED
  ↓
PAYMENT_PENDING
  ↓
PAYMENT_CONFIRMED
  ↓
SIM_SHIPPED
  ↓
┌─────────────────────┬──────────────────────┐
│ Sin Portabilidad    │  Con Portabilidad    │
│                     │                       │
│ COMPLETED          │ PORTABILITY_WAIT_SIM │
│                     │         ↓             │
│                     │ PORTABILITY_NIP_REQ  │
│                     │         ↓             │
│                     │ PORTABILITY_SIM_ACT  │
│                     │         ↓             │
│                     │ PORTABILITY_PROGRESS │
│                     │         ↓             │
│                     │ PORTABILITY_COMPLETE │
│                     │         ↓             │
│                     │ COMPLETED            │
└─────────────────────┴──────────────────────┘
```

---

## 🛠️ Function Calling Tools

El bot utiliza **5 categorías de herramientas** para realizar operaciones especializadas:

### 1. CustomerTools

**Funciones:**
- `registerCustomer` - Registrar nuevo cliente
- `getCustomerByPhone` - Buscar cliente por teléfono
- `getCustomerByEmail` - Buscar cliente por email
- `getCustomerById` - Buscar cliente por ID

### 2. AddressesTools

**Funciones:**
- `createAddress` - Crear nueva dirección de envío
- `getAddressById` - Obtener dirección por ID

### 3. OrderTools

**Funciones:**
- `createNewOrderForSimCardPurchase` - Crear orden de compra de SIM
- `getOrderById` - Obtener orden por ID
- `getAllOrdersByCustomerId` - Obtener todas las órdenes de un cliente

### 4. PaymentTools

**Funciones:**
- `Create_checkout_session` - Crear sesión de pago en Stripe

### 5. ScraperTools

**Funciones:**
- `scrapeImeiCompatibility` - Validar compatibilidad de IMEI

---

## 💾 Gestión de Contexto

### Almacenamiento Automático

El sistema **extrae y almacena automáticamente** información de las respuestas de las tools mediante el aspecto `ContextStorageAspect`.

### Datos Almacenados por Tool

| Tool | Datos Extraídos |
|------|----------------|
| `registerCustomer` | customer_id, customer_email, customer_phone, customer_name |
| `createAddress` | address_id, address_street, address_district, address_number |
| `createNewOrderForSimCardPurchase` | order_id, order_customer_id, order_product_id |
| `Create_checkout_session` | checkout_session_id, checkout_session_url, payment_completed |
| `scrapeImeiCompatibility` | imei_compatible, imei_compatibility_message |

### Beneficios

✅ **Evita preguntas repetitivas**: El bot no vuelve a preguntar información ya recopilada  
✅ **Experiencia fluida**: Conversaciones más naturales  
✅ **Persistencia**: Los datos se mantienen durante toda la conversación  
✅ **Reutilización**: Uso automático de IDs en llamadas subsecuentes

---

## 🔐 Seguridad

### 1. Sanitización de Logs

**Implementado Activamente** ✅

- Enmascara emails: `juan@correo.com` → `j***@correo.com`
- Enmascara teléfonos: `9461234567` → `946***4567`
- Protege URLs con tokens sensibles
- Oculta NIPs e IMEIs en logs

### 2. Encriptación de Datos Sensibles

**Código Completo - Requiere Activación** ⚠️

- Algoritmo: **AES-256-GCM**
- IV aleatorio por encriptación
- Encripta: `portability_nip`, `portability_imei`, `checkout_session_url`

### 3. Política de Retención

- Limpieza automática de datos sensibles después de **30 días**
- Cron programado: `0 0 2 * * *` (2:00 AM diario)
- Configurable mediante `context.data.retention.days`

---

## 📝 Integración con WhatsApp

### Configuración de Webhook

1. **En el Panel de Facebook Developers:**
   - URL del Webhook: `https://<tu-dominio>/api/chat/whatsapp`
   - Verify Token: Configurar en `whatsapp.verify-token`

2. **Suscribirse a Eventos:**
   - `messages` ✅
   - `messaging_postbacks` ✅

### Buffer de Mensajes

El servicio implementa un **buffer inteligente** que:
- Acumula mensajes durante 8 segundos
- Procesa todos los mensajes juntos
- Evita respuestas fragmentadas
- Mejora la experiencia del usuario

---

## 🔧 Desarrollo

### Compilar el Proyecto

```bash
./mvnw clean compile
```

### Ejecutar Tests

```bash
./mvnw test
```

### Generar JAR

```bash
./mvnw clean package
```

El JAR se generará en: `target/bot-service-0.0.1-SNAPSHOT.jar`

### Ejecutar con Maven

```bash
./mvnw spring-boot:run
```
---

## 📊 Monitoreo y Logs

### Aspectos de Logging

- **ServiceLoggingAspect**: Logging de entrada/salida de servicios
- **ToolExceptionHandlingAspect**: Captura y log de excepciones de tools
- **ContextStorageAspect**: Log de almacenamiento de contexto

---

## 📚 Documentación Adicional

Para información más detallada, consulta los siguientes documentos:

- 📖 [**CONTEXT_MANAGEMENT_GUIDE.md**](CONTEXT_MANAGEMENT_GUIDE.md) - Sistema de gestión de contexto
- 📖 [**CONVERSATION_ORCHESTRATOR_GUIDE.md**](CONVERSATION_ORCHESTRATOR_GUIDE.md) - Orquestador de conversaciones
- 🔒 [**SECURITY_IMPLEMENTATION_STATUS.md**](SECURITY_IMPLEMENTATION_STATUS.md) - Estado de implementación de seguridad
- 💾 [**PERSISTENT_MEMORY_GUIDE.md**](PERSISTENT_MEMORY_GUIDE.md) - Memoria persistente
- 🚀 [**DEPLOYMENT_READY.md**](DEPLOYMENT_READY.md) - Guía de despliegue
- 🛠️ [**TOOL_EXCEPTION_HANDLING.md**](TOOL_EXCEPTION_HANDLING.md) - Manejo de excepciones

---

**Versión**: 0.0.1-SNAPSHOT  
**Última Actualización**: Febrero 2026
