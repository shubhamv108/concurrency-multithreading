package code.shubham.multithreading;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

interface HtmlParser {
     default List<String> getUrls(String url) throws IOException {
         // Download HTML
         Document doc = Jsoup.connect(url)
                 .userAgent("Mozilla/5.0")
                 .timeout(10000)
                 .get();

         List<String> urls = new ArrayList<>();

         // Extract all links
         Elements links = doc.select("a[href]");
         for (Element link : links) {
             String absUrl = link.absUrl("href");
             if (!absUrl.isEmpty()) {
                 urls.add(absUrl);
             }
         }

         return urls;
     }
}

class Solution {
    Queue<String> q = new LinkedBlockingQueue<>();
    Set<String> v = new ConcurrentSkipListSet<>();
    String hostName = null;
    HtmlParser htmlParser = null;
    ArrayList<String> result = new ArrayList<>();


    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        this.htmlParser = htmlParser;
        hostName = getHostName(startUrl);
        q.offer(startUrl);
        crawl();
        return result;
    }

    void crawl() {
        try (ExecutorService executor = Executors.newFixedThreadPool(5)) {
            while (!q.isEmpty()) {
                String url = q.poll();
                executor.submit(() -> {
                    try {
                        this.process(url);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    void process(String url) throws IOException {
        for (String a : htmlParser.getUrls(url)) {
            if (hostName.equals(getHostName(a)) && v.add(a)) {
                q.offer(a);
                result.add(a);
            }
        }
    }

    String getHostName(String url) {
        url = url.substring(7);
        String[] parts = url.split("/");
        return parts[0];
    }
}