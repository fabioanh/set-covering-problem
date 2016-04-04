package be.ac.optimization.heuristic;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;

public final class HeuristicSolver {
	private final static Logger LOGGER = Logger.getLogger(HeuristicSolver.class);
	/**
	 * random solution construction
	 */
	private final ConstructiveHeuristic constructiveHeuristic;
	/**
	 * Redundancy elimination
	 */
	private final Boolean re;
	/**
	 * First Improvement or Best Improvement
	 */
	private final ImprovementType improvementType;
	/**
	 * Instance of the set covering problem
	 */
	private final SetCoveringProblem setCoveringProblem;
	/**
	 * Seed used to set up the random generator
	 */
	private final Integer seed;

	private HeuristicSolver(ConstructiveHeuristic constructiveHeuristic, Boolean re,
			ImprovementType improvementType, String instanceFile, Integer seed) {
		this.constructiveHeuristic = constructiveHeuristic;
		this.re = re;
		this.improvementType = improvementType;
		this.setCoveringProblem = new SetCoveringProblem(instanceFile);
		this.seed = seed;
		RandomUtils.getInstance(seed);
	}

	/**
	 * Method that runs the algorithm to solve the set covering problem
	 */
	public void execute() {
		switch (constructiveHeuristic) {
		case CH1:
			ch1Solution();
			break;
		case CH2:
			ch2Solution();
			break;
		case CH3:
			ch3Solution();
			break;
		case CH4:
			ch4Solution();
			break;
		default:
			break;

		}

		/**
		 * Execute final redundancy elimination if set in parameters
		 */
		if (re) {
			setCoveringProblem.redundancyElimination();
		}

		LOGGER.info("Total cost: " + setCoveringProblem.getCoveredSetsCost());
		LOGGER.debug("Sets covered: " + setCoveringProblem.printableCoveredSets());
	}

	/**
	 * Solution for the constructive heurstic 1. Random selection of elements
	 * and sets
	 */
	@SuppressWarnings("unchecked")
	private void ch1Solution() {
		Integer randomElement;
		Integer selectedSet;
		List<Integer> availableSets;
		while (!terminate()) {
			// Choose a random element from the uncovered ones
			randomElement = Utils.getRandomElement(setCoveringProblem.getUncoveredElements(),
					RandomUtils.getInstance(seed)
							.getRandomInt(setCoveringProblem.getUncoveredElements().size()));

			availableSets = ListUtils.subtract(
					setCoveringProblem.getElementSetMap().get(randomElement),
					new ArrayList<>(setCoveringProblem.getCoveredSets()));
			if (!availableSets.isEmpty()) {
				// Choose a random set from the uncovered sets
				selectedSet = availableSets
						.get(RandomUtils.getInstance(seed).getRandomInt(availableSets.size()));
				setCoveringProblem.coverSet(selectedSet);
			}
		}
	}

	/**
	 * Greedy heuristic implementation using the weight of a subset as greedy
	 * value
	 */
	private void ch2Solution() {
		Integer minCost;
		while (!terminate()) {
			minCost = setCoveringProblem.getMinUncoveredSetCost();
			setCoveringProblem.coverSet(setCoveringProblem.getBestUncoveredSetForCost(minCost));
		}
	}

	/**
	 * Greedy heuristic implementation using the weight of a subset divided by
	 * its number of elements as greedy value
	 */
	private void ch3Solution() {
		Double minCost;
		while (!terminate()) {
			minCost = setCoveringProblem.getMinUncoveredSetCostElemsRatio();
			setCoveringProblem
					.coverSet(setCoveringProblem.getBestUncoveredSetForCostElemsRatio(minCost));
		}
	}

	/**
	 * Greedy heuristic implementation using the weight of a subset divided by
	 * the new elements that would be added to the solution as greedy value
	 */
	private void ch4Solution() {
		Double minCost;
		while (!terminate()) {
			minCost = setCoveringProblem.getMinUncoveredSetCostAdditionalElemsRatio();
			setCoveringProblem.coverSet(
					setCoveringProblem.getBestUncoveredSetForCostAdditionalElemsRatio(minCost));
		}
	}

	/**
	 * Sets the termination condition for the algorithm
	 * 
	 * @return
	 */
	private boolean terminate() {
		return this.setCoveringProblem.getUncoveredElements().isEmpty();
	}

	/**
	 * Builder class used to create the instance of the Heuristic Solver
	 * 
	 * @author fakefla
	 *
	 */
	public static class HeuristicSolverBuilder {
		private ConstructiveHeuristic constructiveHeuristic;
		private Boolean re;
		private ImprovementType improvementType;
		private String instanceFile;
		private Integer seed;

		public HeuristicSolverBuilder constructiveHeuristic(
				ConstructiveHeuristic constructiveHeuristic) {
			this.constructiveHeuristic = constructiveHeuristic;
			return this;
		}

		public HeuristicSolverBuilder re(Boolean re) {
			this.re = re;
			return this;
		}

		public HeuristicSolverBuilder improvementType(ImprovementType improvementType) {
			this.improvementType = improvementType;
			return this;
		}

		public HeuristicSolverBuilder instanceFile(String instanceFile) {
			this.instanceFile = instanceFile;
			return this;
		}

		public HeuristicSolverBuilder seed(Integer seed) {
			this.seed = seed;
			return this;
		}

		public HeuristicSolver build() {
			return new HeuristicSolver(constructiveHeuristic, re, improvementType, instanceFile,
					seed);
		}
	}

}
