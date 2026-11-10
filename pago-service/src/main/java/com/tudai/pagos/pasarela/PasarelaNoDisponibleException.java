package com.tudai.pagos.pasarela;

public class PasarelaNoDisponibleException extends RuntimeException{
    public PasarelaNoDisponibleException(String mensaje){
        super(mensaje);
    }
}
