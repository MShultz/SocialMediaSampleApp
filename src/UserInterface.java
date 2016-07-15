import java.util.ArrayList;
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
				System.out.println("Oops! That's not a valid username!");
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
		Statement createUserinFollowing = QueryBuilder.insertInto("SM", "following").value("user_name", userName);
		session.execute(createUserinFollowing);
		System.out.println("User Created");
	}

	public void userMenu(String userName) {
		boolean logout;
		String[] userMenu = { "Create a Post", "View All Posts", "View Posts from a User",
				"View posts from Users you follow", "Follow a User", "Delete a Post", "View all Users", "Logout" };
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
			viewPosts(false, lib.promptForInput("Input the name of the user you'd like to find", false));
			break;
		case 4:
			getFollowedPosts(userName);
			break;
		case 5:
			followAUser(userName, lib.promptForMenuSelection(getAllUsers(), true));
			break;
		case 6:
			deletePost(userName);
			break;
		case 7:
			printAllUsers(getAllUsers());
			break;
		case 8:
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
		printPosts(all);
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
					"Alright. It is gone forever. You're not getting it back. Do you know how much work it took to allow you to delete that garbage?");
		}
	}

	public void getFollowedPosts(String userName) {
		Statement getFollowedPosts = QueryBuilder.select().from("SM", "posts").allowFiltering()
				.where(QueryBuilder.in("user_name", getFollowingList(userName)));
		ResultSet followedPosts = session.execute(getFollowedPosts);
		printPosts(followedPosts);
	}

	public void printPosts(ResultSet posts) {
		for (Row r : posts.all()) {
			System.out.println("-----------------------------------------------");
			System.out.println(r.getString("user_name") + " posted this" + ": \n\n" + r.getString("content"));
			Date date2 = r.getTimestamp("post_id");
			System.out.println("\t\tAt " + date2);
			System.out.println("-----------------------------------------------");
		}
	}

	public void followAUser(String userName, int userToFollow) {
		if (userToFollow != 0) {
			if (isFollowing(userName, getAllUsers()[userToFollow - 1]))
				System.out.println("You're already following that user, Little Muffin Top :(");
			else {
				List<String> newList = getFollowingList(userName);
				newList.add(getAllUsers()[userToFollow - 1]);
				Statement addUser = QueryBuilder.update("SM", "following")
						.with(QueryBuilder.set("followed_user_Names", newList))
						.where(QueryBuilder.eq("user_name", userName));
				session.execute(addUser);
				System.out.println("You are now following " + getAllUsers()[userToFollow-1] + "!");
			}
		}
	}

	public boolean isFollowing(String userName, String userToFollow) {
		boolean isFollowing = false;
		for (String s : getFollowingList(userName)) {
			if (s.equals(userToFollow))
				isFollowing = true;
		}
		return isFollowing;
	}

	public List<String> getFollowingList(String userName) {
		Statement following = QueryBuilder.select().from("SM", "following")
				.where(QueryBuilder.eq("user_name", userName));
		List<Row> userFollows = session.execute(following).all();
		Row follows = userFollows.get(0);
		return follows.getList("followed_user_Names", String.class);
	}

	public String[] getAllUsers() {
		ArrayList<String> currentUsers = new ArrayList<String>();
		Statement users = QueryBuilder.select("user_name").from("SM", "users");
		List<Row> usersplz = session.execute(users).all();
		for (Row r : usersplz) {
			currentUsers.add(r.getString("user_name"));
		}
		String[] pleaseWork = new String[currentUsers.size()];
		return currentUsers.toArray(pleaseWork);
	}

	public void printAllUsers(String[] users) {
	for(int i = 1; i < users.length + 1; ++i){
		System.out.print(users[i-1] + ", ");
		if(i % 5 == 0){
			System.out.println("");
		}
	}
		System.out.println("");
	}
}
