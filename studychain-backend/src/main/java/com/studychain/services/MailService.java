package com.studychain.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${admin.email:}")
    private String adminEmail;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendToAdmin(String subject, String body, @Nullable String replyTo) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender bean not available; skipping email send. Subject: {}", subject);
            return;
        }
        try {
            if (adminEmail == null || adminEmail.isBlank()) {
                log.warn("Admin email is not configured; skipping email send.");
                return;
            }
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(adminEmail);
            msg.setSubject(subject);
            msg.setText(body);
            if (replyTo != null && !replyTo.isBlank()) {
                msg.setReplyTo(replyTo);
            }
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send admin email: {}", e.getMessage());
        }
    }
}


