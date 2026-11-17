package org.example.recomendacionesservice.service;

import org.example.recomendacionesservice.entity.Recomendacion;
import org.example.recomendacionesservice.repository.RecomendacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RecomendacionServiceTest {
    @Mock
    private RecomendacionRepository rRepository;

    @InjectMocks
    private RecomendacionService rService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void guardarRecomendacionDeberiaGuardarExitosamente(){
        Recomendacion recomendacion = new Recomendacion(
                "rec123",
                10L,
                List.of("Recital Rock", "Festival Indie"),
                LocalDate.now()
        );

        when(rRepository.save(any(Recomendacion.class))).thenReturn(recomendacion);

        // Act
        Recomendacion resultado = rService.guardarRecomendacion(recomendacion);

        // Assert
        assertNotNull(resultado);
        assertEquals(10L, resultado.getEventoId());
        assertEquals(2, resultado.getEventosSugeridos().size());
        verify(rRepository, times(1)).save(recomendacion);
    }
}
