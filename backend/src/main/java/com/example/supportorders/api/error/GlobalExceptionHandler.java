package com.example.supportorders.api.error;

import com.example.supportorders.application.DuplicateActiveOrderException;
import com.example.supportorders.application.SupportOrderNotFoundException;
import com.example.supportorders.domain.InvalidStatusTransitionException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce excepciones a respuestas RFC 7807 (application/problem+json) con mensajes claros.
 * Los errores controlados se registran como WARN sin el cuerpo de la petición; los inesperados como ERROR.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SupportOrderNotFoundException.class)
    ProblemDetail handleNotFound(SupportOrderNotFoundException ex, HttpServletRequest request) {
        log.warn("Support order not found: {} {}", request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.NOT_FOUND, "Orden no encontrada", ex.getMessage());
    }

    @ExceptionHandler(DuplicateActiveOrderException.class)
    ProblemDetail handleDuplicate(DuplicateActiveOrderException ex) {
        log.warn("Rejected duplicate active order: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Orden activa duplicada", ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex, HttpServletRequest request) {
        log.warn("Rejected status transition {} -> {} on {}", ex.getFrom(), ex.getTo(), request.getRequestURI());
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Transición de estado no permitida", ex.getMessage());
        problem.setProperty("currentStatus", ex.getFrom());
        problem.setProperty("requestedStatus", ex.getTo());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Concurrent update rejected on {}", request.getRequestURI());
        return problem(HttpStatus.CONFLICT, "Modificación concurrente",
                "La orden fue modificada por otra petición. Consulta su estado actual y reintenta.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid parameter '{}'", ex.getName());
        return problem(HttpStatus.BAD_REQUEST, "Parámetro inválido",
                "Valor inválido para '" + ex.getName() + "'" + allowedValues(ex.getRequiredType()));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado.");
    }

    /** Errores de Bean Validation: devuelve un mapa campo -> mensaje. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        log.warn("Validation failed for fields {}", errors.keySet());

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Uno o más campos no son válidos.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** JSON mal formado o valores de enum desconocidos (p. ej. "priority": "URGENT"). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String detail = "El cuerpo de la petición es obligatorio y debe ser un JSON válido.";
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", detail);

        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String fieldName = mismatch.getPath().get(mismatch.getPath().size() - 1).getFieldName();
            String field = fieldName != null ? fieldName : "body";
            Class<?> type = mismatch instanceof InvalidFormatException invalid ? invalid.getTargetType() : null;
            String message = "valor inválido" + allowedValues(type);
            problem.setDetail("Uno o más campos no son válidos.");
            problem.setProperty("errors", Map.of(field, message));
        }
        log.warn("Unreadable request body ({})", ex.getMostSpecificCause().getClass().getSimpleName());
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private static String allowedValues(Class<?> type) {
        if (type == null || !type.isEnum()) {
            return "";
        }
        return ". Valores permitidos: " + Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
