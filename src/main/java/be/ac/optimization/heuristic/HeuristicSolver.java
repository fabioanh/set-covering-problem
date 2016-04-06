package be.ac.optimization.heuristic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.SerializationUtils;
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

		if (improvementType != null) {
			switch (improvementType) {
			case BI:
				this.iterativeBestImprovement();
				break;
			case FI:
				this.iterativeFirstImprovement();
				break;
			}
		}

		LOGGER.info("Total cost: " + setCoveringProblem.getCoveredSetsCost());
		LOGGER.info("Sets covered: " + setCoveringProblem.printableCoveredSets());
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
	 * Implementation for the iterative first improvement method. After removing
	 * a random set, takes the current uncovered sets and try to put them in the
	 * solution starting from the less expensive one. If all the uncovered sets
	 * are tried the process is finished
	 */
	private void iterativeFirstImprovement() {
		SetCoveringProblem coverProblemFI = SerializationUtils.clone(setCoveringProblem);
		List<Integer> orderedUncoveredSets = setCoveringProblem.getOrderedUncoveredSets();
		Iterator<Integer> uncovSetIter;
		Integer currentCost = setCoveringProblem.getCoveredSetsCost();
		HashSet<Integer> currentCoveredSets;

		Boolean improvement = true;
		while (improvement) {
			improvement = false;
			uncovSetIter = orderedUncoveredSets.iterator();
			currentCoveredSets = new HashSet<>(coverProblemFI.getCoveredSets());
			coverProblemFI.uncoverSet(RandomUtils.getInstance(null)
					.getRandomInt(coverProblemFI.getCoveredSets().size()).intValue());
			while (uncovSetIter.hasNext()) {
				coverProblemFI.coverSet(uncovSetIter.next());
				coverProblemFI.redundancyElimination();
				if (coverProblemFI.getUncoveredElements().isEmpty()
						&& currentCost > coverProblemFI.getCoveredSetsCost()) {
					improvement = true;
					uncovSetIter.remove();
					currentCost = coverProblemFI.getCoveredSetsCost();
					break;
				}
				coverProblemFI.restoreCoveredSets(currentCoveredSets);
			}
		}

		LOGGER.info("First Improvement Final Cost: " + coverProblemFI.getCoveredSetsCost());
		LOGGER.info("Sets covered: " + coverProblemFI.printableCoveredSets());
	}

	private void iterativeBestImprovement() {
		SetCoveringProblem coverProblemFI = SerializationUtils.clone(setCoveringProblem);
		Iterator<Integer> uncovSetIter;
		Integer uncovSet;
		Integer currentCost = setCoveringProblem.getCoveredSetsCost();
		HashSet<Integer> currentCoveredSets;
		HashSet<Integer> initialCoveredSets = new HashSet<>(coverProblemFI.getCoveredSets());

		Boolean improvement = true;
		while (improvement) {
			improvement = false;
			for (Integer cs : initialCoveredSets) {
				uncovSetIter = coverProblemFI.getOrderedUncoveredSets().iterator();
				currentCoveredSets = new HashSet<>(coverProblemFI.getCoveredSets());

				if (coverProblemFI.uncoverSet(cs)) {
					while (uncovSetIter.hasNext()) {
						uncovSet = uncovSetIter.next();
						coverProblemFI.coverSet(uncovSet);
						coverProblemFI.redundancyElimination();
						if (coverProblemFI.getUncoveredElements().isEmpty()
								&& currentCost > coverProblemFI.getCoveredSetsCost()) {
							improvement = true;
							uncovSetIter.remove();
							break;
						}
						coverProblemFI.restoreCoveredSets(currentCoveredSets);
					}
				}
			}
		}

		LOGGER.info("First Improvement Final Cost: " + coverProblemFI.getCoveredSetsCost());
		LOGGER.info("Sets covered: " + coverProblemFI.printableCoveredSets());
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
