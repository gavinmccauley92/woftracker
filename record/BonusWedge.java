package woftracker.record;

import java.util.*;

public enum BonusWedge {
	A_of_AMERICAS_1, M_of_AMERICAS, E_of_AMERICAS, R_of_AMERICAS, I_of_AMERICAS, C_of_AMERICAS, A_of_AMERICAS_2, Apostrophe, S_of_AMERICAS, Star1,
	G_of_GAME, A_of_GAME, M_of_GAME, E_of_GAME, Star2, S_of_SPIN, P_of_SPIN, I_of_SPIN, N_of_SPIN, Ampersand, W_of_WIN, I_of_WIN, N_of_WIN, Star3;
	
	public static final Map<String, Set<BonusWedge>> GROUPING_MAP = new HashMap<>();
	static {
		GROUPING_MAP.put("AMERICA'S",EnumSet.range(A_of_AMERICAS_1, S_of_AMERICAS));
		GROUPING_MAP.put("GAME", EnumSet.range(G_of_GAME, E_of_GAME));
		GROUPING_MAP.put("SPIN&WIN", EnumSet.range(S_of_SPIN, N_of_WIN));
		GROUPING_MAP.put("*/**/***", EnumSet.of(Star1, Star2, Star3));
	}
	
	private String properName;
	
	BonusWedge() {
		properName = formalize();
	}
	
	private String formalize() {
		String s = name().replace('_', ' ').replace("CAS", "CA\'S");
		
		if(s.equals("Apostrophe"))
			return "\'";
			
		if(s.equals("Ampersand"))
			return "&";
		
		if(s.indexOf("Star") != -1) {
			char c = s.charAt(s.length()-1);
			s = "";
			
			for(char i = '0'; i < c; i++)
				s += '*';
			
			return s;
		}
		
		char c;
		if(Character.isDigit((c = s.charAt(s.length()-1))))
			return s.substring(0, s.length() - 2) + " #" + (c == '1' ? "1" : "2");
		
		return s;
	}
	
	public static final Map<String, BonusWedge> LOOKUP = new HashMap<>();
	static {
		for(BonusWedge bW : BonusWedge.values()) {
			if(bW == A_of_AMERICAS_1) {
				LOOKUP.put("1st A of AMERICA'S", bW);
				LOOKUP.put("1st A in AMERICA'S", bW);
			} else if(bW == A_of_AMERICAS_2) {
				LOOKUP.put("2nd A of AMERICA'S", bW);
				LOOKUP.put("2nd A of AMERICA'S", bW);
			}
			LOOKUP.put(bW.properName, bW);
			if(bW.properName.contains("of"))
				LOOKUP.put(bW.properName.replace("of", "in"), bW);
		}
	}
	
	public String toString() {
		return properName;
	}
	
	public static void main(String[] args) {
		for(BonusWedge bW : BonusWedge.values())
			System.out.println(bW);
	}
}