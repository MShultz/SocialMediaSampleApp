import java.util.Date;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import main.Menu;

public class UserInterface {
	private Menu lib;
	Session session;
	Date date;
	private String[] loginMenuOptions = { "Create an Account", "Login with an Existing Account" };

	public UserInterface(Session session) {
		this.session = session;
		date = new Date();
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
			if (exists)
				System.out.println("That username already exists.");
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
			if (!exists)
				System.out.println("Oops! That's not a vailid username!");
		} while (!exists);
		String password;
		boolean valid = false;
		do {
			password = lib.promptForInput("Please Input your password", false);
			Statement getData = QueryBuilder.select().from("SM", "users").allowFiltering()
					.where(QueryBuilder.eq("password", password)).and(QueryBuilder.eq("user_Name", userName));
			ResultSet results = session.execute(getData);
			valid = !results.isExhausted();
			if (!valid)
				System.out.println("That username is invalid");
		} while (!valid);
		System.out.println("Sweet! You're into Social Media!");
		userMenu(userName);

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
		Statement ifExistsQuery = QueryBuilder.select().from("SM", "users")
				.where(QueryBuilder.eq("user_name", userName));
		ResultSet results = session.execute(ifExistsQuery);
		return !results.isExhausted();
	}

	private void createUser(String userName, String password) {
		Statement createUser = QueryBuilder.insertInto("SM", "users").value("user_name", userName).value("password",
				password);
		session.execute(createUser);
		System.out.println("User Created");
	}

	public void userMenu(String userName) {
		boolean logout;
		String[] userMenu = { "Create a Post", "View All Posts", "View Posts from a User", "Delete a Post", "Logout" };
		do {
			logout = determineUserRoute(lib.promptForMenuSelection(userMenu, false), userName);
		} while (!logout);
	}

	public boolean determineUserRoute(int choice, String userName) {
		boolean logoutChoice = false;
		switch (choice) {
		case 1:
			createPost(userName);
			break;
		case 2:
			viewPosts(true, null);
			break;
		case 3:
			viewPosts(false, lib.promptForInput("Input the name of the user you'd like to find.", false));
			break;
		case 4:
			deletePost(userName);
			break;
		case 5:
			logoutChoice = true;
			break;
		}
		return logoutChoice;
	}

	public void createPost(String userName) {
		String content = lib.promptForInput("Insert your heart and soul here", false);
		Statement createPost = QueryBuilder.insertInto("SM", "posts").value("post_id", date.getTime())
				.value("user_name", userName).value("content", content);
		session.execute(createPost);
		System.out.println("Awesome! Your post has been created, " + userName + "!");
	}

	public void viewPosts(boolean isAll, String userChoice) {
		Statement getAllPosts;
		getAllPosts = (isAll ? QueryBuilder.select().from("SM", "posts").allowFiltering()
				: QueryBuilder.select().from("SM", "posts").allowFiltering()
						.where(QueryBuilder.eq("user_name", userChoice)));
		ResultSet all = session.execute(getAllPosts);
		for (Row r : all.all()) {
			System.out.println("-----------------------------------------------");
			System.out.println(r.getString("user_name") + " posted this" + ": \n\n" + r.getString("content"));
			Date date2 = r.getTimestamp("post_id");
			System.out.println("\t\tAt " + date2);
			System.out.println("-----------------------------------------------");
		}
	}

	public void deletePost(String userName) {
		Statement deletePost = QueryBuilder.select().from("SM", "posts").allowFiltering()
				.where(QueryBuilder.eq("user_name", userName));
		System.out.println("Which one would you like to delete?");
		ResultSet userPosts = session.execute(deletePost);
		List<Row> l = userPosts.all();
		String[] posts = new String[l.size()];
		for (int i = 0; i < posts.length; ++i) {
			posts[i] = l.get(i).getString("content");
		}
		int decisionToEndAllDecisions = lib.promptForMenuSelection(posts, true);

		if (decisionToEndAllDecisions != 0) {
			Statement actuallyDeletePost = QueryBuilder.delete().from("SM", "posts")
					.where(QueryBuilder.eq("post_id", l.get(decisionToEndAllDecisions - 1).getTimestamp("post_id")));
			session.execute(actuallyDeletePost);
			System.out.println(
					"Alright. It is gone forever. You're not getting it back. Do you know how much work it took to allow you to delete that garbage?1");
		}
	}
}
