package com.pagatu.coffee.mapper;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.entity.Pagamento;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Pagamento entity and PagamentoDto objects.
 * This class provides bidirectional mapping functionality to facilitate data transfer
 * between different layers of the Coffee application, particularly between the
 * persistence layer (entities) and the presentation layer (DTOs).
 *
 * <p>The mapper handles null-safe conversions and properly maps nested relationships,
 * including user group membership information when available. This class follows
 * the Data Transfer Object pattern to ensure clean separation between internal
 * entity representations and external API contracts.</p>
 *
 * <p><strong>Note:</strong> When converting from DTO to Entity using {@link #toEntity(PagamentoDto)},
 * the UserGroupMembership relationship is not mapped and must be set separately
 * by the calling service layer.</p
 *
 * @see PagamentoDto
 * @see Pagamento
 */
@Component
public class PagamentoMapper {

    /**
     * Converts a Pagamento entity to a PagamentoDto.
     * This method performs a comprehensive mapping of all entity fields to their
     * corresponding DTO fields, including nested relationship data such as user
     * information from the UserGroupMembership association.
     *
     * <p>The mapping includes:</p>
     * <ul>
     *   <li>Basic payment information (ID, date, amount, description)</li>
     *   <li>User identification data (user ID and username) from the associated membership</li>
     * </ul>
     *
     * <p>If the UserGroupMembership is null, the user-related fields in the DTO
     * will remain null, but the conversion will still proceed successfully.</p>
     *
     * @param pagamento the Pagamento entity to convert, may be null
     * @return PagamentoDto containing the mapped data, or null if input is null
     * @throws NullPointerException if pagamento.getUserGroupMembership().getUtente()
     *         is accessed when the membership exists but the user (Utente) is null
     * @see PagamentoDto
     * @see Pagamento#getUserGroupMembership()
     */
    public PagamentoDto toDto(Pagamento pagamento) {

        if (pagamento == null) {
            return null;
        }

        PagamentoDto dto = new PagamentoDto();
        dto.setId(pagamento.getId());

        if (pagamento.getUserGroupMembership() != null) {
            dto.setUserId(pagamento.getUserGroupMembership().getId());
            dto.setUsername(pagamento.getUserGroupMembership().getUtente().getUsername());
        }

        dto.setDataPagamento(pagamento.getDataPagamento());
        dto.setImporto(pagamento.getImporto());
        dto.setDescrizione(pagamento.getDescrizione());

        return dto;
    }

    /**
     * Converts a PagamentoDto to a Pagamento entity.
     * This method performs a basic mapping of DTO fields to their corresponding
     * entity fields. The conversion focuses on the core payment data and does not
     * handle relationship mapping.
     *
     * <p>The mapping includes:</p>
     * <ul>
     *   <li>Payment ID</li>
     *   <li>Payment date (dataPagamento)</li>
     *   <li>Payment amount (importo)</li>
     *   <li>Payment description (descrizione)</li>
     * </ul>
     *
     * <p><strong>Important:</strong> This method does not map the UserGroupMembership
     * relationship. The calling service layer is responsible for setting this
     * relationship on the returned entity before persisting it to the database.</p>
     *
     * @param dto the PagamentoDto to convert, may be null
     * @return Pagamento entity containing the mapped data, or null if input is null
     * @see PagamentoDto
     * @see Pagamento
     * @apiNote The UserGroupMembership must be set separately by the service layer
     */
    public Pagamento toEntity(PagamentoDto dto) {

        if (dto == null) {
            return null;
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setId(dto.getId());
        pagamento.setDataPagamento(dto.getDataPagamento());
        pagamento.setImporto(dto.getImporto());
        pagamento.setDescrizione(dto.getDescrizione());

        return pagamento;
    }
}