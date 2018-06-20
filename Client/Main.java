public class Main {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String url = "http://localhost:8080/Distribuiti/login.html";
		String login = "http://localhost:8080/Distribuiti/LoginServlet";
		String dictonary = "dizionario.txt";
		String exName = "router";
		
		Attaccker c1 = new Attaccker(url, login, dictonary, exName, "Attaccker1");
		Attaccker c2 = new Attaccker(url, login, dictonary, exName, "Attaccker2");
		
		c1.receivedMessage();
		c2.receivedMessage();
		c1.start();
		c2.start();
		
	}
}
