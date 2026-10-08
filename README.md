# sg-ms-iam

## Integracion con recuperacion

Las rutas `/api/profiles/**` son internas y exigen `X-Internal-Api-Key`. Compose
traduce `INTERNAL_API_KEY` del `.env` privado a `APP_INTERNAL_API_KEY`, usada como
`app.internal.api-key`. La clave debe coincidir con la de `sg-ms-forgot-information`.
Sin clave o con una incorrecta, estas rutas responden 401; no se habilitan publicamente.

Se ofrecen busqueda de perfil activo por correo, actualizacion de contrasena usando
el encoder BCrypt de IAM, cambio de correo y comprobacion/actualizacion de telefono.
El telefono se recibe en E.164 y se guarda sin el prefijo configurado en
`PHONE_DEFAULT_COUNTRY_CODE` (57 por defecto).

Los errores esperados 400, 404 y 409 se responden directamente desde el manejador
de excepciones, sin reenviar a `/error` y convertirlos en un 403 de seguridad.
Se verificaron la consulta de un perfil existente (200), la proteccion sin clave
(401), cuentas inexistentes (404), validacion (400) y 13 pruebas automatizadas.
Las pruebas de integracion no enviaron correos/SMS ni cambiaron cuentas reales.