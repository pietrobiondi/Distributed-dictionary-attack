
public aspect LogServer {
	pointcut callDoPost(String session): execution(* LoginServlet.addUserACL(..)) && args(session, ..);

	after(String session) : callDoPost(session)  {
		System.out.println("ID: "+session+ " Ha provato a connettersi");
	}

}
