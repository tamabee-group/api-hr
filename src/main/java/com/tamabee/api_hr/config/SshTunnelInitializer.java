package com.tamabee.api_hr.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Khởi tạo SSH tunnel TRƯỚC KHI Spring context được refresh.
 * Load file .env và tạo tunnel trước khi DataSource được khởi tạo.
 */
public class SshTunnelInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static Session session;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        // Load file .env và thêm vào environment
        loadEnvFile(env);

        String tunnelEnabled = env.getProperty("database.tunnel.enabled", "false");
        if (!"true".equalsIgnoreCase(tunnelEnabled)) {
            System.out.println("SSH tunnel đã bị tắt");
            return;
        }

        String sshHost = env.getProperty("database.tunnel.ssh.host");
        int sshPort = Integer.parseInt(env.getProperty("database.tunnel.ssh.port", "22"));
        String sshUsername = env.getProperty("database.tunnel.ssh.username");
        String sshPassword = env.getProperty("database.tunnel.ssh.password");
        int localPort = Integer.parseInt(env.getProperty("database.tunnel.local.port", "5433"));
        String remoteHost = env.getProperty("database.tunnel.remote.host", "localhost");
        int remotePort = Integer.parseInt(env.getProperty("database.tunnel.remote.port", "5432"));

        try {
            System.out.println("Đang tạo SSH tunnel đến " + sshUsername + "@" + sshHost + ":" + sshPort + "...");

            JSch jsch = new JSch();
            session = jsch.getSession(sshUsername, sshHost, sshPort);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect(30000);
            System.out.println("SSH session đã kết nối thành công");

            session.setPortForwardingL(localPort, remoteHost, remotePort);

            System.out.println("SSH tunnel đã tạo thành công: localhost:" + localPort +
                    " -> " + sshHost + ":" + remoteHost + ":" + remotePort);

            // Đợi tunnel ổn định
            Thread.sleep(2000);
            System.out.println("SSH tunnel đã sẵn sàng");

            // Đăng ký shutdown hook để đóng tunnel khi app tắt
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                    System.out.println("SSH tunnel đã đóng");
                }
            }));

        } catch (Exception e) {
            System.err.println("Lỗi khi tạo SSH tunnel: " + e.getMessage());
            throw new RuntimeException("Không thể tạo SSH tunnel", e);
        }
    }

    /**
     * Đọc file .env và thêm các biến môi trường vào Spring Environment
     */
    private void loadEnvFile(ConfigurableEnvironment env) {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            System.out.println("Không tìm thấy file .env, bỏ qua");
            return;
        }

        try {
            Map<String, Object> envVars = new HashMap<>();
            Files.readAllLines(envPath).forEach(line -> {
                line = line.trim();
                // Bỏ qua dòng trống và comment
                if (line.isEmpty() || line.startsWith("#")) {
                    return;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    envVars.put(key, value);
                }
            });

            // Chuyển đổi biến môi trường sang tên property của Spring
            mapEnvToSpringProperties(envVars);

            // Thêm vào đầu danh sách property sources để có độ ưu tiên cao nhất
            env.getPropertySources().addFirst(new MapPropertySource("dotenv", envVars));
            System.out.println("Đã load " + envVars.size() + " properties từ file .env");

        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file .env: " + e.getMessage());
        }
    }

    /**
     * Chuyển đổi tên biến môi trường sang tên property của Spring
     * Ví dụ: SSH_HOST -> database.tunnel.ssh.host
     */
    private void mapEnvToSpringProperties(Map<String, Object> envVars) {
        // Database properties
        if (envVars.containsKey("DATABASE_URL")) {
            envVars.put("spring.datasource.url", envVars.get("DATABASE_URL"));
        }
        if (envVars.containsKey("DATABASE_USERNAME")) {
            envVars.put("spring.datasource.username", envVars.get("DATABASE_USERNAME"));
        }
        if (envVars.containsKey("DATABASE_PASSWORD")) {
            envVars.put("spring.datasource.password", envVars.get("DATABASE_PASSWORD"));
        }
        
        // SSH tunnel properties
        if (envVars.containsKey("DATABASE_TUNNEL_ENABLED")) {
            envVars.put("database.tunnel.enabled", envVars.get("DATABASE_TUNNEL_ENABLED"));
        }
        if (envVars.containsKey("SSH_HOST")) {
            envVars.put("database.tunnel.ssh.host", envVars.get("SSH_HOST"));
        }
        if (envVars.containsKey("SSH_PORT")) {
            envVars.put("database.tunnel.ssh.port", envVars.get("SSH_PORT"));
        }
        if (envVars.containsKey("SSH_USERNAME")) {
            envVars.put("database.tunnel.ssh.username", envVars.get("SSH_USERNAME"));
        }
        if (envVars.containsKey("SSH_PASSWORD")) {
            envVars.put("database.tunnel.ssh.password", envVars.get("SSH_PASSWORD"));
        }
        if (envVars.containsKey("LOCAL_PORT")) {
            envVars.put("database.tunnel.local.port", envVars.get("LOCAL_PORT"));
        }
        if (envVars.containsKey("REMOTE_HOST")) {
            envVars.put("database.tunnel.remote.host", envVars.get("REMOTE_HOST"));
        }
        if (envVars.containsKey("REMOTE_PORT")) {
            envVars.put("database.tunnel.remote.port", envVars.get("REMOTE_PORT"));
        }
        
        // JWT properties
        if (envVars.containsKey("JWT_SECRET")) {
            envVars.put("jwt.secret", envVars.get("JWT_SECRET"));
        }
        if (envVars.containsKey("JWT_ACCESS_TOKEN_EXPIRATION")) {
            envVars.put("jwt.access-token-expiration", envVars.get("JWT_ACCESS_TOKEN_EXPIRATION"));
        }
        if (envVars.containsKey("JWT_REFRESH_TOKEN_EXPIRATION")) {
            envVars.put("jwt.refresh-token-expiration", envVars.get("JWT_REFRESH_TOKEN_EXPIRATION"));
        }
    }
}
