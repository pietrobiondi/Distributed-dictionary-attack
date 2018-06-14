import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final String userID = "prova";
	private final String password = "prova";
	private final static int NOTBANNED = 0;
	private final static int BANNED = 1;
	private final static int RESET_TENTATIVI = 0;
	//private Object[] userAttributes = new Object[4]; //attributi utente, utilizzati dentro hashmap ACL 0=(int)tempo, 1 =(int)tentativi, 2 =(int)statoban, 3=(String)cookie
	private HashMap<String, Object[]> ACL = new HashMap<String, Object[]>();
		
	
	public LoginServlet() {
		super();
	}

	private int getMinutes() {
		SimpleDateFormat formatter = new SimpleDateFormat("mm");
		Date date = new Date();
		return Integer.parseInt(formatter.format(date));
	}
/*
	private boolean checkUserACL(String user) { // controlla la presenza dell utente nell ACL
		if (ACL.get(user) != null)
			return true;
		else
			return false;
	}
*/
	private int isBanned(String user) { // controlla stato ban utente
		return (int)ACL.get(user)[2];
	}

	private int diffTime(int userTime) {
		return (getMinutes() - userTime) % 60;
	}

	private int unBan(String user) { //vede se è il momento di sbannare
		String oldSessionID = (String)ACL.get(user)[3];
		int tentativi = (int)ACL.get(user)[1];
		if (isBanned(user) == BANNED) {
			if( (int)ACL.get(user)[1] == 4 && diffTime((int)ACL.get(user)[0]) >= 1 ) {
				ACL.put(user, new Object[] { getMinutes(), tentativi, NOTBANNED, oldSessionID});
				return 1;
			}else if((int)ACL.get(user)[1] == 7 && diffTime((int)ACL.get(user)[0]) >= 2 ) {
				ACL.put(user, new Object[] { getMinutes(), tentativi, NOTBANNED, oldSessionID});
				return 1;
			}else if( (int)ACL.get(user)[1] == 10 && diffTime((int)ACL.get(user)[0]) >= 3 ) {
				ACL.put(user, new Object[] { getMinutes(), RESET_TENTATIVI, NOTBANNED, oldSessionID});
				return 1; // sbannato
			}
			else
				return 2; // è bannato ma non è tempo di sbannarlo
		}else
			return 3; // non è bannato
	}

	private void increaseAttempts(String user) { // and ban
		String oldSessionID = (String)ACL.get(user)[3];
		int tentativi = (int)ACL.get(user)[1];
		
		if (tentativi == 4 || tentativi == 7 || tentativi == 10)
			ACL.put(user, new Object[] { getMinutes(), tentativi, BANNED, oldSessionID }); //ban
		else {
			int temp = (int)ACL.get(user)[0];
			ACL.put(user, new Object[] { temp, ++tentativi, NOTBANNED, oldSessionID });
		}
	}

	private void addUserACL(String user, String sessionID) {
		ACL.put(user, new Object[] { getMinutes(), 0, NOTBANNED, sessionID});
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String user = request.getParameter("user");
		String pwd = request.getParameter("pwd");
		String sessionID ="";
		Cookie loginCookie = new Cookie("user", user);
		loginCookie.setMaxAge(30 * 60); // setting cookie to expiry in 30 mins (scadenza)
		HttpSession session = request.getSession();
		
		System.out.println(session.getId());
		
		
		
		addUserACL(user,sessionID);
		if( unBan(user) == 2 ) {
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/login.html");
			PrintWriter out = response.getWriter();
			out.println("<font color=red>You are banned, wait X minutes, and try again.</font>");
			rd.include(request, response);
		}else if (userID.equals(user) && password.equals(pwd)) {
			session.setAttribute("user", user);
			response.addCookie(loginCookie);
			response.sendRedirect("LoginSuccess.jsp");
		}else {
			increaseAttempts(user);
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/login.html");
			PrintWriter out = response.getWriter();
			out.println("<font color=red>Either user name or password is wrong IncreaseAttempts.</font>");
			rd.include(request, response);
		}
		doGet(request, response);
	}

}