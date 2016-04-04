package be.ac.optimization.heuristic;

import java.util.HashSet;
import java.util.Iterator;

public class Utils {

	public static <T> T getRandomElement(HashSet<T> set, Integer position) {
		int k = 0;
		T element = null;
		for (Iterator<T> ue = set.iterator(); ue.hasNext(); k++) {
			element = ue.next();
			if (k == position) {
				break;
			}
		}
		return element;
	}
}
