import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        List<Product> products = new ArrayList<>();
        String baseUrl = "https://www2.hm.com/en_gb/ladies/shop-by-product/dresses.html";

        try {
            Document firstPage = Jsoup.connect(baseUrl).get();
            int totalPages = getTotalPages(firstPage);

            ForkJoinPool customThreadPool = new ForkJoinPool(10); // Creăm un pool custom de thread-uri pentru a controla nivelul de paralelism
            products = customThreadPool.submit(() ->
                    IntStream.range(0, totalPages).parallel()
                            .mapToObj(page -> scrapePage(baseUrl, page))
                            .flatMap(List::stream)
                            .collect(Collectors.toList())
            ).get();

            long scrapeEndTime = System.currentTimeMillis();
            long scrapeExecutionTime = scrapeEndTime - startTime;
            System.out.println("Scraping execution time: " + scrapeExecutionTime + " milliseconds");
            System.out.println("Total products extracted: " + products.size());

            writeDataToCSV(products);
            products.parallelStream().sorted(Comparator.comparingDouble(Product::getPrice).reversed()).collect(Collectors.toList());
            List<Product> topFiveProducts = products.subList(0, Math.min(products.size(), 5));
            generateAndSaveChart(topFiveProducts);

            long processingEndTime = System.currentTimeMillis();
            long processingExecutionTime = processingEndTime - scrapeEndTime;

            System.out.println("Processing execution time: " + processingExecutionTime + " milliseconds");

            System.out.println("Statistical analysis:");
            double averagePrice = products.parallelStream()
                    .mapToDouble(Product::getPrice)
                    .average()
                    .orElse(0.0);
            System.out.println("Average price: " + averagePrice);

            double minPrice = products.parallelStream()
                    .mapToDouble(Product::getPrice)
                    .min()
                    .orElse(0.0);
            System.out.println("Minimum price: " + minPrice);

            double maxPrice = products.parallelStream()
                    .mapToDouble(Product::getPrice)
                    .max()
                    .orElse(0.0);
            System.out.println("Maximum price: " + maxPrice);

        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("\nTotal execution time: " + executionTime + " milliseconds");
    }

    private static List<Product> scrapePage(String baseUrl, int page) {
        List<Product> products = new ArrayList<>();
        String url = baseUrl + "?page=" + (page + 1);
        try {
            Document currentPage = Jsoup.connect(url).get();
            Elements productLinks = currentPage.select("div#products-listing-section li article");

            for (Element link : productLinks) {
                String name = link.select("h2").text();
                String price = link.select("p").text();
                products.add(new Product(name, parsePrice(price)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return products;
    }

    private static int getTotalPages(Document firstPage) {
        Element paginationNav = firstPage.select("nav[aria-label=Pagination]").first();
        if (paginationNav != null) {
            Element lastPageElement = paginationNav.select("a[aria-label^=Go to page]").last();
            if (lastPageElement != null) {
                String lastPageLink = lastPageElement.attr("href");
                String[] parts = lastPageLink.split("page=");
                if (parts.length >= 2) {
                    try {
                        return Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return 1;
    }

    private static double parsePrice(String priceText) {
        String cleanPriceText = priceText.replaceAll("[^\\d.]", "");
        int dotCount = 0;
        StringBuilder result = new StringBuilder();
        for (char c : cleanPriceText.toCharArray()) {
            if (c == '.' && dotCount == 0) {
                result.append(c);
                dotCount++;
            } else if (Character.isDigit(c)) {
                result.append(c);
            }
        }
        try {
            return Double.parseDouble(result.toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    private static void writeDataToCSV(List<Product> products) throws IOException {
        FileWriter out = new FileWriter("products.csv");
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
            printer.printRecord("Name", "Price");
            products.parallelStream().forEach(product -> {
                try {
                    printer.printRecord(product.getName(), product.getPrice());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void generateAndSaveChart(List<Product> products) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        products.parallelStream().forEach(product -> dataset.addValue(product.getPrice(), "Price", product.getName()));

        JFreeChart barChart = ChartFactory.createBarChart(
                "Top 5 Most Expensive Products",
                "Product Name",
                "Price",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        int width = 800;
        int height = 600;
        ChartUtils.saveChartAsPNG(new java.io.File("top_5_expensive_products_chart.png"), barChart, width, height);
    }
}
