package com.github.reedoverflow.stage1streader.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class HTTPUtil {

    public static String doGet(String url) throws IOException {
        return doGet(url,"UTF-8");
    }

    public static String doGet(String url, String charset) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        // 设置类型 "application/x-www-form-urlencoded" "application/json"
        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        //  状态码 response.getStatusLine();

        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), charset));
        String line = null;
        StringBuffer responseSB = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            responseSB.append(line);
        }

        reader.close();
        httpClient.close();

        return responseSB.toString();
    }
}
