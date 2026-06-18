package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Payment;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.exception.NoContentAvailableException;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroupBalanceService {

    private final BaseUserService baseUserService;
    private final PaymentRepository paymentRepository;
    private final UserGroupMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public GroupBalanceDto calculateBalance(Long userId, GroupBalanceRequest request) {
        CoffeeUser requester = baseUserService.findUserByAuthId(userId);
        Group group = baseUserService.findGroupByName(request.getGroupName());

        if (!membershipRepository.existsByCoffeeUserAndGroup(requester, group)) {
            throw new ForbiddenException("Non sei autorizzato a visualizzare il bilancio di questo gruppo");
        }

        List<Payment> payments = paymentRepository.findAllByGroupName(request.getGroupName());
        if (payments.isEmpty()) {
            throw new NoContentAvailableException("Non ci sono pagamenti per calcolare il bilancio");
        }

        List<UserGroupMembership> memberships = membershipRepository.findByGroup(group);
        int memberCount = memberships.size();
        double totalSpent = payments.stream().mapToDouble(Payment::getAmount).sum();
        double fairShare = memberCount > 0 ? totalSpent / memberCount : 0;

        Map<String, Double> paidByUser = new HashMap<>();
        for (Payment payment : payments) {
            String payer = payment.getUserGroupMembership().getCoffeeUser().getUsername();
            paidByUser.merge(payer, payment.getAmount(), Double::sum);
        }

        List<MemberBalanceDto> memberBalances = new ArrayList<>();
        for (UserGroupMembership membership : memberships) {
            CoffeeUser user = membership.getCoffeeUser();
            double totalPaid = paidByUser.getOrDefault(user.getUsername(), 0.0);
            memberBalances.add(new MemberBalanceDto(
                    user.getUsername(),
                    round(totalPaid),
                    round(fairShare),
                    round(totalPaid - fairShare),
                    user.getSatispayLink(),
                    user.getRevolutLink()
            ));
        }

        List<PairwiseDebtDto> pairwiseDebts = new ArrayList<>();
        for (Payment payment : payments) {
            if (payment.getBeneficiaryUsername() != null && !payment.getBeneficiaryUsername().isBlank()) {
                String creditor = payment.getUserGroupMembership().getCoffeeUser().getUsername();
                CoffeeUser creditorUser = payment.getUserGroupMembership().getCoffeeUser();
                pairwiseDebts.add(new PairwiseDebtDto(
                        payment.getBeneficiaryUsername(),
                        creditor,
                        round(payment.getAmount()),
                        creditorUser.getSatispayLink(),
                        creditorUser.getRevolutLink()
                ));
            }
        }

        return new GroupBalanceDto(
                request.getGroupName(),
                round(totalSpent),
                memberCount,
                memberBalances,
                pairwiseDebts
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}