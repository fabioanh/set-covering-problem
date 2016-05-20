package be.ac.optimization.heuristic;

public class SimulatedAnnealingHelper {

	/**
	 * Function implementing the metropolis condition to determine the
	 * probability of acceptance of a neighbour-candidate solution
	 * 
	 * @param temperature
	 * @param initialSCP
	 * @param neighbourSCP
	 * @return
	 */
	public static Double pAccept(Double temperature, SetCoveringProblem initialSCP,
			SetCoveringProblem neighbourSCP) {
		if (neighbourSCP.getCoveredSetsCost() <= initialSCP.getCoveredSetsCost()) {
			return 1.0;
		}
		return Math.exp((initialSCP.getCoveredSetsCost() - neighbourSCP.getCoveredSetsCost())
				/ temperature);
	}

	/**
	 * Depending on the temperature and the metropolis acceptance condition
	 * returns the accepted set covering problem
	 * 
	 * @param temperature
	 * @param current
	 * @param neighbour
	 * @return
	 */
	public static SetCoveringProblem acceptedSCP(Double temperature, SetCoveringProblem current,
			SetCoveringProblem neighbour) {
		return RandomUtils.getInstance(null).getRandomDouble() <= pAccept(temperature, current,
				neighbour) ? neighbour : current;
	}

	public static SetCoveringProblem acceptedSCP(Double temperature, Double pAccept,
			SetCoveringProblem current, SetCoveringProblem neighbour) {
		return RandomUtils.getInstance(null).getRandomDouble() <= pAccept ? neighbour : current;
	}
}
