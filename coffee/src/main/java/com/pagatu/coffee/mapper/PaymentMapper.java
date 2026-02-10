package com.pagatu.coffee.mapper;

import com.pagatu.coffee.dto.PaymentDto;
import com.pagatu.coffee.entity.Payment;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Payment entity and PaymentDto objects.
 * This class provides bidirectional mapping functionality to facilitate data
 * transfer
 * between different layers of the Coffee application, particularly between the
 * persistence layer (entities) and the presentation layer (DTOs).
 *
 * <p>
 * The mapper handles null-safe conversions and properly maps nested
 * relationships,
 * including user group membership information when available. This class
 * follows
 * the Data Transfer Object pattern to ensure clean separation between internal
 * entity representations and external API contracts.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> When converting from DTO to Entity using
 * {@link #toEntity(PaymentDto)},
 * the UserGroupMembership relationship is not mapped and must be set separately
 * by the calling service layer.
 * </p>
 *
 * @see PaymentDto
 * @see Payment
 */
@Component
public class PaymentMapper {

    /**
     * Converts a Payment entity to a PaymentDto.
     * This method performs a comprehensive mapping of all entity fields to their
     * corresponding DTO fields, including nested relationship data such as user
     * information from the UserGroupMembership association.
     *
     * <p>
     * The mapping includes:
     * </p>
     * <ul>
     * <li>Basic payment information (ID, date, amount, description)</li>
     * <li>User identification data (user ID and username) from the associated
     * membership</li>
     * </ul>
     *
     * <p>
     * If the UserGroupMembership is null, the user-related fields in the DTO
     * will remain null, but the conversion will still proceed successfully.
     * </p>
     *
     * @param payment the Payment entity to convert, may be null
     * @return PaymentDto containing the mapped data, or null if input is null
     * @throws NullPointerException if
     *                              payment.getUserGroupMembership().getCoffeeUser()
     *                              is accessed when the membership exists but the
     *                              user is null
     * @see PaymentDto
     * @see Payment#getUserGroupMembership()
     */
    public PaymentDto toDto(Payment payment) {

        if (payment == null) {
            return null;
        }

        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());

        if (payment.getUserGroupMembership() != null && payment.getUserGroupMembership().getCoffeeUser() != null) {
            dto.setUserId(payment.getUserGroupMembership().getCoffeeUser().getId());
            dto.setUsername(payment.getUserGroupMembership().getCoffeeUser().getUsername());
        }

        dto.setPaymentDate(payment.getPaymentDate());
        dto.setAmount(payment.getAmount());
        dto.setDescription(payment.getDescription());

        return dto;
    }

    /**
     * Converts a PaymentDto to a Payment entity.
     * This method performs a basic mapping of DTO fields to their corresponding
     * entity fields. The conversion focuses on the core payment data and does not
     * handle relationship mapping.
     *
     * <p>
     * The mapping includes:
     * </p>
     * <ul>
     * <li>Payment ID</li>
     * <li>Payment date</li>
     * <li>Payment amount</li>
     * <li>Payment description</li>
     * </ul>
     *
     * <p>
     * <strong>Important:</strong> This method does not map the UserGroupMembership
     * relationship. The calling service layer is responsible for setting this
     * relationship on the returned entity before persisting it to the database.
     * </p>
     *
     * @param dto the PaymentDto to convert, may be null
     * @return Payment entity containing the mapped data, or null if input is null
     * @see PaymentDto
     * @see Payment
     * @apiNote The UserGroupMembership must be set separately by the service layer
     */
    public Payment toEntity(PaymentDto dto) {

        if (dto == null) {
            return null;
        }

        Payment payment = new Payment();
        payment.setId(dto.getId());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setAmount(dto.getAmount());
        payment.setDescription(dto.getDescription());

        return payment;
    }
}
