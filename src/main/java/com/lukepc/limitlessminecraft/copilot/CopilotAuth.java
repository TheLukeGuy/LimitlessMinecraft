package com.lukepc.limitlessminecraft.copilot;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CopilotAuth {
    public static final String AUTH_URL = "https://vscode-auth.github.com/authorize/" +
            "?callbackUri=vscode://vscode.github-authentication/did-authenticate" +
            "&scope=read:user" +
            "&responseType=code" +
            "&authServer=https://github.com";

    public static CopilotToken getToken(String vsCodeUrl) {
        Pattern codePattern = Pattern.compile("\\?code=(\\w+)");
        Matcher codeMatcher = codePattern.matcher(vsCodeUrl);
        if (!codeMatcher.find()) {
            return null;
        }
        String code = codeMatcher.group(1);

        HttpClient client = HttpClient.newHttpClient();
        JSONObject getResponseBody;
        try {
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://vscode-auth.github.com/token/?code=" + code))
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject postResponseBody = new JSONObject(postResponse.body());

            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/copilot_internal/token"))
                    .header("Accept", "application/json")
                    .header("Authorization", "token " + postResponseBody.getString("access_token"))
                    .build();
            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            getResponseBody = new JSONObject(getResponse.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        String token = getResponseBody.getString("token");
        int expiry = getResponseBody.getInt("expires_at");
        return new CopilotToken(token, expiry);
    }
}
