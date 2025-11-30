package misc;

import utils.json.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GetBidClient {

    public MonoResult invokeAndGetFirst(List<String> urls) {
        final MonoResult result = new MonoResult();
        urls.parallelStream()
                .filter(Objects::nonNull)
                .filter(url -> !url.isEmpty())
                .map(uri -> uri.replace(" ", "%20"))
                .map(uri -> {
                    try (HttpClient client = HttpClient.newHttpClient()) {
                        return client.sendAsync(
                                HttpRequest.newBuilder(new URI(uri)).GET().build(),
                                BodyHandlers.ofString());
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .map(future -> future.thenApplyAsync((httpResponse) -> {
                    String url = httpResponse.uri().getHost();
                    Map<String, Object> body = (Map<String, Object>)  new JsonParser(httpResponse.body()).parse();
                    return new MonoResult(url, (Double) body.get("bid"));
                }))
                .forEach(future -> future.whenCompleteAsync(
                        (response, exception) -> new ResultUpdater().update(response, result)));
        return result;
    }
}

class ResultUpdater {
    public void update(MonoResult response, MonoResult result) {
        if (response == null)
            return;

        if (response.getBid() == null) {
            return;
        }

        synchronized (result) {
            if (result.getBid() != null)
                return;
            result.setUrl(response.getUrl());
            result.setBid(response.getBid());
        }
    }
}

class MonoResult {
    private String url;
    private Double bid;

    public MonoResult() {}

    public MonoResult(String url, Double bid) {
        this.url = url;
        this.bid = bid;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setBid(Double bid) {
        this.bid = bid;
    }

    public String getUrl() {
        return url;
    }

    public Double getBid() {
        return bid;
    }
}
