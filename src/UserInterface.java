import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import main.Menu;

public class UserInterface {
	private Menu lib;
	Session session;
	private String[] loginMenuOptions = { "Create an Account", "Login with an Existing Account" };

	public UserInterface(Session session) {
		this.session = session;
		lib = new Menu();
	}

	public boolean loginMenu() {
		System.out.println("Welcome to SocialMedia!");
		return determineRoute(lib.promptForMenuSelection(loginMenuOptions, true));
	}

	public void createUserPrompt() {
		String userName;
		boolean exists;
		do {
			userName = lib.promptForInput("Please Input your username", false);
			exists = checkIfUsernameExists(userName);
			if(exists) System.out.println("That username already exists.");
		} while (exists);
		String password = lib.promptForInput("Please Input your password", false);
		createUser(userName, password);
	}

	public void login() {
		String userName;
		boolean exists;
		do {
			userName = lib.promptForInput("Please Input your username", false);
			exists = checkIfUsernameExists(userName);
			if(!exists) System.out.println("Oops! That's not a vailid username!");
		} while (!exists);
		String password;
		boolean valid = false;
		do{
			password = lib.promptForInput("Please Input your password", false);
			Statement getData = QueryBuilder.select().from("SM", "users").allowFiltering().where(QueryBuilder.eq("password", password)).and(QueryBuilder.eq("user_Name", userName));
			ResultSet results = session.execute(getData);
			valid = !results.isExhausted();
			if(!valid) System.out.println("That username is invalid");
		}while(!valid);
		System.out.println("Sweet! You're into Social Media!");
		
		
		
	}

	private boolean determineRoute(int userChoice) {
		boolean continuePlz = true;
		switch (userChoice) {
		case 1:
			createUserPrompt();
			continuePlz = true;
			break;
		case 2:
			login();
			continuePlz = true;
			break;
		case 0:
			System.out.println("GoodBye!");
			continuePlz = false;
			break;
		}
		return continuePlz;
	}

	private boolean checkIfUsernameExists(String userName) {
			Statement ifExistsQuery = QueryBuilder.select().from("SM", "users").where(QueryBuilder.eq("user_name", userName));
			ResultSet results= session.execute(ifExistsQuery);	
			return !results.isExhausted();
	}
	private void createUser(String userName, String password){
		Statement createUser = QueryBuilder.insertInto("SM","users").value("user_name", userName).value("password", password);
		session.execute(createUser);
		System.out.println("User Created");
	}
	
}
