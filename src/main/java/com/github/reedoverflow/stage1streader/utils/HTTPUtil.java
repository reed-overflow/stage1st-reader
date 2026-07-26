package com.github.reedoverflow.stage1streader.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public final class HTTPUtil {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private HTTPUtil() {
    }

    public static String doGet(String url) throws IOException {
        return doGet(url, "UTF-8");
    }

    public static String doGet(String url, String charset) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(READ_TIMEOUT_MS)
                .build();

        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(requestConfig);
        httpGet.setHeader("Accept", "application/json, text/plain, */*");
        httpGet.setHeader("User-Agent", "stage1st-reader IntelliJ plugin");

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = entity == null ? "" : EntityUtils.toString(entity, charset);

            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP " + statusCode + " " + response.getStatusLine().getReasonPhrase());
            }
            return responseBody;
        }
    }
}
