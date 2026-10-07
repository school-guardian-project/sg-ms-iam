package com.school_guardian.ms_iam.domain.port.out;

import java.util.UUID;

/**
 * Consulta a ms-school-management por gRPC los datos de colegio que ms-iam no
 * posee: la relacion admin-colegio y el catalogo de sedes.
 *
 * <p>Es un puerto de salida porque el dato vive en otro microservicio. La
 * implementacion gRPC esta en infrastructure/grpc.
 *
 * <p>Regla de degradacion: si el servicio remoto no responde, los metodos
 * devuelven null/empty en vez de propagar la excepcion. Un admin debe poder
 * iniciar sesion aunque ms-school-management este caido; lo que pierde es el
 * claim schoolId, no la autenticacion.
 */
public interface SchoolDirectory {

    /**
     * Colegio que administra el perfil, o null si no hay relacion registrada o si
     * ms-school-management no responde.
     */
    AdminSchool findAdminSchool(UUID profileId);

    /**
     * Registra que el perfil administra el colegio. La usa ms-iam justo despues de
     * crear el perfil del admin, porque el Profile.Id lo genera este servicio y
     * ms-school-management no lo podia conocer antes.
     *
     * <p>Devuelve true si la relacion quedo registrada. Devuelve false, sin
     * excepcion, si ms-school-management no responde: el perfil ya esta creado y
     * registrar la relacion es un paso que se puede reintentar, mientras que
     * fallar el alta del usuario dejaria una cuenta sin credenciales.
     */
    boolean linkAdminSchool(UUID profileId, UUID schoolId);

    record AdminSchool(UUID id, String name, UUID cityId) {}
}