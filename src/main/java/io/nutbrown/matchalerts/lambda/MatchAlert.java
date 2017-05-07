package com.nutbrown.matchalerts.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 28/04/2017.
 */
public class MatchAlert implements RequestHandler<String, String> {

	private static final String PHONE_NUMBER = "+441234567890";
	private static final String TEAM_NAME = "Sheffield Wednesday";
	
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet("http://api.football-data.org/v1/teams/345/fixtures");

    public String handleRequest(String input, Context context) {
        String messageSent = "False";
        try {
            HttpResponse response = httpClient.execute(getRequest);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

            String responseString = EntityUtils.toString(response.getEntity());
            Date date = new Date();
            String modifiedDate= new SimpleDateFormat("yyyy-MM-dd").format(date);

            if(responseString.contains(modifiedDate)) {
                sendSMSMessage();
                messageSent = "True";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messageSent;
    }

    public static void sendSMSMessage() {

        AmazonSNSClient snsClient = new AmazonSNSClient();
        String message = TEAM_NAME + " are playing today.";
        String phoneNumber = PHONE_NUMBER;
        Map<String, MessageAttributeValue> smsAttributes =
                new HashMap<String, MessageAttributeValue>();

        smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                .withStringValue("football")
                .withDataType("String"));

        smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue("Promotional") 
                .withDataType("String"));

        smsAttributes.put("AWS.SNS.SMS.MaxPrice", new MessageAttributeValue()
                .withStringValue("0.20")
                .withDataType("Number"));

        PublishResult result = snsClient.publish(new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phoneNumber)
                .withMessageAttributes(smsAttributes));
        System.out.println("Message sent:" + result);
    }

}