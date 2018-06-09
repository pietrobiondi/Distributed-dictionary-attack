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
	private HashMap<String, int[]> ACL = new HashMap<String, int[]>();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LoginServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	private int getMinutes() {
		SimpleDateFormat formatter = new SimpleDateFormat("mm");
		Date date = new Date();
		return Integer.parseInt(formatter.format(date));
	}

	private boolean checkUserACL(String user) {
		// controlla la presenza dell utente nell ACL
		if (ACL.get(user) != null)
			return true;
		else
			return false;
	}

	private int isBanned(String user) {
		// controlla stato ban utente
		return ACL.get(user)[2];
	}

	private int diffTime(int userTime) {
		return (getMinutes() - userTime) % 60;
	}

	private boolean unBan(String user) {
		if (isBanned(user) == BANNED && diffTime(ACL.get(user)[0]) >= 3) {
			// se è bannato e la diff è >=3 allora è da sbannare
			ACL.put(user, new int[] { getMinutes(), 0, NOTBANNED });
			return true;
		} else
			return false;
	}

	private void increaseAttempts(String user) {
		int tentativi = ACL.get(user)[1];

		if (tentativi >= 3)
			ACL.put(user, new int[] { getMinutes(), 0, BANNED });
		else
			ACL.put(user, new int[] { getMinutes(), ++tentativi, NOTBANNED });

		System.out.println("Accesso negato.. IncAtt, tentativi =" + tentativi);
	}

	private void addUserACL(String user) {
		ACL.put(user, new int[] { getMinutes(), 1, NOTBANNED });
		System.out.println("Accesso negato primo tentativo");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// get request parameters for userID and password
		String user = request.getParameter("user");
		String pwd = request.getParameter("pwd");

		if (checkUserACL(user) == true && !(unBan(user) == false)) {
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/login.html");
			PrintWriter out = response.getWriter();
			out.println("<font color=red>You are banned, wait 3 minutes, and try again.</font>");
			rd.include(request, response);
		} else if (userID.equals(user) && password.equals(pwd)) {
			Cookie loginCookie = new Cookie("user", user);
			// setting cookie to expiry in 30 mins
			loginCookie.setMaxAge(30 * 60);
			HttpSession session = request.getSession();
			session.setAttribute("user", user);
			response.addCookie(loginCookie);
			response.sendRedirect("LoginSuccess.jsp");
		} else {
			/*
			  caso 1: utente non in ACL e credenziali sbagliate
			  caso 2: utente presente in ACL e credenziali sbagliate
			 */
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