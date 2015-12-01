package woftracker.record;

import woftracker.*;
import java.util.*;

public enum BonusAmount {
	$30000(30000), $32000(32000), $33000(33000), VEHICLE(0), $35000(35000), $40000(40000), $45000(45000), $50000(50000), $100000(100000), ONE_MILLION(1000000);
	
	public static final Map<String, Set<BonusAmount>> GROUPING_MAP = new HashMap<>();
	static {
		GROUPING_MAP.put("$3X000", EnumSet.of($30000, $32000, $33000));
	}
	
	private int amount;
	private String properName;
	
	BonusAmount(int amount) {
		this.amount = amount;
		properName = name().replace('_', ' ');
	}
	
	public int getAmount() {
		return amount;
	}
	
	public static final Map<String, BonusAmount> LOOKUP = new HashMap<>();
	static {
		for(BonusAmount bA : BonusAmount.values())
			if(bA != VEHICLE) {
				LOOKUP.put(bA.properName, bA);
				if(bA != ONE_MILLION)
					LOOKUP.put(bA.properName.substring(1), bA);	//no $
				else
					LOOKUP.put(String.valueOf(1000000), bA);
				LOOKUP.put(String.valueOf(bA.amount/1000), bA);
			}
	}
	
	public String toString() {
		return properName;
	}
}