package org.example.compraservice.dto;

/** Destinatario único de avisos vinculados a un evento. */
public record CompradorNotificacionResponse(Long clienteId, String email) {}
