package com.pagatu.coffee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagatu.coffee.dto.PaymentDto;
import com.pagatu.coffee.entity.NewPaymentRequest;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @Test
    void registerPayment_acceptsGroupNameParam() throws Exception {
        when(jwtService.extractUserIdFromAuthHeader(anyString())).thenReturn(42L);
        when(paymentService.registerPayment(eq(42L), eq("Caffe Team"), any(NewPaymentRequest.class)))
                .thenReturn(new PaymentDto());

        NewPaymentRequest body = new NewPaymentRequest();
        body.setAmount(2.5);

        mockMvc.perform(post("/api/coffee/pagamento")
                        .param("groupName", "Caffe Team")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(paymentService).registerPayment(eq(42L), eq("Caffe Team"), any(NewPaymentRequest.class));
    }

    @Test
    void registerPayment_missingGroupName_rejectsRequest() throws Exception {
        NewPaymentRequest body = new NewPaymentRequest();
        body.setAmount(2.5);

        mockMvc.perform(post("/api/coffee/pagamento")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));

        verify(paymentService, never()).registerPayment(anyLong(), anyString(), any());
    }

    @Test
    void skipPayment_acceptsGroupNameParam() throws Exception {
        when(jwtService.extractUserIdFromAuthHeader(anyString())).thenReturn(42L);

        mockMvc.perform(post("/api/coffee/salta/pagamento")
                        .param("groupName", "Caffe Team")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());

        verify(paymentService).skipPayment(42L, "Caffe Team");
    }

    @Test
    void payFor_acceptsGroupNameParam() throws Exception {
        when(jwtService.extractUserIdFromAuthHeader(anyString())).thenReturn(42L);
        when(paymentService.payFor(eq(42L), eq("Caffe Team"), any(NewPaymentRequest.class)))
                .thenReturn(new PaymentDto());

        NewPaymentRequest body = new NewPaymentRequest();
        body.setAmount(3.0);

        mockMvc.perform(post("/api/coffee/pagamento/pagaPer")
                        .param("groupName", "Caffe Team")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(paymentService).payFor(eq(42L), eq("Caffe Team"), any(NewPaymentRequest.class));
    }
}