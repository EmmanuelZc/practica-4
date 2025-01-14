# Proyecto: Aplicaciones Móviles Nativas - API de Autenticación

Este proyecto es una API desarrollada en **Spring Boot** que proporciona funcionalidades básicas de autenticación y gestión de usuarios, incluyendo roles y sincronización de datos.

---

## 🚀 Características

- **Autenticación de usuarios**:
  - Verificación de credenciales con contraseñas cifradas usando `BCrypt`.
  - Endpoint para iniciar sesión con validación de usuario y contraseña.
  
- **Gestión de usuarios**:
  - Recuperación de datos de usuario con roles asociados.
  - Almacenamiento y sincronización de usuarios locales y remotos.

- **Seguridad**:
  - Filtros de seguridad configurados con Spring Security.
  - Protección contra CSRF y otros ataques comunes.

---

## 📋 Requisitos previos

Antes de ejecutar este proyecto, asegúrate de tener instalado lo siguiente:

1. **Java** (versión 21 o superior).
2. **Maven** o **Gradle** para la construcción del proyecto.
3. **MySQL** como base de datos (puedes usar cualquier base de datos compatible con JPA/Hibernate).
4. Un cliente API como **Postman** para probar los endpoints.

---

2. Configura la base de datos
Edita el archivo application.properties o application.yml para configurar la conexión a tu base de datos MySQL:

properties
Copiar código


spring.datasource.url=jdbc:mysql://localhost:3306/tu_base_datos
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true



📚 Documentación de Endpoints
Autenticación de Usuarios
POST /api/auth

Descripción: Autentica a un usuario con su nombre de usuario y contraseña.
Parámetros:
usuario (string): Nombre de usuario.
password (string): Contraseña del usuario.
Respuesta:
200 OK: Devuelve un JSON con la información del usuario autenticado.
401 Unauthorized: Credenciales inválidas.
500 Internal Server Error: Error interno del servidor.


EJEMPLO DE SOLICITUD
POST /api/auth?usuario=esurita1&password=12345678 HTTP/1.1
Host: localhost:8080

🛠️ Estructura del Proyecto
data/models: Modelos de datos (clases para JPA y DTOs).
data/api: Clientes Retrofit para comunicación con API remotas.
ui/theme: Interfaces para interacción con usuarios (frontend móvil).
sync: Lógica para sincronización de usuarios locales/remotos.

