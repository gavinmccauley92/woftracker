package woftracker.record;

import woftracker.util.FormatFactory;
import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;

public enum Category {
	//& not allowed in identifier, use $ for single character simplicity. It's OK since I convert it below.
	THING(true), PHRASE(false), BEFORE_$_AFTER(false), SAME_NAME(false), SAME_LETTER(false), PLACE(true), PERSON(true), WHAT_ARE_YOU_DOING(false), EVENT(true),
	FUN_$_GAMES(false), PROPER_NAME(true), FOOD_$_DRINK(false), ON_THE_MAP(false), LIVING_THING(true), RHYME_TIME(false), AROUND_THE_HOUSE(false), IN_THE_KITCHEN(false), OCCUPATION(true), 
	LANDMARK(true), WHATS_THAT_SONG(false), SONG_LYRICS(false), SHOW_BIZ(false), ROCK_ON$(false), THE_70S(false), THE_80S(false), THE_90S(false), QUOTATION(false), TV_QUOTE(false), MOVIE_QUOTE(false),
	FICTIONAL_CHARACTER(true), FICTIONAL_FAMILY(false), FICTIONAL_PLACE(true), COLLEGE_LIFE(false), HEADLINE(false), TITLE$AUTHOR(false), BEST_SELLER(false), HUSBAND_$_WIFE(false),
	FAMILY(false), SONG_$_ARTIST(false), STAR_$_ROLE(false), TITLE(true), SONG_TITLE(true), MOVIE_TITLE(true), TV_TITLE(true), CLASSIC_TV(false);
	
	private String singleName, pluralName, titleCase;
	private static boolean allowPlural = false, useBoth = false;
	
	public static final EnumSet<Category> TRIVIA = EnumSet.of(WHATS_THAT_SONG, TV_QUOTE, MOVIE_QUOTE);
	public static final Map<String, Category> LOOKUP = new HashMap<>(), PLURAL_LOOKUP = new HashMap<>();
	
	Category(boolean plural) {
		singleName = formalize(this, false);
		pluralName = plural ? formalize(this, true) : singleName;
		
		titleCase = FormatFactory.toTitleCase(singleName.toLowerCase());
	}
	
	public String getTitleCase() {
		return titleCase;
	}
	
	public static void setPlural(boolean b) {
		allowPlural = b;
	}
	
	public static boolean isCurrentlyPlural() {
		return allowPlural;
	}
	
	public static void setBoth(boolean b) {
		useBoth = b;
	}
	
	private static String formalize(Category c, boolean usePlural) {
		String s = c.name();
		if(s.equals("WHAT_ARE_YOU_DOING"))
			return "WHAT ARE YOU DOING?";	//special case
		if(s.equals("WHATS_THAT_SONG"))
			return "WHAT'S THAT SONG?";		//special case
		if(s.endsWith("0S"))
			s = s.replace("S", "\'S");	//70'S/80'S/90'S
		if(s.equals("TITLE$AUTHOR"))
			return "TITLE/AUTHOR";	//special case, /
		if(s.equals("SONG_$_ARTIST"))
			return "SONG/ARTIST";	//special case, /, didn't notice this until too late
		if(s.equals("STAR_$_ROLE"))
			return "STAR/ROLE";	//special case, /, didn't notice this until too late
		if(s.equals("FICTIONAL_CHARACTER"))
			s = "CHARACTER";	//as of mid-S30, go down to usePlural below if needed
		if(s.equals("ROCK_ON$"))
			return "ROCK ON!";	//special case
		if(usePlural) {
			if(s.equals("PERSON"))
				return "PEOPLE";	//special case
			else
				s += 'S';
		}
		
		return s.replace('_', ' ').replace('$','&'); 
	}
	
	public String toString() {
		return useBoth ? (singleName == pluralName ? singleName : singleName + (this == PERSON ? "/PEOPLE" : "(S)" )) : (allowPlural ? pluralName : singleName);
		//note above how singleName == pluralName is OK due to how constructor assigns data. This avoids needing String's linear time equals method.
	}
	
	//static within enum is as valid as static within any class. pretty deft
	static {
		for(Category c : Category.values()) {
			LOOKUP.put(c.singleName, c);
			if(c.singleName != c.pluralName)
				PLURAL_LOOKUP.put(c.pluralName, c);	//HashMap will quickly ignore potential duplicate anyways, check isn't too necessary
		}
		
		LOOKUP.put("FICTIONAL CHARACTER", FICTIONAL_CHARACTER);
		PLURAL_LOOKUP.put("FICTIONAL CHARACTERS", FICTIONAL_CHARACTER);
		//adding these anyways
		LOOKUP.put("STAR & ROLE", STAR_$_ROLE);
		LOOKUP.put("SONG & ARTIST", SONG_$_ARTIST);
		LOOKUP.put("SONG / ARTIST", SONG_$_ARTIST);
		LOOKUP.put("AUTHOR/TITLE", TITLE$AUTHOR);
		LOOKUP.put("AUTHOR / TITLE", TITLE$AUTHOR);
		LOOKUP.put("AUTHOR & TITLE", TITLE$AUTHOR);
		LOOKUP.put("TITLE / AUTHOR", TITLE$AUTHOR);
	}
}