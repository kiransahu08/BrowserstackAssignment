package browswrstackAssignmentPackage;


import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//import jdk.nashorn.internal.parser.JSONParser;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class WebScrapping {
	

    private static final String RAPID_TRANSLATE_API_KEY = "d5891b3792msh79e3b486c869aeap1f5da4jsncae900fb7bbb"; // Updated API Key
    private static final String RAPID_TRANSLATE_API_HOST = "rapid-translate-multi-traduction.p.rapidapi.com"; // Updated Host
    private static final long TIMEOUT_SECONDS = 10;

    
    public static void main(String[] args) {
        WebDriver driver = null;
        try {
            
            driver = new ChromeDriver();
           
         // Step 1: Scrape and analyze articles
            List<String> titles = scrapeAndAnalyze(driver);
           // scrapeAndAnalyze(driver);
            
         // Step 2: Translate and analyze the titles
            translateAndAnalyzeTitles(titles);
            
        } catch (Exception e) {
            System.err.println("Error during execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    
    
    private static List<String> scrapeAndAnalyze(WebDriver driver) throws Exception {
        List<String> titles = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            // Navigate to El País website
            driver.get("https://elpais.com/");
            driver.manage().window().maximize();
            Thread.sleep(2000);

            // Handle cookie consent
            try {
                WebElement confirmElement = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[text()='Aceptar']")));
                confirmElement.click();
                WebElement htmlTag = driver.findElement(By.tagName("html"));
                String langAttribute = htmlTag.getAttribute("lang");
                if ("es-ES".equalsIgnoreCase(langAttribute)) {
                    System.out.println("Validation Passed: The website language is Spanish (lang=es-ES).");
                } else {
                    System.out.println("Validation Failed: The website language is NOT Spanish. Detected: " + langAttribute);
                }
                Thread.sleep(1000);
            } catch (TimeoutException e) {
                System.out.println("Cookie consent button not found or not needed");
            }

            // Navigate to Opinion section
            WebElement opinionLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Opinión")));
            opinionLink.click();
            Thread.sleep(2000);

            // Get article URLs
            List<String> articleUrls = new ArrayList<>();
            List<WebElement> articleElements = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("article h2 a")));
            
            for (WebElement article : articleElements) {
                if (articleUrls.size() < 5) {
                    try {
                        String url = article.getAttribute("href");
                        if (url != null && !url.isEmpty()) {
                            articleUrls.add(url);
                        }
                    } catch (StaleElementReferenceException e) {
                        continue;
                    }
                }
            }

            // Process each article
            for (int i = 0; i < articleUrls.size(); i++) {
                try {
                    driver.get(articleUrls.get(i));
                    Thread.sleep(2000);

                 // Get title and content
                    String title = driver.findElement(By.tagName("h1")).getText();
                    String content = driver.findElement(By.tagName("article")).getText();

                    titles.add(title);
                    contents.add(content);
                    
                    System.out.println("Title: " + title);
                    System.out.println("Content: " + content);         
            
            // Try different image selectors
            
                try {
                WebElement img = null;
                String[] imageSelectors = {
                    "article img.multimedia-image",
                    "article figure img",
                    ".article_header_image img",
                    ".article__media img",
                    "img.cover"
                };

                for (String selector : imageSelectors) {
                    try {
                        List<WebElement> images = driver.findElements(By.cssSelector(selector));
                        if (!images.isEmpty()) {
                            img = images.get(0);
                            break;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                
                }
                if (img != null) {
                    String imgSrc = img.getAttribute("src");
                    if (imgSrc != null && !imgSrc.isEmpty()) {
                        downloadImage(imgSrc, "article_" + (i + 1) + "_image.jpg");
                        System.out.println("Image downloaded for article " + (i + 1));
                    }
                
                }
                else {
                    System.out.println("No image found for article " + (i + 1));
                }
            } 
                catch (Exception e) {
            	System.out.println("Error downloading image for article " + (i + 1) + ": " + e.getMessage());
            }
                } catch (Exception e) {
                    System.err.println("Error processing article " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error during scraping: " + e.getMessage());
            e.printStackTrace();
        }
        return titles;
        //downloadImage(translatedTitles);
    }

    
    private static void downloadImage(String imageUrl, String fileName) throws IOException {
        try (InputStream in = new URL(imageUrl).openStream();
             OutputStream out = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("Successfully downloaded: " + fileName);
        }
        
    }
    
    

    private static void translateAndAnalyzeTitles(List<String> titles) throws InterruptedException {
        System.out.println("\nTranslating titles:");
        
        // List to hold translated titles
        List<String> translatedTitles = new ArrayList<>();
        
        for (String title : titles) {
            try {
                String translatedTitle = translateText(title);
                translatedTitles.add(translatedTitle); // Store the translated title
                System.out.println("Original: " + title);
                System.out.println("Translated: " + translatedTitle);
                System.out.println("-------------------");
            } catch (IOException e) {
                System.err.println("Translation failed for: " + title);
                System.err.println("Error: " + e.getMessage());
            }

            // Add delay between requests to avoid rate-limiting
            try {
                Thread.sleep(1000); // Wait for 1 second before sending the next request
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Analyze repeated words from translated titles
        analyzeRepeatedWords(translatedTitles);
    }

    
    private static void analyzeRepeatedWords(List<String> translatedTitles) {
        // Map to hold word frequencies
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        // Process each translated title
        for (String title : translatedTitles) {
            String[] words = title.toLowerCase().split("\\s+"); // Split by whitespace and convert to lowercase
            
            for (String word : words) {
                word = word.replaceAll("[^a-zA-Z]", ""); // Remove non-alphabetic characters
                if (!word.isEmpty()) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Print repeated words that occur more than twice
        System.out.println("\nRepeated words that appear more than twice:");
        for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
            if (entry.getValue() > 2) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }
    
    

    private static String translateText(String text) throws IOException, InterruptedException {
        // Prepare the translation request
        String jsonBody = String.format("{\"from\":\"es\",\"to\":\"en\",\"q\":\"%s\"}", text);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://rapid-translate-multi-traduction.p.rapidapi.com/t"))
            .header("x-rapidapi-key", RAPID_TRANSLATE_API_KEY)
            .header("x-rapidapi-host", RAPID_TRANSLATE_API_HOST)
            .header("Content-Type", "application/json")
            .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Print the raw response for debugging
        System.out.println("Response body: " + response.body());

        // Check for rate-limiting (HTTP 429) and retry if necessary
        if (response.statusCode() == 429) {
            System.out.println("Rate limit exceeded. Retrying...");
            try {
                Thread.sleep(5000); // Wait for 5 seconds before retrying
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return translateText(text); // Retry the translation
        }

        // Handle response as an array
        try {
            // If the response is an array, we parse it as such
            String[] translatedArray = JsonParser.parseString(response.body()).getAsJsonArray().toString().replace("[", "").replace("]", "").split(",");
            String translatedText = String.join(" ", translatedArray).trim();
            return translatedText;
        } catch (IllegalStateException e) {
            // If the response is not a valid JSON array or object
            System.err.println("Error: Unexpected response format.");
            return "Translation failed (unexpected response format).";
        }
    }

}