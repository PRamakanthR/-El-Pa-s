package articles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class ElPaisScraper {

    private static final String DEEPL_API_KEY = "API_KEY"; // Replace with your DeepL API key
    private static final String DEEPL_API_URL = "https://api-free.deepl.com/v2/translate";

    private static Map<String, Integer> wordCountMap = new HashMap<>();

    @Test
    @Parameters({"browserName"})
    public void scrapeElPais(String browserName) {
        String browserstackUsername = "USER_NAME"; // Replace with your Browserstack Username
        String browserstackAccessKey = "ACCESS_KEY"; // Replace with your Browserstack AccessKey

        WebDriver driver = null;

        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability("browserName", browserName);
            capabilities.setCapability("browserVersion", "latest");
            Map<String, Object> browserstackOptions = new HashMap<>();
            browserstackOptions.put("os", "Windows");
            browserstackOptions.put("osVersion", "10");
            browserstackOptions.put("userName", browserstackUsername);
            browserstackOptions.put("accessKey", browserstackAccessKey);
            capabilities.setCapability("bstack:options", browserstackOptions);

            driver = new RemoteWebDriver(
                    new URL("https://" + browserstackUsername + ":" + browserstackAccessKey + "@hub-cloud.browserstack.com/wd/hub"),
                    capabilities
            );

            driver.navigate().to("https://elpais.com");
            driver.manage().window().maximize();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            WebElement agreeButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("didomi-notice-agree-button")));
            agreeButton.click();

            WebElement opinionLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Opinión")));
            opinionLink.click();

            for (int i = 1; i <= 5; i++) {
                try {
                    WebElement titleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//article/header/h2/a)[" + i + "]")));
                    WebElement contentElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//article/p)[" + i + "]")));

                    String title = titleElement.getText();
                    String content = contentElement.getText();

                    System.out.println("Article " + i + ": ");
                    System.out.println("Title: " + title);

                    String translatedTitle = translateTitle(title);
                    System.out.println("Translated Title: " + translatedTitle);
                    System.out.println("Content: " + content);

                    updateWordCount(translatedTitle);

                    titleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//article/header/h2/a)[" + i + "]")));
                    titleElement.click();

                    WebElement imageElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//header//img)[2]")));
                    String imageUrl = imageElement.getAttribute("src");
                    Path localImagePath = Paths.get("C:\\Users\\ramak\\RestAssured\\El_Pais\\src\\Images\\article" + i + ".jpg");
                    downloadImage(imageUrl, localImagePath);

                    driver.navigate().back();
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Opinión")));

                } catch (Exception e) {
                    System.out.println("Article " + i + " not found or could not be loaded.");
                    e.printStackTrace();
                }
            }

            printRepeatedWords();

        } catch (Exception e) {
            System.err.println("Error initializing WebDriver or during test execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static void downloadImage(String imageUrl, Path savePath) throws IOException {
        URI uri = URI.create(imageUrl);
        URL url = uri.toURL();
        try (InputStream in = url.openStream(); FileOutputStream out = new FileOutputStream(savePath.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private static String translateTitle(String title) {
        try {
            URL url = new URL(DEEPL_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "DeepL-Auth-Key " + DEEPL_API_KEY);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            String data = "text=" + title + "&target_lang=EN";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data.getBytes());
            }

            try (InputStream responseStream = connection.getInputStream()) {
                String jsonResponse = new String(responseStream.readAllBytes());
                JSONObject jsonObject = new JSONObject(jsonResponse);
                return jsonObject.getJSONArray("translations").getJSONObject(0).getString("text");
            }

        } catch (Exception e) {
            System.err.println("Error translating title: " + e.getMessage());
            e.printStackTrace();
            return "Translation failed";
        }
    }

    private static void updateWordCount(String translatedTitle) {
        String[] words = translatedTitle.split("\\s+");
        for (String word : words) {
            word = word.toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (!word.isEmpty()) {
                wordCountMap.put(word, wordCountMap.getOrDefault(word, 0) + 1);
            }
        }
    }

    private static void printRepeatedWords() {
        System.out.println("Repeated words across translated titles:");
        for (Map.Entry<String, Integer> entry : wordCountMap.entrySet()) {
            if (entry.getValue() > 1) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
