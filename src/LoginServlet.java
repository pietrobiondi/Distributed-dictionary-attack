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
	private final static int RESET_TENTATIVI = 1;
	private HashMap<String, Object[]> ACL = new HashMap<String, Object[]>();
		
	
	public LoginServlet() {
		super();
	}
	private long getTimeACL(String cookie) {
		return (long)ACL.get(cookie)[0];
	}
	private int getAttemptsACL(String cookie) {
		return (int)ACL.get(cookie)[1];
	}
	private int getStatusBanACL(String cookie) {
		return (int)ACL.get(cookie)[2];
	}
	private String getUserACL(String cookie) {
		return (String)ACL.get(cookie)[3];
	}
	private long getTimeinMills() {
		return System.currentTimeMillis();
	}

	private boolean checkCookieACL(String cookie) { // controlla la presenza del cookie nell ACL
		if (ACL.get(cookie) != null)
			return true;
		else
			return false;
	}
	
	private int isBanned(String cookie) { // controlla stato ban cookie
		return getStatusBanACL(cookie);
	}

	private int diffTime(long userTime) {
		return (int)(getTimeinMills() - userTime) / 1000;
	}

	private int unBan(String cookie) { //vede se è il momento di sbannare
		String user = getUserACL(cookie);
		int tentativi = getAttemptsACL(cookie);
		int flag;
		if (isBanned(cookie) == BANNED) {
			if( tentativi == 3 && diffTime( getTimeACL(cookie) ) >= 30 ) {
				ACL.put(cookie, new Object[] { getTimeinMills(), tentativi, NOTBANNED, user});
				flag = 1;
			}else if( tentativi == 6 && diffTime( getTimeACL(cookie) ) >=40 ) {
				ACL.put(cookie, new Object[] { getTimeinMills(), tentativi, NOTBANNED, user});
				flag = 1;
			}else if( tentativi == 10 && diffTime( getTimeACL(cookie) ) >= 50 ) {
				ACL.put(cookie, new Object[] { getTimeinMills(), RESET_TENTATIVI, NOTBANNED, user});
				flag = 1; // sbannato
			}
			else
				flag = 2; // è bannato ma non è tempo di sbannarlo
		}else
			flag = 3; // non è bannato
		
		return flag;	
	}
	
	private void Ban(String cookie, int tentativi,String user) {
			ACL.put(cookie, new Object[] { getTimeinMills(), tentativi, BANNED, user }); //ban
	}
	
	private void increaseAttempts(String cookie) {
		String user = getUserACL(cookie);
		int tentativi = getAttemptsACL(cookie);
		long temp = getTimeACL(cookie);
		ACL.put(cookie, new Object[] { temp, ++tentativi, NOTBANNED, user });
		if (tentativi == 3 || tentativi == 6 || tentativi == 10)
			Ban(cookie,tentativi,user);
	}

	private void addUserACL(String cookie, String user) {
		if(!checkCookieACL(cookie))
			ACL.put(cookie, new Object[] { getTimeinMills(), RESET_TENTATIVI, NOTBANNED, user});
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath()).append("STATUS: "+response.getStatus());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String user = request.getParameter("user");
		String pwd = request.getParameter("pwd");
		Cookie loginCookie = new Cookie("user", user);
		loginCookie.setMaxAge(30 * 60); // setting cookie to expiry in 30 mins (scadenza)
		HttpSession session = request.getSession();
		String sessionID = session.getId(); // cookie
		System.out.println("ID SESSIONE: "+sessionID);

		addUserACL(sessionID,user);
		if( unBan(sessionID) == 2 ) {
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/login.html");
			PrintWriter out = response.getWriter();
			out.println("<font color=red>You are banned Wait and try later again.</font>");
			response.setStatus(270); //banned
			rd.include(request, response);
		}else if (userID.equals(user) && password.equals(pwd)) {
			ACL.put(sessionID, new Object[] { getTimeinMills(), RESET_TENTATIVI, NOTBANNED, user});
			session.setAttribute("user", user);
			response.addCookie(loginCookie);
			response.sendRedirect("LoginSuccess.jsp");
		}else {
			increaseAttempts(sessionID);
			System.out.println("TENTATIVI =" + getAttemptsACL(sessionID));
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/login.html");
			PrintWriter out = response.getWriter();
			out.println("<font color=red>Either user name or password is wrong IncreaseAttempts.</font>");
			response.setStatus(271); //hai ancora tentativi
			rd.include(request, response);
		}
		doGet(request, response);
	}

}