package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.dto.ProssimoPagamentoDto;
import com.pagatu.coffee.entity.NuovoPagamentoRequest;
import com.pagatu.coffee.entity.Pagamento;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.event.ProssimoPagamentoEvent;
import com.pagatu.coffee.mapper.PagamentoMapper;
import com.pagatu.coffee.repository.PagamentoRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class PagamentoService {

    @Autowired
    @Lazy
    private PagamentoService self;

    private static final Random RANDOM = new Random();
    private final PagamentoRepository pagamentoRepository;
    private final UtenteRepository utenteRepository;
    private final PagamentoMapper pagamentoMapper;
    private final KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.pagamenti-caffe}")
    private String pagamentiTopic;

    public PagamentoService(PagamentoRepository pagamentoRepository, UtenteRepository utenteRepository,
                            PagamentoMapper pagamentoMapper, KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate) {
        this.pagamentoRepository = pagamentoRepository;
        this.utenteRepository = utenteRepository;
        this.pagamentoMapper = pagamentoMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public PagamentoDto registraPagamento(Long userId, @Valid NuovoPagamentoRequest request) {

        Utente utente = utenteRepository.findByAuthId(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con authId: " + userId));

        log.info("Registrando pagamento per utente: {}", utente.getUsername());

        Pagamento pagamento = new Pagamento();
        pagamento.setUtente(utente);
        pagamento.setImporto(request.getImporto());
        pagamento.setDescrizione(request.getDescrizione());
        pagamento.setDataPagamento(LocalDateTime.now());
        Pagamento savedPagamento = pagamentoRepository.save(pagamento);

        //Mette l'utente in stato PAGATO
        utente.setStatus(Status.PAGATO);
        utenteRepository.save(utente);

        // Determina chi sarà il prossimo a pagare
        ProssimoPagamentoDto prossimoPagamento = self.determinaProssimoPagatore();

        // Pubblica l'evento su Kafka
        ProssimoPagamentoEvent event = new ProssimoPagamentoEvent();
        event.setUltimoPagamentoId(savedPagamento.getId());
        event.setUltimoPagatoreUsername(utente.getUsername());
        event.setUltimoPagatoreEmail(utente.getEmail());
        event.setProssimoUserId(prossimoPagamento.getUserId());
        event.setProssimoUsername(prossimoPagamento.getUsername());
        event.setProssimoEmail(prossimoPagamento.getEmail());
        event.setDataUltimoPagamento(savedPagamento.getDataPagamento());
        event.setImporto(savedPagamento.getImporto());

        kafkaTemplate.send(pagamentiTopic, event);

        log.info("Pagamento registrato: {} - Prossimo pagatore: {}", savedPagamento.getId(), prossimoPagamento.getUsername());

        return pagamentoMapper.toDto(savedPagamento);
    }

    @Transactional
    public ProssimoPagamentoDto determinaProssimoPagatore() {

        //Ottieni gli utenti in stato NON_PAGATO
        List<Utente> utenti_NON_PAGATO = utenteRepository.findAll().stream().filter(utente -> utente.getStatus().equals(Status.NON_PAGATO)).toList();
        log.info("Utenti in stato NON_PAGATO: {}", utenti_NON_PAGATO);

        if (utenti_NON_PAGATO.isEmpty()) {
            List<Utente> utentiResettati = resetUtentiInStatusNonPagato();
            //resetta tutti gli utenti in stato NON_PAGATO e determina il prossimo pagatore
            Utente prossimoPagatore = utentiResettati.get(RANDOM.nextInt(utentiResettati.size()));
            log.info("Tutti hanno già pagato, ricomincio il giro. Prossimo pagatore: {}", prossimoPagatore.getUsername());
            return creaProssimoPagamentoDto(prossimoPagatore);
        } else {
            //determina il prossimo pagatore
            Utente prossimoPagatore = utenti_NON_PAGATO.get(RANDOM.nextInt(utenti_NON_PAGATO.size()));
            log.info("Prossimo pagatore: {}", prossimoPagatore.getUsername());
            return creaProssimoPagamentoDto(prossimoPagatore);
        }
    }

    private ProssimoPagamentoDto creaProssimoPagamentoDto(Utente utente) {
        ProssimoPagamentoDto dto = new ProssimoPagamentoDto();
        dto.setUserId(utente.getId());
        dto.setUsername(utente.getUsername());
        dto.setEmail(utente.getEmail());
        return dto;
    }

    private List<Utente> resetUtentiInStatusNonPagato() {

        List<Utente> utenti = utenteRepository.findAll();

        for (Utente utente : utenti) {
            utente.setStatus(Status.NON_PAGATO);
        }

        utenteRepository.saveAll(utenti);
        return utenti;
    }
}