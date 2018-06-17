
public class Main {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String url = "http://localhost:8080/Distribuiti/login.html";
		String login = "http://localhost:8080/Distribuiti/LoginServlet";
		String dictonary = "dizionario.txt";
		
		Attaccker c1 = new Attaccker(url, login, dictonary);
		
		c1.start();
		
	}

}
