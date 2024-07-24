package org.example;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        startResetTask();
    }

    private void startResetTask() {
        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                1, 1, timeUnit);
    }

    public synchronized void createDocument(Document document, String signature) throws Exception {
        semaphore.acquire();

        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);

            document.signature = signature;
            String jsonInputString = new ObjectMapper().writeValueAsString(document);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int response = connection.getResponseCode();

            if (response != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + response);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            semaphore.release();
        }
    }

    public static class Description {
    }

    public static class Document {
    }

    public static class Product {

    }


}