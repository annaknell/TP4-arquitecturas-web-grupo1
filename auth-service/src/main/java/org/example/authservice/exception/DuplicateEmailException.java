package org.example.authservice.exception;

/** Email ya registrado (US-45). Mapea a HTTP 409. */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Ya existe una cuenta registrada con el email: " + email);
    }
}
