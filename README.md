# Web-Scraping-v2
Web Scraping Varianta Paralela #1

Overview

This project demonstrates how to scrape product data from an e-commerce website, analyze the data, and visualize the results. The example uses the H&M website's dress section to extract product names and prices, write the data to a CSV file, perform statistical analysis, and generate a bar chart of the top 5 most expensive products.

Features

Web Scraping: Uses Jsoup to scrape product data from multiple pages.
Parallel Processing: Utilizes Java's parallel streams and ForkJoinPool for efficient data scraping and processing.
Data Export: Writes scraped data to a CSV file using Apache Commons CSV.
Data Analysis: Calculates average, minimum, and maximum prices.
Data Visualization: Generates a bar chart of the top 5 most expensive products using JFreeChart.

How It Works

Scraping: The scrapePage method uses Jsoup to fetch and parse HTML from each page of the product listing.
Parallel Processing: Pages are scraped in parallel using IntStream.range(0, totalPages).parallel().
Data Storage: Scraped data is collected into a list of Product objects and written to a CSV file.
Statistical Analysis: Calculates and prints average, minimum, and maximum prices from the collected data.
Visualization: Generates and saves a bar chart of the top 5 most expensive products using JFreeChart.

Example Output

The program outputs the following to the console:

Total execution time
Number of products extracted
Average, minimum, and maximum prices

Additionally, it generates:

- products.csv: A CSV file containing the names and prices of all scraped products.
- top_5_expensive_products_chart.png: A bar chart image of the top 5 most expensive products.
