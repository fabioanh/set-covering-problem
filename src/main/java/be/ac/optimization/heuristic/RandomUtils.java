package be.ac.optimization.heuristic;

import java.util.Random;

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

}
