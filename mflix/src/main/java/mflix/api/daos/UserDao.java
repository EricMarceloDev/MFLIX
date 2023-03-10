package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Document> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions");
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        
    	//TODO > Ticket: Handling Errors - make sure to only add new users
        // and not users that already exist.	
		if(usersCollection.find(new Document("email", user.getEmail() )).first() != null)
			throw new IncorrectDaoOperation("Invalid User");
		
		//TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
		WriteConcern writeConcern = new WriteConcern("majority");
		usersCollection.withWriteConcern(writeConcern).insertOne(user);
        
		return true;
    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        //TODO> Ticket: User Management - implement the method that allows session information to be
        // stored in it's designated collection.
    	try {
    		if(sessionsCollection.find(new Document("jwt", jwt)).first() != null)
    			throw new IllegalArgumentException();
    		sessionsCollection.insertOne(new Document("user_id", userId).append("jwt", jwt));
            return true;
    	} catch (Exception e) {
    		e.getMessage();
    		return false;
    	}
    	
        //TODO > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        User user = null;
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        FindIterable<User> retrieved = usersCollection.find(new Document("email", email));
        
        user = retrieved.first();
        
        return user;
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //TODO> Ticket: User Management - implement the method that returns Sessions for a given
        // userId
    	FindIterable<Document> docs = sessionsCollection.find(new Document("user_id", userId));
    	Document result  = docs.first();
    	if(result == null)
    		return null;
    	Session session = new Session();
    	session.setUserId(result.getString("user_id"));
    	session.setJwt(result.getString("jwt"));
    	
        return session;
    }

    public boolean deleteUserSessions(String userId) {
        //TODO> Ticket: User Management - implement the delete user sessions method
    	DeleteResult session = sessionsCollection.deleteOne(new Document("user_id" , userId));
    	return session.wasAcknowledged();
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
    	Session session = getUserSession(email);
    	if(session != null)
    		deleteUserSessions(session.getUserId());

        //TODO> Ticket: User Management - implement the delete user method
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.
    	try {
    		return usersCollection.deleteOne(new Document("email", email)).wasAcknowledged();
    	} catch (MongoWriteException | NullPointerException mwe) {
    		log.warn("!!!!!!!!!!!!!!" + mwe.getMessage());
    	}    	
    	
        return true;
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
    	
    	if(userPreferences == null)
    		throw new IncorrectDaoOperation("NULL");
    	
    	FindIterable<User> users = usersCollection.find(new Document("email", email));
    	User user = users.first();
    	System.out.println(user.getEmail());
    	
    	Map<String, String> before = new HashMap<>();
    	System.out.println(before);

    	try {
    		usersCollection.updateOne(new Document("email", email), new Document("$set", new Document("preferences", userPreferences)));
    	} catch (Exception e) {
    		return false;
    	}
    	
    	FindIterable<User> users1 = usersCollection.find(new Document("email", email).append("preferences", userPreferences));
    	User user1 = users1.first();
    	System.out.println(user1.getEmail());
    	Map<String, String> map = user1.getPreferences();
    	System.out.println(map);

        return true;
    }
}
