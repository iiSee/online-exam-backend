package edu.sandau.service.impl;

import edu.sandau.dao.EmailMessageDao;
import edu.sandau.entity.EmailMessage;
import edu.sandau.service.EmailService;
import edu.sandau.utils.FreemarkerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private JavaMailSender javaMailSender;
    private SimpleMailMessage simpleMailMessage;
    @Autowired
    private EmailMessageDao emailMessageDao;

    public EmailServiceImpl(JavaMailSender javaMailSender, SimpleMailMessage simpleMailMessage) {
        this.javaMailSender = javaMailSender;
        this.simpleMailMessage = simpleMailMessage;
        USERNAME = ((JavaMailSenderImpl) javaMailSender).getUsername();
    }

    /***
     * 发送邮箱地址 FROM
     */
    private static String USERNAME;

    public void sendSimpleMail(EmailMessage emailMessage) throws Exception {
        try {
            simpleMailMessage.setFrom(USERNAME);
            //用于接收邮件的邮箱
            simpleMailMessage.setTo(emailMessage.getEmail());
            //邮件的主题
            simpleMailMessage.setSubject(emailMessage.getSubject());
            //邮件的正文，第二个boolean类型的参数代表html格式
            simpleMailMessage.setText(emailMessage.getContent());

            //发送邮件
            javaMailSender.send(simpleMailMessage);
            log.info("发送邮件：{}", simpleMailMessage);
//            emailVoDao.save(emailMessage);
        } catch (Exception e) {
            throw new MessagingException("failed to send mail!", e);
        }
    }

    public void sendHTMLMail(EmailMessage emailMessage, Map<String,Object> model, String templateFileName) throws Exception{
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage,true,"UTF-8");
        messageHelper.setSubject(emailMessage.getSubject());    //设置邮件主题
//        messageHelper.setText(emailMessage.getContent());   //设置邮件主题内容
        messageHelper.setTo(emailMessage.getEmail());   //设定收件人Email

        String text = FreemarkerUtil.getTemplate(templateFileName, model);
        messageHelper.setText(text, true);  //设置邮件主题内容
        messageHelper.setFrom(USERNAME);
        javaMailSender.send(mimeMessage);
    }

}