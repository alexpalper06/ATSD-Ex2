package todolist.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason="Acceso no autorizado a usuarios estandar")
public class AccesoNoAutorizadoException extends RuntimeException {
}
