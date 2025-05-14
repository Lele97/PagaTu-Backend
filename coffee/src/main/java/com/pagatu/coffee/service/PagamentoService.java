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
import java.util.stream.Collectors;

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

        // Determina chi sarà il prossimo a pagare
        ProssimoPagamentoDto prossimoPagamento = self.determinaProssimoPagatore(utente);

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

    public List<PagamentoDto> getUltimiPagamenti() {
        List<Pagamento> pagamenti = pagamentoRepository.findTop10ByOrderByDataPagamentoDesc();
        return pagamenti.stream()
                .map(pagamentoMapper::toDto)
                .toList();
    }

    public ProssimoPagamentoDto getProssimoPagatore() {

        // Ottieni tutti gli utenti attivi
        List<Utente> utentiAttivi = utenteRepository.findByAttivoTrue();

        if (utentiAttivi.isEmpty()) {
            throw new RuntimeException("Nessun utente attivo trovato");
        }

        // Trova gli utenti che non hanno mai pagato
        List<Utente> utentiSenzaPagamenti = trovaUtentiSenzaPagamenti(utentiAttivi);

        // Se tutti hanno pagato, ricomincia il giro
        if (utentiSenzaPagamenti.isEmpty()) {
            log.info("Tutti gli utenti hanno già pagato almeno una volta, si ricomincia il giro");
            // Ottieni l'ultimo pagamento
            List<Pagamento> ultimiPagamenti = pagamentoRepository.findTop1ByOrderByDataPagamentoDesc();

            if (!ultimiPagamenti.isEmpty()) {
                Utente ultimoPagatore = ultimiPagamenti.get(0).getUtente();

                // Escludiamo l'ultimo pagatore se possibile
                List<Utente> altriUtenti = utentiAttivi.stream()
                        .filter(u -> !u.getId().equals(ultimoPagatore.getId()))
                        .toList();

                if (!altriUtenti.isEmpty()) {

                    // Scegli un utente casuale tra gli altri
                    Utente prossimoPagatore = altriUtenti.get(RANDOM.nextInt(altriUtenti.size()));
                    return creaProssimoPagamentoDto(prossimoPagatore);
                }
            }

            // Se non ci sono pagamenti precedenti o c'è un solo utente, scegli un utente casuale
            Utente prossimoPagatore = utentiAttivi.get(RANDOM.nextInt(utentiAttivi.size()));
            return creaProssimoPagamentoDto(prossimoPagatore);
        } else {

            // Scegli un utente casuale tra quelli che non hanno ancora pagato
            Utente prossimoPagatore = utentiSenzaPagamenti.get(RANDOM.nextInt(utentiSenzaPagamenti.size()));
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

    private List<Utente> trovaUtentiSenzaPagamenti(List<Utente> utentiAttivi) {

        // Ottieni tutti gli utenti che hanno effettuato almeno un pagamento
        List<Utente> utentiConPagamenti = pagamentoRepository.findDistinctUtente();

        // Trova gli utenti che non hanno mai pagato
        return utentiAttivi.stream()
                .filter(u -> utentiConPagamenti.stream().noneMatch(p -> p.getId().equals(u.getId())))
                .toList();
    }

    @Transactional
    public ProssimoPagamentoDto determinaProssimoPagatore(Utente ultimoPagatore) {

        // Ottieni tutti gli utenti attivi
        List<Utente> utentiAttivi = utenteRepository.findByAttivoTrue();

        // Trova gli utenti che non hanno mai pagato (escluso l'ultimo pagatore)
        List<Utente> utentiSenzaPagamenti = trovaUtentiSenzaPagamenti(utentiAttivi).stream()
                .filter(u -> !u.getId().equals(ultimoPagatore.getId()))
                .toList();

        log.info("Utenti senza pagamenti: {}", utentiSenzaPagamenti.stream().map(Utente::getUsername).collect(Collectors.joining(", ")));

        // Se ci sono utenti che non hanno mai pagato, scegliamo tra loro
        if (!utentiSenzaPagamenti.isEmpty()) {
            Utente prossimoPagatore = utentiSenzaPagamenti.get(RANDOM.nextInt(utentiSenzaPagamenti.size()));
            log.info("Selezionato prossimo pagatore che non ha mai pagato: {}", prossimoPagatore.getUsername());
            return creaProssimoPagamentoDto(prossimoPagatore);
        }

        // Se tutti hanno pagato, ricomincia il giro ma esclude l'ultimo pagatore
        List<Utente> altriUtenti = utentiAttivi.stream()
                .filter(u -> !u.getId().equals(ultimoPagatore.getId()))
                .toList();

        // Se non ci sono altri utenti (solo uno attivo), l'unico pagherà di nuovo
        if (altriUtenti.isEmpty()) {
            log.info("Solo un utente attivo, dovrà pagare di nuovo: {}", ultimoPagatore.getUsername());
            return creaProssimoPagamentoDto(ultimoPagatore);
        }

        // Scegli un utente casuale tra gli altri
        Utente prossimoPagatore = altriUtenti.get(RANDOM.nextInt(altriUtenti.size()));
        log.info("Tutti hanno già pagato, ricomincio il giro. Prossimo pagatore: {}", prossimoPagatore.getUsername());
        return creaProssimoPagamentoDto(prossimoPagatore);
    }
}