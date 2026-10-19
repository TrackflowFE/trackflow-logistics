package com.trackflow.bootstrap.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validación de las sesiones (HU-08) en la tabla auth_sessions, que comparten las tres
 * APIs. Las sesiones las abre y las cierra la API de Envíos y acceso (login y logout);
 * esta API solo comprueba que sigan vigentes y registra la actividad, para que quien
 * trabaja registrando movimientos no quede fuera por inactividad mientras lo hace.
 */
@Service
@RequiredArgsConstructor
public class Sesiones {

    private final SesionRepository repositorio;
    private final UsuarioRepository usuarios;
    private final SecurityProperties propiedades;
    private final Clock clock;

    /**
     * Si la sesión del token sigue vigente, registra la actividad y devuelve true.
     * Una sesión inactiva o de un usuario desactivado se cierra en el acto, para que
     * no pueda volver a usarse aunque el token aún no haya vencido.
     */
    @Transactional
    public boolean validarYRenovar(String jti) {
        if (jti == null) {
            return false;
        }
        Optional<Sesion> encontrada = repositorio.findById(jti);
        if (encontrada.isEmpty() || !encontrada.get().abierta()) {
            return false;
        }
        Sesion sesion = encontrada.get();
        Instant ahora = clock.instant();
        boolean usuarioActivo = usuarios.findById(sesion.getUsername()).map(Usuario::isActive).orElse(false);
        if (!usuarioActivo || sesion.inactivaDesde(ahora, propiedades.inactividad())) {
            sesion.cerrar(ahora);
            return false;
        }
        sesion.registrarActividad(ahora);
        return true;
    }
}
