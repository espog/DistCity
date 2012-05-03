package bankNode;

import java.util.HashMap;

public class BankNode {
	
	private HashMap BankMap;

	public BankNode() {
		BankMap = new HashMap(10);
	}
	
	public HashMap getBankMap(){
		return BankMap;
	}

}
