package com.tudai.pagos.service;

import com.tudai.pagos.repository.PagoRepository;
import org.springframework.stereotype.Service;

@Service
public class PagoService {
    private final PagoRepository pagoRepository;

    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

}
