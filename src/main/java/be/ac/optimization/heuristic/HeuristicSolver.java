package be.ac.optimization.heuristic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.MutablePair;
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
	private SetCoveringProblem setCoveringProblem;
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
		}

		Integer costBeforeRE = setCoveringProblem.getCoveredSetsCost();
		/**
		 * Execute final redundancy elimination if set in parameters
		 */
		if (re) {
			setCoveringProblem.redundancyElimination();
		}
		Integer costAfterRE = setCoveringProblem.getCoveredSetsCost();

		Integer costBeforeImprovement = setCoveringProblem.getCoveredSetsCost();

		if (improvementType != null) {
			// iterativeImprovement();
			switch (improvementType) {
			case BI:
				this.iterativeBestImprovement();
				break;
			case FI:
				this.iterativeFirstImprovement();
				break;
			}
		}

		Integer costAfterImprovement = setCoveringProblem.getCoveredSetsCost();
		LOGGER.info("Total cost: " + costAfterImprovement);
		if (improvementType != null) {
			LOGGER.info("Improvement profit: " + (costBeforeImprovement > costAfterImprovement));
			LOGGER.info(
					"Improvement profit value: " + (costBeforeImprovement - costAfterImprovement));
		}
		if (re && improvementType == null) {
			LOGGER.info("RedEl profit: " + (costBeforeRE > costAfterRE));
			LOGGER.info("RedEl profit value: " + (costBeforeRE - costAfterRE));
		}

		LOGGER.debug(
				"Sets covered: " + Utils.printableCollection(setCoveringProblem.getCoveredSets()));
	}

	/**
	 * Solution for the constructive heuristic 1. Random selection of elements
	 * and sets
	 */
	@SuppressWarnings("unchecked")
	private void ch1Solution() {
		Integer randomElement;
		Integer selectedSet;
		List<Integer> availableSets;
		while (!terminate()) {
			// Choose a random element from the uncovered ones
			randomElement = RandomUtils.getInstance(null)
					.getRandomFromSet(setCoveringProblem.getUncoveredElements());

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
	 * Implementation for the iterative first improvement method. After removing
	 * a random set, takes the current uncovered sets and try to put them in the
	 * solution starting from the less expensive one. If all the uncovered sets
	 * are tried the process is finished
	 */
	private void iterativeFirstImprovement() {
		SetCoveringProblem coverProblemFI = SerializationUtils.clone(setCoveringProblem);
		Iterator<Integer> uncovSetIter;
		Integer currentCost = coverProblemFI.getCoveredSetsCost();
		HashSet<Integer> iterCurrentCoveredSets;
		HashSet<Integer> currentCoveredSets;

		Boolean improvement = true;
		while (improvement) {
			improvement = false;

			// Set fixed order to check neighborhoods
			uncovSetIter = coverProblemFI.getOrderedUncoveredSets().iterator();

			// Uncover random set from the currently covered sets (remove random
			// column)
			currentCoveredSets = new HashSet<>(coverProblemFI.getCoveredSets());
			coverProblemFI.uncoverSet(RandomUtils.getInstance(null)
					.getRandomFromSet(coverProblemFI.getCoveredSets()));
			iterCurrentCoveredSets = new HashSet<>(coverProblemFI.getCoveredSets());

			while (uncovSetIter.hasNext()) {
				// Attempt to cover the gap by trying to use the uncovered sets
				// starting from the less expensive ones. Definition of possible
				// neighbours
				coverProblemFI.coverSet(uncovSetIter.next());
				coverProblemFI.redundancyElimination();

				// If the new selected set leads to an improvement, set it as
				// the new best cost, breaks the current attempts to find a
				// better option and proceed with another random element
				// elimination.
				if (coverProblemFI.getUncoveredElements().isEmpty()
						&& currentCost > coverProblemFI.getCoveredSetsCost()) {
					improvement = true;
					uncovSetIter.remove();
					currentCost = coverProblemFI.getCoveredSetsCost();
					currentCoveredSets = new HashSet<>(coverProblemFI.getCoveredSets());
					break;
				}
				coverProblemFI.restoreCoveredSets(iterCurrentCoveredSets);
			}

			if (!coverProblemFI.getUncoveredElements().isEmpty()) {
				coverProblemFI.restoreCoveredSets(currentCoveredSets);
			}
		}

		LOGGER.debug("First Improvement Final Cost: " + coverProblemFI.getCoveredSetsCost());
		LOGGER.debug("Sets covered: " + Utils.printableCollection(coverProblemFI.getCoveredSets()));
		LOGGER.debug("Uncovered elements: "
				+ Utils.printableCollection(coverProblemFI.getUncoveredElements()));

		this.setCoveringProblem = SerializationUtils.clone(coverProblemFI);

	}

	private void iterativeBestImprovement() {
		SetCoveringProblem coverProblemBI = SerializationUtils.clone(setCoveringProblem);
		Iterator<Integer> uncovSetIter;
		HashSet<Integer> iterCurrentCoveredSets;

		MutablePair<Integer, HashSet<Integer>> bestFound = new MutablePair<Integer, HashSet<Integer>>(
				coverProblemBI.getCoveredSetsCost(),
				new HashSet<>(coverProblemBI.getCoveredSets()));

		Boolean improvement = true;
		while (improvement) {
			improvement = false;

			// Set fixed order to check neighborhoods and make sure all of them
			// are covered
			uncovSetIter = coverProblemBI.getOrderedUncoveredSets().iterator();

			// Uncover random set from the currently covered sets (remove random
			// column)
			coverProblemBI.uncoverSet(RandomUtils.getInstance(null)
					.getRandomFromSet(coverProblemBI.getCoveredSets()));
			iterCurrentCoveredSets = new HashSet<>(coverProblemBI.getCoveredSets());

			// Go through the neighbours and check if a better solution than the
			// current one is found
			while (uncovSetIter.hasNext()) {
				coverProblemBI.coverSet(uncovSetIter.next());
				coverProblemBI.redundancyElimination();

				// If a better solution is found, save it as the best found and
				// keep trying the remaining neighbours
				if (coverProblemBI.getUncoveredElements().isEmpty()
						&& bestFound.getLeft() > coverProblemBI.getCoveredSetsCost()) {
					improvement = true;
					uncovSetIter.remove();
					bestFound.setLeft(coverProblemBI.getCoveredSetsCost());
					bestFound.setRight(new HashSet<>(coverProblemBI.getCoveredSets()));
				}
				coverProblemBI.restoreCoveredSets(iterCurrentCoveredSets);
			}
			coverProblemBI.restoreCoveredSets(bestFound.getRight());
		}

		LOGGER.debug("Best Improvement Final Cost: " + coverProblemBI.getCoveredSetsCost());
		LOGGER.debug("Sets covered: " + Utils.printableCollection(coverProblemBI.getCoveredSets()));
		LOGGER.debug("Uncovered elements: "
				+ Utils.printableCollection(coverProblemBI.getUncoveredElements()));

		this.setCoveringProblem = SerializationUtils.clone(coverProblemBI);
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
