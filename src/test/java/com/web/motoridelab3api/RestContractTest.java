package com.web.motoridelab3api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.web.motoridelab3api.controller.MotorcycleController;
import com.web.motoridelab3api.dto.MotorcycleResponse;
import com.web.motoridelab3api.exception.GlobalExceptionHandler;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.service.MotorcycleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

class RestContractTest {
    private MotorcycleService service;
    private MockMvc mvc;
    private static final String JSON =
            """
            {"brand":"Honda","model":"CB","engineCc":500,"horsepower":47,"description":"City bike",
             "price":100.00,"imageUrl":"/images/cb.png","categoryId":1,"technologyIds":[]}
            """;

    @BeforeEach
    void setup() {
        service = mock(MotorcycleService.class);
        mvc =
                MockMvcBuilders.standaloneSetup(new MotorcycleController(service))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    private MotorcycleResponse bike() {
        return new MotorcycleResponse(
                7L,
                "Honda",
                "CB",
                500,
                47,
                null,
                "/images/cb.png",
                null,
                "naked",
                "Naked",
                null,
                "City bike",
                List.of(),
                1L,
                List.of());
    }

    @Test
    void allRequiredMethods() throws Exception {
        when(service.byId(7L)).thenReturn(bike());
        when(service.create(any())).thenReturn(bike());
        when(service.update(eq(7L), any())).thenReturn(bike());
        mvc.perform(get("/api/motorcycles/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
        mvc.perform(head("/api/motorcycles/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        mvc.perform(post("/api/motorcycles").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/motorcycles/7"));
        mvc.perform(put("/api/motorcycles/7").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/motorcycles/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).delete(7L);
    }

    @Test
    void validatesAndReportsMissingHead() throws Exception {
        mvc.perform(
                        post("/api/motorcycles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON.replace("500", "-1")))
                .andExpect(status().isBadRequest());
        doThrow(new ResourceNotFoundException("missing")).when(service).requireExists(99L);
        mvc.perform(head("/api/motorcycles/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}
