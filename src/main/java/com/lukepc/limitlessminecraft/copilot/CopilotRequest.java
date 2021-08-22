package com.lukepc.limitlessminecraft.copilot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CopilotRequest {
    private static final String API_URL = "https://copilot.githubassets.com/v1/engines/" +
            "github-multi-stochbpe-cushman-pii/completions";
    private final HttpClient client = HttpClient.newHttpClient();

    private int maxTokens = 70;
    private double temperature = 0.2;
    private double topP = 1.0;
    private int count = 3;
    private int logProbability = 2;
    private List<String> stop = List.of("\n");

    public List<String> send(CopilotToken token, String prompt) {
        JSONObject requestBody = new JSONObject(Map.of(
                "prompt", prompt,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "top_p", topP,
                "n", count,
                "logprobs", logProbability,
                "stop", stop
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(3))
                .header("Authorization", "Bearer " + token.token())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Openai-Organization", "github-copilot")
                .header("OpenAI-Intent", "copilot-ghost")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject responseBody = new JSONObject(httpResponse.body());

            List<String> choices = new ArrayList<>();
            JSONArray choicesJson = responseBody.getJSONArray("choices");
            for (int i = 0; i < choicesJson.length(); i++) {
                JSONObject choice = choicesJson.getJSONObject(i);
                choices.add(choice.getString("text"));
            }
            return choices;
        } catch (HttpTimeoutException e) {
            return null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTopP() {
        return topP;
    }

    public void setTopP(double topP) {
        this.topP = topP;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getLogProbability() {
        return logProbability;
    }

    public void setLogProbability(int logProbability) {
        this.logProbability = logProbability;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }
}
