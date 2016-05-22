package be.ac.optimization.heuristic;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.log4j.Logger;

import be.ac.optimization.heuristic.ACOHelper.ACOHelperBuilder;

public final class HeuristicSolver {
	private final static Logger LOGGER = Logger.getLogger(HeuristicSolver.class);
	private static final Integer NOT_IMPROVEMENT_THRESHOLD = 50;
	private static final Double LOWER_THRESHOLD_METROPOLIS_ACCEPTANCE = 0.03;
	// private static final Double HIGHER_THRESHOLD_METROPOLIS_ACCEPTANCE = 0.5;

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
	 * Stochastic Local Search Algorithm
	 */
	private final StochasticLocalSearch stochasticLocalSearch;
	/**
	 * Temperature value for the Simulated Annealing local search
	 */
	private Double temperature;
	/**
	 * Cooling parameter for the Simulated Annealing local search
	 */
	private final Double cooling;
	/**
	 * Beta parameter for the Ant Colony Algorithm
	 */
	private final Double beta;
	/**
	 * Epsilon parameter for the Ant Colony Algorithm
	 */
	private final Double epsilon;
	/**
	 * Rho parameter for the Ant Colony Algorithm
	 */
	private final Double rho;
	/**
	 * Number of ants for the Ant Colony Algorithm
	 */
	private final Integer numberOfAnts;

	private Long duration;
	private final Integer maxLoops;

	private HeuristicSolver(ConstructiveHeuristic constructiveHeuristic, Boolean re,
			ImprovementType improvementType, String instanceFile, Integer seed,
			StochasticLocalSearch stochasticLocalSearch, Double temperature, Double cooling,
			Double beta, Double epsilon, Double rho, Integer numberOfAnts, Long duration,
			Integer maxLoops) {
		this.constructiveHeuristic = constructiveHeuristic;
		this.re = re;
		this.improvementType = improvementType;
		this.setCoveringProblem = new SetCoveringProblem(instanceFile);
		this.stochasticLocalSearch = stochasticLocalSearch;
		this.temperature = temperature;
		this.cooling = cooling;
		this.beta = beta;
		this.epsilon = epsilon;
		this.rho = rho;
		this.numberOfAnts = numberOfAnts;
		this.duration = duration;
		this.maxLoops = maxLoops;
		RandomUtils.getInstance(seed);
	}

	/**
	 * Method that runs the algorithm to solve the set covering problem
	 */
	public void execute() {

		Integer costBeforeRE = null;
		Integer costAfterRE = null;
		Integer costBeforeImprovement = null;

		if (stochasticLocalSearch == null || (stochasticLocalSearch != null
				&& !stochasticLocalSearch.equals(StochasticLocalSearch.ACO))) {
			switch (constructiveHeuristic) {
			case CH1:
				setCoveringProblem.ch1Solution();
				break;
			case CH2:
				setCoveringProblem.ch2Solution();
				break;
			case CH3:
				setCoveringProblem.ch3Solution();
				break;
			case CH4:
				setCoveringProblem.ch4Solution();
				break;
			}

			costBeforeRE = setCoveringProblem.getCoveredSetsCost();
			/**
			 * Execute final redundancy elimination if set in parameters
			 */
			if (re) {
				setCoveringProblem.redundancyElimination();
			}
			costAfterRE = setCoveringProblem.getCoveredSetsCost();

			costBeforeImprovement = setCoveringProblem.getCoveredSetsCost();

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
		}

		if (stochasticLocalSearch != null) {
			switch (stochasticLocalSearch) {
			case ACO:
				ACOHelperBuilder helperBuilder = new ACOHelperBuilder();
				helperBuilder.beta(beta).epsilon(epsilon).rho(rho).numberOfAnts(numberOfAnts)
						.setCoveringProblem(setCoveringProblem).maxLoops(maxLoops);
				if (duration == null) {
					duration = computeDuration();
				}
				helperBuilder.duration(duration);
				this.setCoveringProblem = helperBuilder.build().execute();
				break;
			case SA:
				this.simulatedAnnealing();
				break;
			}
		}

		Integer costAfterImprovement = setCoveringProblem.getCoveredSetsCost();
		LOGGER.info("Total cost: " + costAfterImprovement);
		if (improvementType != null && stochasticLocalSearch == null) {
			LOGGER.info("Improvement profit: " + (costBeforeImprovement > costAfterImprovement));
			LOGGER.info(
					"Improvement profit value: " + (costBeforeImprovement - costAfterImprovement));
		}
		if (re && improvementType == null && stochasticLocalSearch == null) {
			LOGGER.info("RedEl profit: " + (costBeforeRE > costAfterRE));
			LOGGER.info("RedEl profit value: " + (costBeforeRE - costAfterRE));
		}

		LOGGER.debug(
				"Sets covered: " + Utils.printableCollection(setCoveringProblem.getCoveredSets()));
	}

	/**
	 * compute the desired duration for the Ant Colony solution. Time taken
	 * running a solution and a first improvement algorithm multiplied by 100
	 * 
	 * @return
	 */
	private Long computeDuration() {
		long initialTime = System.currentTimeMillis();
		setCoveringProblem.ch4Solution();
		iterativeFirstImprovement();
		return (System.currentTimeMillis() - initialTime) * 100;
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

	/**
	 * Method to perform the iterative best improvement method.
	 */
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
	 * Implementation of the Simulated Annealing Stochastic Local Search.
	 * Uncovering a random set the algorithm uses the metropolis condition and
	 * the given temperature parameter to define whether a proposed solution
	 * (taken from the current solution neighbourhood) is accepted or not.
	 */
	private void simulatedAnnealing() {
		SetCoveringProblem coverProblemSA = SerializationUtils.clone(setCoveringProblem);
		SetCoveringProblem neighbourProblem = SerializationUtils.clone(setCoveringProblem);
		Integer notImprovementCounter = 0;
		Double currentMetropolisAcceptance = 1.0;
		Double initTemp = temperature;
		Integer loopCounter = 0;
		Integer previousCost;
		Integer currentCost;
		Long startTime = System.currentTimeMillis();
		StringBuilder qrtdBuffer = new StringBuilder();
		String outputFileName = Utils.getQRTDOutputFileName(startTime,
				setCoveringProblem.getInstanceFile());

		while (!terminateSimulatedAnnealing(notImprovementCounter, currentMetropolisAcceptance,
				temperature, initTemp)) {
			previousCost = coverProblemSA.getCoveredSetsCost();
			neighbourProblem = generateNeighbourSA(coverProblemSA, neighbourProblem);
			currentMetropolisAcceptance = SimulatedAnnealingHelper.pAccept(temperature,
					coverProblemSA, neighbourProblem);
			LOGGER.trace("f(s)=" + coverProblemSA.getCoveredSetsCost());
			LOGGER.trace("f'(s)=" + neighbourProblem.getCoveredSetsCost());
			coverProblemSA = SimulatedAnnealingHelper.acceptedSCP(temperature,
					currentMetropolisAcceptance, coverProblemSA, neighbourProblem);
			currentCost = coverProblemSA.getCoveredSetsCost();
			neighbourProblem = SerializationUtils.clone(coverProblemSA);
			notImprovementCounter = currentCost.equals(previousCost) ? notImprovementCounter + 1
					: 0;
			temperature = cool(temperature, initTemp, loopCounter);
			loopCounter++;
			LOGGER.trace("Temperature: " + temperature);
			LOGGER.trace("Metropolis Acceptance: " + currentMetropolisAcceptance);
			qrtdBuffer.append(Utils.runtimeDistributionTextValue(startTime, loopCounter,
					coverProblemSA.getCoveredSetsCost()));
		}
		Utils.outputQRTD(qrtdBuffer, outputFileName);
		this.setCoveringProblem = coverProblemSA;
	}

	/**
	 * Creates a neighbour for the simulated annealing algorithm removing a
	 * random covered set and filling its gap by randomly choosing one of the
	 * constructive heuristics defined for the set covering problem
	 * 
	 * @param coverProblemSA
	 * @param neighbourProblem
	 * @return
	 */
	private SetCoveringProblem generateNeighbourSA(SetCoveringProblem coverProblemSA,
			SetCoveringProblem neighbourProblem) {
		neighbourProblem.uncoverSet(
				RandomUtils.getInstance(null).getRandomFromSet(coverProblemSA.getCoveredSets()));
		switch (RandomUtils.getInstance(null).getRandomInt(3)) {
		case 3:
			neighbourProblem.ch1Solution();
			break;
		case 0:
			neighbourProblem.ch2Solution();
			break;
		case 1:
			neighbourProblem.ch3Solution();
			break;
		case 2:
			neighbourProblem.ch4Solution();
			break;
		}
		neighbourProblem.redundancyElimination();
		return neighbourProblem;
	}

	/**
	 * Cooling function used in the Simulated Annealing solution
	 * 
	 * @param temperature
	 * @param initTemp
	 * @param loopCounter
	 * @return
	 */
	private Double cool(Double temperature, Double initTemp, Integer loopCounter) {
		return initTemp - cooling * loopCounter;
		// return temperature * cooling;
	}

	/**
	 * Based on the current temperature and the amount of improvement failures
	 * for the last runs decides whether terminate the simulated annealing
	 * process or keep trying.
	 * 
	 * @param notImprovementCounter
	 * @return
	 */
	private boolean terminateSimulatedAnnealing(Integer notImprovementCounter,
			Double currentMetropolisAcceptance, Double temperature, Double initialTemperature) {
		if (temperature <= 0) {
			return true;
		}

		if (notImprovementCounter > NOT_IMPROVEMENT_THRESHOLD
				&& temperature <= initialTemperature * 0.003) {
			return true;
		}

		return currentMetropolisAcceptance < LOWER_THRESHOLD_METROPOLIS_ACCEPTANCE
				&& temperature <= initialTemperature * 0.003;
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
		private StochasticLocalSearch stochasticLocalSearch;
		private Double temperature;
		private Double cooling;
		private Double beta;
		private Double epsilon;
		private Double rho;
		private Integer numberOfAnts;
		private Integer maxLoops;
		private Long duration;

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

		public HeuristicSolverBuilder stochasticLocalSearch(
				StochasticLocalSearch stochasticLocalSearch) {
			this.stochasticLocalSearch = stochasticLocalSearch;
			return this;
		}

		public HeuristicSolverBuilder temperature(Double temperature) {
			this.temperature = temperature;
			return this;
		}

		public HeuristicSolverBuilder cooling(Double cooling) {
			this.cooling = cooling;
			return this;
		}

		public HeuristicSolverBuilder beta(Double beta) {
			this.beta = beta;
			return this;
		}

		public HeuristicSolverBuilder epsilon(Double epsilon) {
			this.epsilon = epsilon;
			return this;
		}

		public HeuristicSolverBuilder rho(Double rho) {
			this.rho = rho;
			return this;
		}

		public HeuristicSolverBuilder numberOfAnts(Integer nAnts) {
			this.numberOfAnts = nAnts;
			return this;
		}

		public HeuristicSolverBuilder duration(Long duration) {
			this.duration = duration;
			return this;
		}

		public HeuristicSolverBuilder maxLoops(Integer maxLoops) {
			this.maxLoops = maxLoops;
			return this;
		}

		public HeuristicSolver build() {
			return new HeuristicSolver(constructiveHeuristic, re, improvementType, instanceFile,
					seed, stochasticLocalSearch, temperature, cooling, beta, epsilon, rho,
					numberOfAnts, duration, maxLoops);
		}
	}

}
