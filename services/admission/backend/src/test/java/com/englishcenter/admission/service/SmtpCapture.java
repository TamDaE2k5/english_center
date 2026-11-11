package com.englishcenter.admission.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/** SMTP loopback cho QA: nhận email thật từ JavaMail, giữ trong bộ nhớ và không ghi nội dung ra log. */
final class SmtpCapture implements AutoCloseable {
    private final ServerSocket server;
    private final List<Mail> messages = new CopyOnWriteArrayList<>();
    private record Mail(String recipient, String data) {}
    SmtpCapture() {
        try { server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress()); }
        catch (IOException ex) { throw new UncheckedIOException(ex); }
        Thread thread = new Thread(() -> {
            while (!server.isClosed()) {
                try { Socket socket = server.accept(); Thread session = new Thread(() -> receive(socket)); session.setDaemon(true); session.start(); }
                catch (IOException ex) { if (!server.isClosed()) throw new UncheckedIOException(ex); }
            }
        });
        thread.setDaemon(true); thread.start();
    }
    int port() { return server.getLocalPort(); }
    /** Lấy token bằng mục đích và người nhận; assertion không in body hoặc token ra output. */
    String token(String email, String action) {
        var pattern = Pattern.compile("#" + action + "=([A-Za-z0-9_-]+)");
        for (int i = messages.size() - 1; i >= 0; i--) {
            Mail mail = messages.get(i);
            if (!mail.recipient().toLowerCase(Locale.ROOT).contains(email.toLowerCase(Locale.ROOT))) continue;
            var match = pattern.matcher(mail.data()); if (match.find()) return match.group(1);
        }
        throw new AssertionError("Expected local SMTP message was not received");
    }
    int count(String email) { return (int) messages.stream().filter(m -> m.recipient().contains(email)).count(); }
    private void receive(Socket socket) {
        try (socket; var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            socket.setSoTimeout(10000); send(out, "220 localhost QA"); String recipient = "", line;
            while ((line = in.readLine()) != null) {
                String command = line.toUpperCase(Locale.ROOT);
                if (command.startsWith("EHLO") || command.startsWith("HELO")) send(out, "250 localhost");
                else if (command.startsWith("RCPT TO:")) { recipient = line; send(out, "250 OK"); }
                else if (command.equals("DATA")) {
                    send(out, "354 End with dot"); StringBuilder body = new StringBuilder();
                    while ((line = in.readLine()) != null && !line.equals(".")) body.append(line).append('\n');
                    messages.add(new Mail(recipient, body.toString())); send(out, "250 Accepted");
                } else if (command.equals("QUIT")) { send(out, "221 Bye"); return; }
                else send(out, "250 OK");
            }
        } catch (IOException ignored) { /* Không log phiên SMTP vì dữ liệu có token. */ }
    }
    private void send(PrintWriter out, String value) { out.print(value + "\r\n"); out.flush(); }
    @Override public void close() throws IOException { server.close(); }
}
