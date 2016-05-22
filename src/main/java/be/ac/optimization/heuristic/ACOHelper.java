package be.ac.optimization.heuristic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class ACOHelper {
	private final static Logger LOGGER = Logger.getLogger(ACOHelper.class);
	private static final Double HUGE_VALUE = 100000.0;
	private HashMap<Integer, Double> pheromone;
	private ArrayList<Ant> ants;
	private SetCoveringProblem problem;
	private Double minPheromone;
	private Double maxPheromone;
	private Double rho;
	private Double epsilon;
	private Ant bestAnt;
	private Integer numberOfAnts;
	private Long maxTime;
	private Integer maxLoops;

	/**
	 * Initial set up for the ant colony algorithm execution
	 * 
	 * @param numberOfAnts
	 * @param scp
	 * @param beta
	 * @param rho
	 * @param epsilon
	 * @param duration
	 */
	private ACOHelper(Integer numberOfAnts, SetCoveringProblem scp, Double beta, Double rho,
			Double epsilon, Long duration, Integer maxLoops) {
		problem = scp;
		problem.uncoverAllSets();
		this.numberOfAnts = numberOfAnts;
		HashMap<Integer, Double> heuristicInformation = initHeuristicInformation();
		ants = new ArrayList<>();
		initPheromone();
		for (int i = 0; i < this.numberOfAnts; i++) {
			ants.add(new Ant(scp, pheromone, heuristicInformation, beta));
		}
		this.rho = rho;
		this.epsilon = epsilon;
		this.maxTime = System.currentTimeMillis() + duration;
		this.maxLoops = maxLoops;
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(this.maxTime);
		LOGGER.info("expected finish time: " + c.getTime());
	}

	/**
	 * Method to initialize pheromone with an arbitrary big number. In this case
	 * the maximum Double number is used as value.
	 */
	private void initPheromone() {
		pheromone = new HashMap<>();
		for (Integer set : problem.getUncoveredSets()) {
			pheromone.put(set, HUGE_VALUE);
		}
		this.maxPheromone = HUGE_VALUE;
	}

	/**
	 * Initializes the Heuristic Information
	 * 
	 * @return
	 */
	private HashMap<Integer, Double> initHeuristicInformation() {
		HashMap<Integer, Double> heuristicInformation = new HashMap<>();
		for (Integer s : problem.getUncoveredSets()) {
			heuristicInformation.put(s,
					Double.valueOf(problem.getSetElementMap().get(s).getElems().size())
							/ Double.valueOf(problem.getSetElementMap().get(s).getCost()));
		}
		return heuristicInformation;
	}

	/**
	 * Function under control of the general execution of the ACO algorithm
	 * 
	 * @return
	 */
	public SetCoveringProblem execute() {
		Ant currentBestAnt = null;
		// Ant currentBestAnt = ants.get(0);
		Boolean firstLoop = true;
		Integer loopCounter = 0;
		Long startTime = System.currentTimeMillis();
		StringBuilder qrtdBuffer = new StringBuilder();
		String outputFileName = Utils.getQRTDOutputFileName(startTime, problem.getInstanceFile());
		while (!terminate(loopCounter)) {
			currentBestAnt = bestAnt;
			for (Ant ant : ants) {
				ant.solve();
				ant.getProblem().redundancyElimination();
				if (currentBestAnt == null) {
					currentBestAnt = ant;
				}
				if (bestAnt == null || (bestAnt != null && bestAnt.getCost() > ant.getCost())) {
					bestAnt = new Ant(ant);
				}
			}
			updatePheromone(currentBestAnt, firstLoop);
			firstLoop = false;
			if (loopCounter % 20 == 0) {
				LOGGER.debug("Iteration: " + loopCounter);
				LOGGER.debug("Current best ant's cost: " + bestAnt.getCost());
			}
			loopCounter++;
			qrtdBuffer.append(
					Utils.runtimeDistributionTextValue(startTime, loopCounter, bestAnt.getCost()));
		}
		Utils.outputQRTD(qrtdBuffer, outputFileName);
		return bestAnt.getProblem();
	}

	private boolean terminate(Integer loop) {
		return System.currentTimeMillis() > this.maxTime || loop >= maxLoops;
	}

	/**
	 * Method Used to update the values of the pheromone used by the ants. This
	 * is invoked on each iteration done over the whole colony in order to
	 * evaporate pheromone and update the importance of the values for the best
	 * solution contained by the best ant.
	 * 
	 * @param currentBestAnt
	 * @param firstLoop
	 */
	public void updatePheromone(Ant currentBestAnt, Boolean firstLoop) {
		evaporatePheromone();
		Integer totalCostBestAnt = 0;
		for (Integer set : bestAnt.getProblem().getCoveredSets()) {
			totalCostBestAnt += bestAnt.getProblem().getSetElementMap().get(set).getCost();
		}
		Double delta = 1.0 / totalCostBestAnt;

		// update max and min values for pheromone when a new best solution set
		// is found
		if (!currentBestAnt.getProblem().getCoveredSets()
				.equals(this.bestAnt.getProblem().getCoveredSets()) || firstLoop) {
			maxPheromone = 1 / ((1 - rho) * totalCostBestAnt);
			minPheromone = epsilon * maxPheromone;
		}

		Double pheromoneValue;
		for (Integer set : bestAnt.getProblem().getCoveredSets()) {
			pheromoneValue = rho * pheromone.get(set) + delta;
			if (pheromoneValue < maxPheromone && pheromoneValue > minPheromone) {
				pheromone.put(set, pheromoneValue);
			} else if (pheromoneValue > maxPheromone) {
				pheromone.put(set, maxPheromone);
			} else if (pheromoneValue < minPheromone) {
				pheromone.put(set, minPheromone);
			}
		}
	}

	/**
	 * Method in charge of the pheromone evaporation. Uses rho as the main
	 * parameter to update the pheromone.
	 */
	private void evaporatePheromone() {
		if (minPheromone == null) {
			minPheromone = 0.0;
		}
		for (Integer set : problem.getSetElementMap().keySet()) {
			if (pheromone.get(set) * rho < minPheromone) {
				pheromone.put(set, minPheromone);
			} else {
				pheromone.put(set, pheromone.get(set) * rho);
			}
		}
	}

	/**
	 * Builder class for the Ant Colony Helper
	 * 
	 * @author fakefla
	 *
	 */
	public static class ACOHelperBuilder {
		private SetCoveringProblem problem;
		private Integer numberOfAnts;
		private Double beta;
		private Double rho;
		private Double epsilon;
		private Long duration;
		private Integer maxLoops;

		public ACOHelperBuilder setCoveringProblem(SetCoveringProblem scp) {
			problem = scp;
			return this;
		}

		public ACOHelperBuilder numberOfAnts(Integer numberOfAnts) {
			this.numberOfAnts = numberOfAnts;
			return this;
		}

		public ACOHelperBuilder beta(Double beta) {
			this.beta = beta;
			return this;
		}

		public ACOHelperBuilder rho(Double rho) {
			this.rho = rho;
			return this;
		}

		public ACOHelperBuilder epsilon(Double epsilon) {
			this.epsilon = epsilon;
			return this;
		}

		public ACOHelperBuilder duration(Long duration) {
			this.duration = duration;
			return this;
		}

		public ACOHelperBuilder maxLoops(Integer maxLoops) {
			this.maxLoops = maxLoops;
			return this;
		}

		public ACOHelper build() {
			return new ACOHelper(numberOfAnts, problem, beta, rho, epsilon, duration, maxLoops);
		}
	}
}
