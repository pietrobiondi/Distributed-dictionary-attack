import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Client {

  private List<String> cookies;
  private HttpURLConnection conn;

  private final String USER_AGENT = "Mozilla/5.0";

  public static void main(String[] args) throws Exception {

	String url = "http://localhost:8080/Distribuiti/login.html";
	String login = "http://localhost:8080/Distribuiti/LoginServlet";
	String line = "";
	List<String> lines = new ArrayList<String>();
	List<Integer> indexRecived = new ArrayList<Integer>(); //parole ricevute da altri client
	int index [] = new int[3];
	int response[] = new int[3];

	Client http = new Client();

	// make sure cookies is turn on
	CookieHandler.setDefault(new CookieManager());

	// 1. Send a "GET" request, so that you can extract the form's data.
	String page = http.GetPageContent(url);
	
	BufferedReader abc = new BufferedReader(new FileReader("dizionario.txt"));

	while((line = abc.readLine()) != null) {
	    lines.add(line);

	}
	abc.close();
	indexRecived.add(5);
	indexRecived.add(9);
	indexRecived.add(0);
	indexRecived.add(33);
	indexRecived.add(10);
	indexRecived.add(38);
	indexRecived.add(8);
	indexRecived.add(17);
	indexRecived.add(13);
	indexRecived.add(20);
	indexRecived.add(2);
	

	String[] data = lines.toArray(new String[]{});
	
	//scelta indici random da dizionario
	index[0] = (int)(Math.random() * (data.length));
	
	while(indexRecived.contains(index[0])) {
		
		index[0] = (int)(Math.random() * (data.length));
	}
	
	index[1] = (int)(Math.random() * (data.length));
	while(indexRecived.contains(index[1])) {
		
		index[1] = (int)(Math.random() * (data.length));
	}
	
	index[2] = (int)(Math.random() * (data.length));
	while(indexRecived.contains(index[2])) {
		index[2] = (int)(Math.random() * (data.length));
	}
	
	String postParams[] = new String[3];
	for(int i=0;i<3;i++) {
		postParams[i] = http.getFormParams(page, "prova", data[index[i]]);
	}
	 

	// 2. Construct above post's content and then send a POST request for
	// authentication
	for(int i=0;i<3;i++) {
		response[i] = http.sendPost(login, postParams[i]);
	}
	
	//Qui si deve considerare response[] ed agire di conseguenza.
	
	//String result = http.GetPageContent(login);
	//System.out.println(result);
  }

  private int sendPost(String url, String postParams) throws Exception {

	URL obj = new URL(url);
	conn = (HttpURLConnection) obj.openConnection();

	// Acts like a browser
	conn.setUseCaches(false);
	conn.setRequestMethod("POST");
	conn.setRequestProperty("Host", "localhost:8080");
	conn.setRequestProperty("User-Agent", USER_AGENT);
	conn.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
	conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
	/*for (String cookie : this.cookies) {
		conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
	}*/
	conn.setRequestProperty("Connection", "keep-alive");
	conn.setRequestProperty("Referer", "http://localhost:8080/Distribuiti/login.html");
	conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	conn.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

	conn.setDoOutput(true);
	conn.setDoInput(true);

	// Send post request
	DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
	wr.writeBytes(postParams);
	wr.flush();
	wr.close();

	int responseCode = conn.getResponseCode();
	System.out.println("\nSending 'POST' request to URL : " + url);
	System.out.println("Post parameters : " + postParams);
	System.out.println("Response Code : " + responseCode);

	BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	String inputLine;
	StringBuffer response = new StringBuffer();

	while ((inputLine = in.readLine()) != null) {
		response.append(inputLine);
	}
	in.close();
	// System.out.println(response.toString());
	
	return responseCode;
  }

  private String GetPageContent(String url) throws Exception {

	URL obj = new URL(url);
	conn = (HttpURLConnection) obj.openConnection();

	// default is GET
	conn.setRequestMethod("GET");

	conn.setUseCaches(false);

	// act like a browser
	conn.setRequestProperty("User-Agent", USER_AGENT);
	conn.setRequestProperty("Accept",
		"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
	conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
	if (cookies != null) {
		for (String cookie : this.cookies) {
			conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
		}
	}
	int responseCode = conn.getResponseCode();
	System.out.println("\nSending 'GET' request to URL : " + url);
	System.out.println("Response Code : " + responseCode);

	BufferedReader in = 
            new BufferedReader(new InputStreamReader(conn.getInputStream()));
	String inputLine;
	StringBuffer response = new StringBuffer();

	while ((inputLine = in.readLine()) != null) {
		response.append(inputLine);
	}
	in.close();

	// Get the response cookies
	setCookies(conn.getHeaderFields().get("Set-Cookie"));

	return response.toString();

  }

  public String getFormParams(String html, String username, String password)
		throws UnsupportedEncodingException {

	System.out.println("Extracting form's data...");

	Document doc = Jsoup.parse(html);

	// Google form id
	Element loginform = doc.getElementById("formLogin");
	Elements inputElements = loginform.getElementsByTag("input");
	inputElements.remove(2);
	List<String> paramList = new ArrayList<String>();
	for (Element inputElement : inputElements) {
		String key = inputElement.attr("name");
		String value = inputElement.attr("value");

		if (key.equals("user"))
			value = username;
		else if (key.equals("pwd"))
			value = password;
		paramList.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
	}

	// build parameters list
	StringBuilder result = new StringBuilder();
	
	for (String param : paramList) {
		if (result.length() == 0) {
			result.append(param);
		} else {
			result.append("&" + param);
		}
	}
	return result.toString();
  }

  public List<String> getCookies() {
	return cookies;
  }

  public void setCookies(List<String> cookies) {
	this.cookies = cookies;
  }

}