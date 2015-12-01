package woftracker.record;

import woftracker.*;
import java.util.*;

public enum Letter {
	A, B, C, D, E, F, G, H, I, J, K, L, M,	// 0-12
	N, O, P, Q, R, S, T, U, V, W, X, Y, Z;	// 13-25
	
	public static final Set<Letter> VOWELS = Collections.unmodifiableSet(EnumSet.of(A, E, I, O, U)),
		CONSONANTS = Collections.unmodifiableSet(EnumSet.of(B, C, D, F, G, H, J, K, L, M, N, P, Q, R, S, T, V, W, X, Y, Z));
	
	public static Letter getLetter(char c) {
		try {
			return Letter.values()[c - 'A'];
		} catch(IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public static Letter getLetter(String s) {
		return s.length() == 1 ? getLetter(s.charAt(0)) : null;
	}
	
	public char getChar() {
		return this.name().charAt(0);
	}
}