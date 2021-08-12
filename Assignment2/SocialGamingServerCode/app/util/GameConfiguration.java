package util;

/**
 * Simple class that holds all configuration specific information for the game.
 * 
 * @author Niklas Klügel
 *
 */
public class GameConfiguration {

	// TODO insert your firebase cloud messaging server auth token here
	public static final String firebaseServerAuthToken = "AAAADuEaDHw:APA91bHKmEgGHN5YZ23Q8hVlCJAiZ9oLnciZ1ERdIU8XMYBFe0MvjN8OHNQWZ9ssGkypwpNAIKbz_K7CKSVPHIaw0hUgpkXHoKxYbyne0LcYYdV_iqpgmRGM4BEJ3xPnlJ2xJrU9IBoP";
	
	// limits the maximum distance for user lookups (meters)
	public static double MaxDistanceOfUserForNearbyUsers = 1000000.0;
	public static int MaxNumberOfReturnedUsers = 20;
	
	// this is the time to live for a user login to be still valid
	// e.g. if chosen as an opponent the user has to logged less than these seconds ago
	public static long MaxTimeForLoginTimeOutInSeconds = 3600;

}
