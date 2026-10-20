package com.innowise.academy.authservice.controller;

import com.innowise.academy.authservice.dto.request.RegisterRequest;
import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.entity.Role;
import com.innowise.academy.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvx;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private AuthService service;


    @Test
    void register_shouldReturn201_whenRequestValid() throws Exception {
        //given
        CredentialResponse alex = new CredentialResponse(1L, "alex", Role.USER);
        RegisterRequest registerRequestDto = new RegisterRequest("alex", "122333",
                "Alex", "Alexiiv", LocalDate.of(2020,2,2), "mail@mail.com");

        //when
        when(service.register(registerRequestDto)).thenReturn(alex);

        //then
        mockMvx.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registerRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("alex"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_Throw_whenRequestNotValid() throws Exception {
        //given
        RegisterRequest registerRequestDto = new RegisterRequest("alex", "122333",
                "Alex", "Alexiiv", LocalDate.now(), "mail");


        //then
        mockMvx.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(registerRequestDto)))
                .andExpect(status().isBadRequest());
    }
}