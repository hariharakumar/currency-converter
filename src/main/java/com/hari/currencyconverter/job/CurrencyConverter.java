package com.hari.currencyconverter.job;

import com.hari.currencyconverter.email.EmailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Component
public class CurrencyConverter {

    private static final Logger LOGGER = LogManager.getLogger(CurrencyConverter.class);

    private final String apiAccessKey;
    private final String fromCurrency;
    private final String toCurrency;
    private final String bankersAlgoBaseUrl;
    private final EmailService emailService;

    public CurrencyConverter(@Value("${bankersalgo.accesskey}") String apiAccessKey,
                             @Value("${from.currency}") String fromCurrency,
                             @Value("${to.currency}") String toCurrency,
                             @Value("${bankersalgo.baseurl}") String bankersAlgoBaseUrl, EmailService emailService) {
        this.apiAccessKey = apiAccessKey;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.bankersAlgoBaseUrl = bankersAlgoBaseUrl;
        this.emailService = emailService;
    }

    //@Scheduled(cron = "0 */15 6-20 * * MON-FRI")
    @Scheduled(cron = "*/10 * * * * *")
    public void processCurrencies() {

        LOGGER.info("Started the app");

        // Call bankersalgo API to get the exchange rate
        JSONObject apiResponse = fetchCurrencyConversionObject();
        boolean success = validateJsonResponse(apiResponse);

        if(!success) {
            throw new RuntimeException("Failed validations for API response from bankersalgo");
        }

        Double conversionValue = apiResponse.getJSONObject("rates").getDouble(toCurrency);

        String emailBody = "Conversion value for 1 " + fromCurrency + " to " + toCurrency + " is " + conversionValue;
        LOGGER.info(emailBody);

        emailService.sendEmail(emailBody);
    }

    //TODO : add more validations in the future
    private boolean validateJsonResponse(JSONObject apiResponse) {

        if(!(apiResponse.has("base")
                && apiResponse.getString("base") != fromCurrency)) {
            return false;
        }

        if(!(apiResponse.has("rates") &&
                apiResponse.getJSONObject("rates").has("INR"))) {
            return false;
        }

        return true;
    }


    public JSONObject fetchCurrencyConversionObject() {

        HttpURLConnection connection = null;
        JSONObject apiResponse = new JSONObject();

        try {
            URL url = new URL(bankersAlgoBaseUrl + apiAccessKey + "/" + fromCurrency);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if(connection.getResponseCode() != 200) {
                throw new RuntimeException("Failed, Http Error code : " + connection.getResponseCode());
            }

            Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name());
            String response = scanner.useDelimiter("\\A").next();

            apiResponse = new JSONObject(response);

            return apiResponse;
        }
        catch (MalformedURLException me) {
            LOGGER.error("Error with request URL: " + me);
        }
        catch (IOException ie) {
            LOGGER.error("Exception when making API call: " + ie);
        }
        finally { // TODO : change this to java 8 style try with catch
            if(connection != null) {
                connection.disconnect();
            }
        }

        return apiResponse;
    }

}
