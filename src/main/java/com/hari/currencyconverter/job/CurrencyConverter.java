package com.hari.currencyconverter.job;

import com.hari.currencyconverter.email.EmailService;
import com.hari.currencyconverter.util.Constants;
import net.spy.memcached.MemcachedClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Scanner;

@Component
public class CurrencyConverter {

    private static final Logger LOGGER = LogManager.getLogger(CurrencyConverter.class);

    private final String apiAccessKey;
    private final String fromCurrency;
    private final String toCurrency;
    private final String bankersAlgoBaseUrl;
    private final EmailService emailService;
    private final Double currencyEmailThreshold;
    private final MemcachedClient memcachedClient;

    @Autowired
    public CurrencyConverter(@Value("${bankersalgo.accesskey}") String apiAccessKey,
                             @Value("${from.currency}") String fromCurrency,
                             @Value("${to.currency}") String toCurrency,
                             @Value("${bankersalgo.baseurl}") String bankersAlgoBaseUrl,
                             @Value("${currency.email.threshold}") Double currencyEmailThreshold,
                             EmailService emailService,
                             MemcachedClient memcachedClient) {
        this.apiAccessKey           = apiAccessKey;
        this.fromCurrency           = fromCurrency;
        this.toCurrency             = toCurrency;
        this.bankersAlgoBaseUrl     = bankersAlgoBaseUrl;
        this.emailService           = emailService;
        this.currencyEmailThreshold = currencyEmailThreshold;
        this.memcachedClient        = memcachedClient;
    }

    //@Scheduled(cron = "0 */15 6-20 * * MON-FRI")
    @Scheduled(cron = "*/10 * * * * *") // CRON runs every 10 seconds
    public void processCurrencies() {

        LOGGER.info("Started the app");

        // Call bankersalgo API to get the exchange rate
        JSONObject apiResponse = fetchCurrencyConversionData();
        boolean validationResponse = validateJsonResponse(apiResponse);

        if(!validationResponse) {
            throw new RuntimeException("Failed validations for API response from bankersalgo");
        }

        Double currentConversionValue = apiResponse.getJSONObject("rates").getDouble(toCurrency);

        String emailBody = "Conversion value for 1 " + fromCurrency + " to " + toCurrency + " currently is " + currentConversionValue;
        LOGGER.info(emailBody);

        /* Send email  :
           1. If exchange value is more than value specified in config file OR
           2. If email is already sent last time app ran and conversion value now is 20 paise more than last time
         ** Don't send email more than 5 times in a day */
        if(Double.compare(currentConversionValue, currencyEmailThreshold) >= 0) {

            Object conversionValueInCache = memcachedClient.get(Constants.CONVERSION_KEY);
            Object emailSentCountObj      = memcachedClient.get(Constants.EMAIL_SENT_COUNT_KEY);
            Object previousEmailSentAtObj = memcachedClient.get(Constants.EMAIL_SENT_TIME_KEY);

            if(conversionValueInCache == null || Double.compare(currentConversionValue , (Double) conversionValueInCache + 0.20) >= 0) {

                int emailSentCount = emailSentCountObj == null ? 0 : (int) emailSentCountObj;

                if(canSendEmail(previousEmailSentAtObj, emailSentCountObj)) {
                    // exp : 0 - item is never removed from cache - ttl = never expire
                    memcachedClient.set(Constants.CONVERSION_KEY, 0, currentConversionValue);
                    memcachedClient.set(Constants.EMAIL_SENT_COUNT_KEY, 0, emailSentCount + 1);
                    memcachedClient.set(Constants.EMAIL_SENT_TIME_KEY, 0, LocalDateTime.now());
                    emailService.sendEmail(emailBody);
                }
            }
        }
    }

    private boolean canSendEmail(Object previousEmailSentAtObj, Object emailSentCountObj) {

        LocalDateTime emailSentAt = previousEmailSentAtObj == null ? LocalDateTime.now() : (LocalDateTime) previousEmailSentAtObj;
        int emailSentCount        = emailSentCountObj == null ? 0 : (int) emailSentCountObj;

        if(emailSentAt.getDayOfYear() - LocalDateTime.now().getDayOfYear() == 0 && emailSentCount >= 5) {
            return false;
        }

        return true;
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


    public JSONObject fetchCurrencyConversionData() {

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