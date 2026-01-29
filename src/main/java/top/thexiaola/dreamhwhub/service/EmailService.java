package top.thexiaola.dreamhwhub.service;

public interface EmailService {
    void sendVerificationCode(String email);
    boolean verifyCode(String email, String code);
}