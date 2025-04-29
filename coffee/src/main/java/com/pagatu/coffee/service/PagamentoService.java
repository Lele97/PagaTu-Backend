package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.dto.ProssimoPagamentoDto;
import com.pagatu.coffee.entity.NuovoPagamentoRequest;
import com.pagatu.coffee.entity.Pagamento;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.event.ProssimoPagamentoEvent;
import com.pagatu.coffee.mapper.PagamentoMapper;
import com.pagatu.coffee.repository.PagamentoRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagamentoService {
    private final PagamentoRepository pagamentoRepository;
    private final UtenteRepository utenteRepository;
    private final PagamentoMapper pagamentoMapper;
    private final KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate;

    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            UtenteRepository utenteRepository,
            PagamentoMapper pagamentoMapper,
            KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate) {
        this.pagamentoRepository = pagamentoRepository;
        this.utenteRepository = utenteRepository;
        this.pagamentoMapper = pagamentoMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public PagamentoDto registraPagamento(Long userId, NuovoPagamentoRequest request) {
        Utente utente = utenteRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        Pagamento pagamento = new Pagamento();
        pagamento.setUserId(userId);
        pagamento.setUsername(utente.getUsername());
        pagamento.setImporto(request.getImporto());
        pagamento.setDescrizione(request.getDescrizione());
        pagamento.setDataPagamento(LocalDateTime.now());

        Pagamento savedPagamento = pagamentoRepository.save(pagamento);

        // Determina chi è il prossimo a pagare
        Utente prossimo = determinaProssimoPagatore(utente);

        // Pubblica un evento Kafka per notificare l'email service
        ProssimoPagamentoEvent event = new ProssimoPagamentoEvent();
        event.setUltimoPagamentoId(savedPagamento.getId());
        event.setUltimoPagatoreUsername(utente.getUsername());
        event.setUltimoPagatoreEmail(utente.getEmail());
        event.setProssimoUserId(prossimo.getId());
        event.setProssimoUsername(prossimo.getUsername());
        event.setProssimoEmail(prossimo.getEmail());
        event.setDataUltimoPagamento(savedPagamento.getDataPagamento());
        event.setImporto(savedPagamento.getImporto());

        kafkaTemplate.send("pagamenti-caffe", event);

        return pagamentoMapper.toDto(savedPagamento);
    }

    private Utente determinaProssimoPagatore(Utente ultimo) {
        List<Utente> utentiAttivi = utenteRepository.findByAttivoTrue();

        if (utentiAttivi.size() <= 1) {
            return ultimo;
        }

        int currentIndex = utentiAttivi.indexOf(ultimo);
        int nextIndex = (currentIndex + 1) % utentiAttivi.size();

        return utentiAttivi.get(nextIndex);
    }

    public List<PagamentoDto> getUltimiPagamenti() {
        return pagamentoRepository.findTop10ByOrderByDataPagamentoDesc()
                .stream()
                .map(pagamentoMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProssimoPagamentoDto getProssimoPagatore() {
        Pagamento ultimoPagamento = pagamentoRepository.findTopByOrderByDataPagamentoDesc()
                .orElse(null);

        if (ultimoPagamento == null) {
            // Se non ci sono pagamenti, prendi il primo utente attivo
            Utente primo = utenteRepository.findByAttivoTrue().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Nessun utente attivo trovato"));

            return new ProssimoPagamentoDto(primo.getId(), primo.getUsername(), primo.getEmail());
        }

        Utente ultimoPagatore = utenteRepository.findById(ultimoPagamento.getUserId())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        Utente prossimo = determinaProssimoPagatore(ultimoPagatore);

        return new ProssimoPagamentoDto(prossimo.getId(), prossimo.getUsername(), prossimo.getEmail());
    }
}
