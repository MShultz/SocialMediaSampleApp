
public class main {	
	public static void main(String[] args) {
		Application app = new Application();
		UserInterface application = new UserInterface(app.getSession());
		
		boolean continueApp = true;
		while(continueApp){
			continueApp = application.loginMenu();
		}
		app.shutdownConnection();
		System.exit(0);
		
	}

}
