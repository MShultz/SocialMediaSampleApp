import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class Application {
	Cluster.Builder cluster;
	Session session;
	
	
	public Application(){
		cluster = Cluster.builder().addContactPoint("13.91.46.223").withPort(9042).withCredentials("cassandra", "bitnami");
		session = cluster.build().connect();
		//createApplication("SM");
		
	}
	public void createApplication(String keySpace){
		createKeySpace(keySpace);
		createSchema();
	}
	private void createKeySpace(String keySpace){
		String cqlStatement = "CREATE KEYSPACE " + keySpace + " WITH replication = "
				+ " {'class':'SimpleStrategy','replication_factor':1}";        
		session.execute(cqlStatement);
	}
	private void createSchema(){
		String use = "Use SM";
		session.execute(use);
		
		String createUsers = "CREATE TABLE SM.users (" + 
                " user_name varchar PRIMARY KEY," + 
                " password varchar " + 
                ")";
		String createFollowing = "CREATE TABLE SM.following (" + 
                " user_name varchar PRIMARY KEY," + 
                " followed_user_Names list<text> " + 
                ")";
		String createPostWall = "CREATE TABLE SM.posts (" + 
                " post_ID timestamp," + 
                " user_Name varchar, " + 
                " content text,"+
                "PRIMARY KEY (post_ID, user_Name))";

		session.execute(createUsers);
		session.execute(createFollowing);
		session.execute(createPostWall);
		
	}
	public Session getSession(){
		return session;
	}
	public void shutdownConnection(){
		session.close();
	}
}
