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
	private HashMap<String, int[]> ACL = new HashMap<String, int[]>();

	public LoginServlet() {
		super();
	}

	private int getMinutes() {
		SimpleDateFormat formatter = new SimpleDateFormat("mm");
		Date date = new Date();
		return Integer.parseInt(formatter.format(date));
	}

	private boolean checkUserACL(String user) { // controlla la presenza dell utente nell ACL
		if (ACL.get(user) != null)
			return true;
		else
			return false;
	}

	private int isBanned(String user) { // controlla stato ban utente
		return ACL.get(user)[2];
	}

	private int diffTime(int userTime) {
		return (getMinutes() - userTime) % 60;
	}

	private int unBan(String user) {
		if (isBanned(user) == BANNED) {
			if (diffTime(ACL.get(user)[0]) >= 3) { // ha già aspettato 3 minuti? se si sban.
				ACL.put(user, new int[] { getMinutes(), RESET_TENTATIVI, NOTBANNED });
				return 1; // 1 = sbannato
			} else
				return 2; // è bannato ma non è tempo di sbannarlo
		} else
			return 3; // non è bannato
	}

	private void increaseAttempts(String user) {
		int tentativi = ACL.get(user)[1];

		if (tentativi >= 3)
			ACL.put(user, new int[] { getMinutes(), RESET_TENTATIVI, BANNED });
		else {
			int temp = ACL.get(user)[0];
			ACL.put(user, new int[] { temp, ++tentativi, NOTBANNED });
		}
	}

	private void addUserACL(String user) {
		ACL.put(user, new int[] { getMinutes(), 1, NOTBANNED });
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String user = request.getParameter("user");
		String pwd = request.getParameter("pwd");

		if (checkUserACL(user) == true && unBan(user) == 2) {
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/login.html");
			PrintWriter out = response.getWriter();
			out.println("<font color=red>You are banned, wait 3 minutes, and try again.</font>");
			rd.include(request, response);
		} else if (userID.equals(user) && password.equals(pwd)) {
			Cookie loginCookie = new Cookie("user", user);
			loginCookie.setMaxAge(30 * 60); // setting cookie to expiry in 30 mins
			HttpSession session = request.getSession();
			session.setAttribute("user", user);
			response.addCookie(loginCookie);
			response.sendRedirect("LoginSuccess.jsp");
		} else {
			if (checkUserACL(user) == false)
				addUserACL(user);
			else
				increaseAttempts(user);

			RequestDispatcher rd = getServletContext().getRequestDispatcher("/login.html");
			PrintWriter out = response.getWriter();
			out.println("<font color=red>Either user name or password is wrong.</font>");
			rd.include(request, response);
		}
		doGet(request, response);
	}

}