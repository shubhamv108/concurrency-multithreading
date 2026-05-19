package code.shubham.multithreading.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * // This is the HtmlParser's API interface.
 * // You should not implement it, or speculate about its implementation
 * interface HtmlParser {
 *     public List<String> getUrls(String url) {}
 * }
 */

interface HtmlParser {
    List<String> getUrls(String url);
}

class JsoupDocumentParser implements HtmlParser {
    public List<String> getUrls(String pageUrl) {
        try {
            org.jsoup.nodes.Document document = Jsoup
                    .connect(pageUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            Elements links = document.select("a[href]");
            return links.stream()
                    .map(link -> link.absUrl("href"))
                    .filter(absUrl -> !absUrl.isEmpty())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class UrlUtils {
    public static String getHostName(String url) {
        url = url.substring(7);
        String[] parts = url.split("/");
        return parts[0];
    }
}

class Solution {

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        HashSet<String> visited = new HashSet(Arrays.asList(startUrl));
        LinkedList<String> q = new LinkedList<>(visited);
        WebCrawler crawler = new WebCrawler(visited, q, htmlParser, UrlUtils.getHostName(startUrl));
        ExecutorService pool = null;
        try {
            pool = Executors.newFixedThreadPool(10);
            for (int i = 0; i < 10; ++i)
                pool.submit(crawler);
        } finally {
            if (pool != null) {
                pool.shutdown();
                while (true) {
                    try {
                        if (pool.awaitTermination(1, TimeUnit.SECONDS)) break;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return new ArrayList<>(visited);
    }

    class WebCrawler implements Runnable {
        private final HashSet<String> visited;
        private final LinkedList<String> q;
        private final HtmlParser htmlParser;
        private final String hostName;

        public WebCrawler(
                HashSet<String> visited,
                LinkedList<String> q,
                HtmlParser htmlParser,
                String hostName) {
            this.visited = visited;
            this.q = q;
            this.htmlParser = htmlParser;
            this.hostName = hostName;
        }

        public void run() {
            while (true) {
                try {
                    String url = poll(10);
                    if (url == null)
                        break;
                    offer(htmlParser.getUrls(url));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        String poll(long waitInMilliSeconds) throws InterruptedException {
            synchronized (q) {
                if (q.isEmpty())
                    q.wait(waitInMilliSeconds);

                String url = q.poll();
                q.notifyAll();
                return url;
            }
        }

        void offer(List<String> urls) {
            for (String url : urls) {
                if (!UrlUtils.getHostName(url).equals(hostName))
                    continue;

                synchronized (q) {
                    if (visited.add(url))
                        q.offer(url);
                }
            }
        }
    }
}

class Solution2 {

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        Set<String> visited = ConcurrentHashMap.newKeySet();
        ExecutorService pool = null;
        try {
            pool = Executors.newVirtualThreadPerTaskExecutor();
            AtomicInteger active = new AtomicInteger(1);
            WebCrawler crawler = new WebCrawler(visited, htmlParser, UrlUtils.getHostName(startUrl), pool, active);
            visited.add(startUrl);
            pool.submit(() -> crawler.run(startUrl));

            while (active.get() > 0) {
                try {
                    Thread.sleep(15);
                }
                catch(InterruptedException e) {}
            }
        } finally {
            if (pool != null) {
                pool.shutdown();
                while (true) {
                    try {
                        if (pool.awaitTermination(1, TimeUnit.SECONDS)) break;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return new ArrayList<>(visited);
    }

    class WebCrawler {
        private final Set<String> visited;
        private final HtmlParser htmlParser;
        private final String hostName;
        private final ExecutorService pool;
        private final AtomicInteger active;

        public WebCrawler(
                Set<String> visited,
                HtmlParser htmlParser,
                String hostName,
                ExecutorService pool,
                AtomicInteger active) {
            this.visited = visited;
            this.htmlParser = htmlParser;
            this.hostName = hostName;
            this.pool = pool;
            this.active = active;
        }

        public void run(String startUrl) {
            htmlParser.getUrls(startUrl)
                    .parallelStream()
                    .filter(url -> UrlUtils.getHostName(url).equals(hostName) && visited.add(url))
                    .forEach(url -> {
                        active.incrementAndGet();
                        pool.submit(() -> this.run(url));
                    });
            active.decrementAndGet();
        }
    }
}

class Solution3 {

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        String host = UrlUtils.getHostName(startUrl);
        Set<String> visited = ConcurrentHashMap.newKeySet();
        ForkJoinPool pool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors()
        );

        try {
            visited.add(startUrl);
            pool.invoke(new CrawlTask(startUrl, host, htmlParser, visited));
        } finally {
            pool.shutdown();
        }

        return new ArrayList<>(visited);
    }

    static class CrawlTask extends RecursiveAction {

        private final String url;
        private final String host;
        private final HtmlParser parser;
        private final Set<String> visited;

        CrawlTask(String url,
                  String host,
                  HtmlParser parser,
                  Set<String> visited) {

            this.url = url;
            this.host = host;
            this.parser = parser;
            this.visited = visited;
        }

        @Override
        protected void compute() {
            List<CrawlTask> subtasks = new ArrayList<>();
            try {
                for (String next : parser.getUrls(url))
                    if (UrlUtils.getHostName(next).equals(host) && visited.add(next))
                        subtasks.add(
                                new CrawlTask(
                                        next,
                                        host,
                                        parser,
                                        visited
                                ));

                invokeAll(subtasks);
            } catch (Exception ignored) {
                // production: log here
            }
        }
    }
}

public class WebCrawler {
    void main() throws InterruptedException {
        System.out.println(new Solution2().crawl("http://news.yahoo.com/news/topics/", new JsoupDocumentParser()));
    }
}