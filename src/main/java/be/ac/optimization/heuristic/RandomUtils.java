package be.ac.optimization.heuristic;

import java.util.Random;
import java.util.Set;

public class RandomUtils {

	private static RandomUtils instance = null;
	private final Random random;

	private RandomUtils(Integer seed) {
		random = new Random(seed);
	}

	public static RandomUtils getInstance(Integer seed) {
		if (instance == null && seed != null) {
			instance = new RandomUtils(seed);
		}
		return instance;
	}

	/**
	 * Returns an unbounded random integer
	 * 
	 * @return
	 */
	public Integer getRandomInt() {
		return random.nextInt();
	}

	/**
	 * Returns a bounded random integer
	 * 
	 * @param upperBound
	 * @return
	 */
	public Integer getRandomInt(Integer upperBound) {
		Integer aux = random.nextInt(upperBound);
		return aux;
	}

	public Random getRandom() {
		return random;
	}

	public <T> T getRandomFromSet(Set<T> set) {
		int rndPos = random.nextInt(set.size());
		int i = 0;
		for (T obj : set) {
			if (i == rndPos) {
				return obj;
			}
			i++;
		}
		return null;
	}

}
