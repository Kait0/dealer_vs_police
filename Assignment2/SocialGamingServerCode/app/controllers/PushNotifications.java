package controllers;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Map;

import org.json.simple.JSONObject;
import util.GameConfiguration;

import javax.net.ssl.HttpsURLConnection;

/**
 * This is a simple collection of methods to send push messages to the
 * client devices vie firebase cloud messaging.
 *
 * The recipient is always the device Id of the device you want the
 * message to be sent to.
 *
 * The message itself is not just a text message, but a Map of strings to strings
 * for more Information on maps:
 * https://docs.oracle.com/javase/tutorial/collections/interfaces/map.html
 */
public class PushNotifications {

	private static final String FCMServerURL = "https://fcm.googleapis.com/fcm/send";

	/**
	 * For a given recipient this method tries to send the message using Firebase
	 * Note that the message to be sent is a Map of Strings to Strings
	 * Also values should be not null
	 * TTL is set to a default of 4 weeks (the maximum)
	 * @param recipient the device token from the smartphone
	 * @param message the message to be sent
	 * @throws IOException
	 */
	public static void sendMessage(String recipient, Map<String,String> message) throws IOException
	{
		sendMessage(recipient,message,4*7*24*60*60);
	}

	/**
	 * For a given recipient this method tries to send the message using Firebase
	 * Note that the message to be sent is a Map of Strings to Strings
	 * Also values should be not null
	 * @param recipient the device token from the smartphone
	 * @param message the message to be sent
	 * @param ttl (seconds) how long the message will be on the Firebase servers when the device is offline
	 * @throws IOException
	 */
	public static void sendMessage(String recipient, Map<String,String> message, int ttl) throws IOException
	{
		// more examples:
		// https://stackoverflow.com/questions/1051004/how-to-send-put-delete-http-request-in-httpurlconnection

		// connect to the FCM Server
		URL url = new URL(FCMServerURL);

		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

		// as we want to send some additional data this must be set to true
		// https://stackoverflow.com/questions/8587913/what-exactly-does-urlconnection-setdooutput-affect
		connection.setDoOutput(true);

		// the Firebase API requires the action to be PUT
		// and the headers to have specific values
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Authorization","key="+GameConfiguration.firebaseServerAuthToken);
		connection.setRequestProperty("Content-Type","application/json");

		// now build the actual data to be sent
		JSONObject requestBody = new JSONObject();
		requestBody.put("to",recipient);
		JSONObject data = new JSONObject();
		for( String key : message.keySet() )
		{
			data.put(key,message.get(key));
		}
		requestBody.put("data",data);
		requestBody.put("time_to_live",ttl);

		// for even more things you can add, see here:
		// https://firebase.google.com/docs/cloud-messaging/http-server-ref
		// extend this as needed

		// append the additional data (that is, the message) to the request
		OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
		out.write(requestBody.toJSONString());
		out.close();

		// https://stackoverflow.com/questions/4844535/why-do-you-have-to-call-urlconnectiongetinputstream-to-be-able-to-write-out-to
		connection.getInputStream();
	}
}
