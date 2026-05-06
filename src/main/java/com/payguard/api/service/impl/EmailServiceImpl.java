package com.payguard.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.payguard.api.entity.Escrow;
import com.payguard.api.entity.EscrowParticipant;
import com.payguard.api.entity.User;
import com.payguard.api.entity.enumeration.ParticipantRole;
import com.payguard.api.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final TemplateEngine templateEngine;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://app.payguard.ng}")
    private String frontendUrl;

    @Value("${spring.mail.from:${EMAIL_HOST_USER}}")
    private String fromEmail;

    @Async
    @Override
    public void sendEscrowInviteEmails(Escrow escrow, User creator) {
        List<EscrowParticipant> participants = escrow.getParticipants();

        EscrowParticipant creatorParticipant = participants.stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(creator.getId()))
                .findFirst()
                .orElse(null);

        if (creatorParticipant != null) {
            Context creatorCtx = buildCreatorContext(creatorParticipant, escrow, participants);
            String subject = "Your Escrow Transaction Has Been Created";
            sendHtmlEmail(creatorParticipant.getEmail(), subject, "escrow-email-creator", creatorCtx);
            log.info("Sent escrow creator confirmation to {} for escrow {}", creatorParticipant.getEmail(), escrow.getId());
        } else {
            log.warn("No creator participant found for escrow {}. Skipping creator confirmation email.", escrow.getId());
        }

        List<EscrowParticipant> uninvited = participants.stream()
                .filter(p -> Boolean.FALSE.equals(p.getInviteAccepted()))
                .toList();

        for (EscrowParticipant invitee : uninvited) {
            Context inviteCtx = buildInviteContext(invitee, creatorParticipant, escrow);
            String roleLabel = toRoleLabel(invitee.getRole());
            String subject = "You've Been Invited to an Escrow Transaction as " + roleLabel;
            sendHtmlEmail(invitee.getEmail(), subject, "escrow-email-invite", inviteCtx);
            log.info("Sent escrow invite to {} ({}) for escrow {}", invitee.getEmail(), roleLabel, escrow.getId());
        }
    }

    private Context buildCreatorContext(EscrowParticipant creator, Escrow escrow,
                                        List<EscrowParticipant> allParticipants) {
        String viewLink = frontendUrl + "/escrow/" + escrow.getId() + "/dashboard";

        long invitedCount = allParticipants.stream()
                .filter(p -> Boolean.FALSE.equals(p.getInviteAccepted()))
                .count();
        String nextStep = switch ((int) invitedCount) {
            case 0 -> "All participants have been confirmed. The escrow is now active.";
            case 1 -> "We've sent an invitation to the other party. The escrow will proceed once they accept.";
            default -> "We've sent invitations to " + invitedCount + " participants. The escrow will proceed once they all accept.";
        };

        Context ctx = new Context();
        ctx.setVariable("creatorName", creator.getName() != null ? creator.getName() : "there");
        ctx.setVariable("escrowTitle", escrow.getTitle());
        ctx.setVariable("transactionId", escrow.getId());
        ctx.setVariable("amount", escrow.getAmount());
        ctx.setVariable("status", escrow.getStatus());
        ctx.setVariable("viewLink", viewLink);
        ctx.setVariable("nextStepMessage", nextStep);
        return ctx;
    }

    private Context buildInviteContext(EscrowParticipant invitee, EscrowParticipant creator, Escrow escrow) {
        String inviteLink = frontendUrl + "/invite/" + invitee.getInviteToken();

        String creatorName = (creator != null && creator.getName() != null)
                ? creator.getName()
                : escrow.getCreatedBy().getName();

        Context ctx = new Context();
        ctx.setVariable("recipientName", invitee.getName() != null ? invitee.getName() : "there");
        ctx.setVariable("recipientRole", toRoleLabel(invitee.getRole()));
        ctx.setVariable("creatorName", creatorName);
        ctx.setVariable("escrowTitle", escrow.getTitle());
        ctx.setVariable("transactionId", escrow.getId());
        ctx.setVariable("amount", escrow.getAmount());
        ctx.setVariable("status", escrow.getStatus());
        ctx.setVariable("acceptanceLink", inviteLink);
        return ctx;
    }

    private void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        String htmlContent = templateEngine.process(templateName, context);
        String uniqueId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + "-" + UUID.randomUUID().toString().substring(0, 5);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject + " (" + uniqueId + ")");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent via SMTP to [{}] — Message ID: {}", to, uniqueId);
        } catch (MessagingException e) {
            log.error("Failed to send email via SMTP to [{}]: {}", to, e.getMessage(), e);
        }
    }

    private String toRoleLabel(ParticipantRole role) {
        if (role == null) return "Participant";
        return switch (role) {
            case BUYER  -> "Buyer";
            case SELLER -> "Seller";
            case AGENT  -> "Agent";
        };
    }
}
