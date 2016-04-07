package be.ac.optimization.heuristic;

import java.util.Collection;

public class Utils {

	public static <T> String printableCollection(Collection<T> collection) {
		StringBuilder sb = new StringBuilder();
		for (T cs : collection) {
			sb.append(cs).append(' ');
		}
		return sb.toString();
	}
}
