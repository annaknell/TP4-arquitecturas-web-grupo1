package com.tudai.pagos.service;

import com.tudai.pagos.repository.ReintegroRepository;
import org.springframework.stereotype.Service;

@Service
public class ReintegroService {
    private final ReintegroRepository reintegroRepository;

    public ReintegroService(ReintegroRepository reintegroRepository) {
        this.reintegroRepository = reintegroRepository;
    }
}
