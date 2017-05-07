package io.nutbrown.matchalerts.lambda;

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
 * Created by Stephen Nutbrown for nutbrown.io on 28/04/2017.
 */
public class MatchAlert implements RequestHandler<String, String> {

	private static final String PHONE_NUMBER = "+441234567890";
	private static final String TEAM_NAME = "Sheffield Wednesday";
	private static final String TEAM_ID = "345";

    /**
     * Entry method for AWS lambda call.
     * @param input Any input (probably none). Can use this in future iterations to send team ID/Name?
     * @param context AWS lambda context.
     * @return A string saying if the message was successfully sent or not.
     */
    public String handleRequest(String input, Context context) {
        String messageSent = "Message not sent";

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet("http://api.football-data.org/v1/teams/" + TEAM_ID + "/fixtures");
        try {
            HttpResponse response = httpClient.execute(getRequest);
            String responseBody = EntityUtils.toString(response.getEntity());
            String modifiedDate = getCurrentDateAsFormattedString();

            if(responseBody.contains(modifiedDate)) {
                sendSMSMessage();
                messageSent = "Message sent.";
            }
        } catch (IOException e) {
            e.printStackTrace();
            messageSent = "IOException caused message to not be sent - check your lambda logs";
        }
        return messageSent;
    }

    /**
     * Method to get the current date in the format yyyy-MM-dd, the same as api.football-data.org.
     * @return the date in the formay yyyy-MM-dd.
     */
    private static String getCurrentDateAsFormattedString() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    /**
     * Method sends a text message via AWS SNS. Requires the role to have permission to
     * create SNS notifications.
     */
    private static void sendSMSMessage() {
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
    }

}