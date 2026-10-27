# sg-ms-iam

## Login and refresh persistence

Login atomically creates the session history and stores the SHA-256 hash of its
refresh token in `Iam.RefreshToken`. No raw refresh token is stored. Refresh JWTs
have a unique `jti`, including when issued within the same second. Rotation saves
the replacement and revokes the old token; reuse rejection commits the session
revocation rather than rolling it back with the expected invalid-token exception.
A request without a refresh cookie/token still returns 401.

The database must include `Iam.RefreshToken`. For an older database missing it,
apply the existing database repository scripts in order:
`ms-iam-db/01-ddl/03-tables/007-create-table-refresh-token.sql`,
`ms-iam-db/01-ddl/04-alter/007-add-fk-refresh-token.sql`, and
`ms-iam-db/01-ddl/09-indexes/003-idx-table-refresh-token.sql`.

Focused regressions:

```shell
./mvnw -Dtest=LoginServiceTest,RefreshTokenServiceTest,JwtRefreshTokenTest,RefreshTokenPersistenceTest test
```

`GatewayAuthenticationIntegrationTest` is opt-in. Set `GATEWAY_E2E_TESTS=true`,
`E2E_DATABASE_URL`, `MSSQL_USER`, `MSSQL_SA_PASSWORD`, and `E2E_GATEWAY_URL` only in
an isolated development environment, then run it with `-Dtest=GatewayAuthenticationIntegrationTest`.
It creates a random temporary SuperAdmin fixture, tests real login, consecutive
refreshes, protected routes and replay rejection through Kong. It then assigns
the fixture the Admin role and an existing school, verifies school isolation,
type filtering and search, and deletes its own school assignment, tokens,
sessions, profile and person in `finally`. It requires alerts in at least two
existing schools and never sends email or SMS.

Configure persistent `JWT_PRIVATE_KEY` (base64 PKCS#8) and `JWT_PUBLIC_KEY`
(base64 SPKI) in the private environment before rebuilding IAM. The current
`RsaKeyProvider` rejects missing keys; do not rely on an older image generating
ephemeral signing keys. Never commit signing keys or share them in logs.

## Relacion entre perfiles y escuelas

- Administradores: `Iam.Profile.Id -> School.SchoolAdmin.ProfileId -> School.School.Id`.
  Se conserva la relacion existente, unica por perfil, y se actualiza mediante
  `admin.created` y `admin.school.updated`. Este ultimo topic usa un consumidor
  tipado como `AdminSchoolUpdatedEvent`, no como `PersonCreatedEvent`.
- Estudiantes, conductores y acudientes: `Iam.Profile.CampuseId ->
  School.SchoolCampus.Id -> School.SchoolCampus.SchoolId`. Los eventos de registro
  transportan `CampusId`; JPA lo guarda en la columna existente `CampuseId`.
- Login, refresh y `/api/v1/auth/profile` resuelven la escuela con la asignacion
  administrativa o con la sede. `campusId` y `schoolId` no son intercambiables.
  El identificador de perfil del JWT es su `sub`.

## Integracion con recuperacion

Las rutas `/api/profiles/**` son internas y exigen `X-Internal-Api-Key`. Compose
traduce `INTERNAL_API_KEY` del `.env` privado a `APP_INTERNAL_API_KEY`, usada como
`app.internal.api-key`. La clave debe coincidir con la de `sg-ms-forgot-information`.
Sin clave o con una incorrecta, estas rutas responden 401; no se habilitan publicamente.

Se ofrecen busqueda de perfil activo por correo, actualizacion de contrasena usando
el encoder BCrypt de IAM, cambio de correo y comprobacion/actualizacion de telefono.
El telefono se recibe en E.164 y se guarda sin el prefijo configurado en
`PHONE_DEFAULT_COUNTRY_CODE` (57 por defecto).
