
public aspect LogAttacker {
	pointcut callSendPost(Attaccker a, String i): call(* Attaccker.sendPost(..)) && this(a) && args(..,i);

	before(Attaccker a, String i) : callSendPost(a,i) {
		
		System.out.println(a.getNameAttack()+" provo questa combinazione "+i+"\n");
	}

	after(Attaccker a, String i) returning(int r) : callSendPost(a,i)  {
		System.out.println(a.getNameAttack()+": La coppia "+i+" ha generato la seguente RESPONSE: "+r+"\n");
	}

}
