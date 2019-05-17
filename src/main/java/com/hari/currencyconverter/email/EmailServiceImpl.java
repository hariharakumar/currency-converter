package com.hari.currencyconverter.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LogManager.getLogger(EmailServiceImpl.class);

    private final JavaMailSender javaMailSender;

    private final String toEmail;

    private final String fromEmail;

    @Autowired
    public EmailServiceImpl(JavaMailSender javaMailSender,
                            @Value("${email.to}") String toEmail,
                            @Value("${spring.mail.username}") String fromEmail) {
        this.javaMailSender = javaMailSender;
        this.toEmail = toEmail;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(String body) {

        try {
            final SimpleMailMessage email = new SimpleMailMessage();
            email.setSubject("Currency Conversion Rate now");
            email.setTo(toEmail);
            email.setText(body);
            email.setFrom(fromEmail);

            javaMailSender.send(email);
        }
        catch (Exception e) {
            LOGGER.error("Exception when sending email " , e);
        }

    }
}
