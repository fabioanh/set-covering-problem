package be.ac.optimization.heuristic;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import be.ac.optimization.heuristic.HeuristicSolver.HeuristicSolverBuilder;

/**
 * Class containing main method used to execute the application
 * 
 * @author Fabio Navarrete
 */
public class App {

	private final static Logger LOGGER = Logger.getLogger(App.class);
	private static final String INSTANCE = "instance";
	private static final String SEED = "seed";
	private static final String CH = "ch";
	private static final String RE = "re";
	private static final String IMPROVEMENT = "improvement";
	private static final String STOCHASTIC_LOCAL_SEARCH = "sls";
	private static final String TEMPERATURE = "temp";
	private static final String COOLING = "cool";
	private static final String BETA = "beta";
	private static final String EPSILON = "epsilon";
	private static final String RHO = "rho";
	private static final String NUMBER_OF_ANTS = "ants";
	private static final String MAX_LOOPS = "loops";
	private static final String DURATION = "duration";

	/**
	 * Main function called by to execute the application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		HeuristicSolver scpSolver = readArguments(args);
		scpSolver.execute();
		long stopTime = System.currentTimeMillis();
		LOGGER.info("Exec Time: " + (stopTime - startTime));
	}

	/**
	 * Using apache commons implementation, reads the parameters given for the
	 * application in the command line
	 * 
	 * @param args
	 * @return
	 */
	private static HeuristicSolver readArguments(String[] args) {
		HeuristicSolverBuilder builder = getDefaultParameters();

		Options options = new Options();

		options.addOption(INSTANCE, true, "Path for the instance file of the SCP");
		options.addOption(SEED, true, "Seed to be used in the random numbers obtention");
		options.addOption(CH, true, "Constructive Heuristic Type");
		options.addOption(RE, false, "Redundancy elimination");
		options.addOption(IMPROVEMENT, true,
				"Type of improvement, FI (first improvement) or BI (best improvement)");
		options.addOption(STOCHASTIC_LOCAL_SEARCH, true,
				"Type of stochastic local search algorithm to use. SA (Simulated Annealing) "
						+ "or ACO (Ant Colony Optimization)");
		options.addOption(TEMPERATURE, true,
				"Initial Temperature value for the Simulated Annealing algorithm");
		options.addOption(COOLING, true, "Cooling parameter for the Simulated Annealing algorithm");
		options.addOption(BETA, true,
				"Beta parameter used in the probabilities of the Ant Colony Solver");
		options.addOption(EPSILON, true,
				"Beta parameter used in the pheromone thresholds of the Ant Colony Solver");
		options.addOption(RHO, true,
				"Rho parameter used in the probabilities of the Ant Colony Solver");
		options.addOption(NUMBER_OF_ANTS, true, "Number of ants for the Ant Colony Solver");
		options.addOption(MAX_LOOPS, true, "Maximum number of loops to execute");
		options.addOption(DURATION, true, "Expected max duration for the execution");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			if (cmd.getOptionValue(CH) != null) {
				builder = builder.constructiveHeuristic(
						(ConstructiveHeuristic.valueOf(cmd.getOptionValue(CH).toUpperCase())));
			}
			if (cmd.hasOption(RE)) {
				builder = builder.re(true);
			}
			if (cmd.getOptionValue(SEED) != null) {
				builder = builder.seed(Integer.valueOf(cmd.getOptionValue(SEED)));
			}
			if (cmd.getOptionValue(NUMBER_OF_ANTS) != null) {
				builder = builder.numberOfAnts(Integer.valueOf(cmd.getOptionValue(NUMBER_OF_ANTS)));
			}
			if (cmd.getOptionValue(MAX_LOOPS) != null) {
				builder = builder.maxLoops(Integer.valueOf(cmd.getOptionValue(MAX_LOOPS)));
			}
			if (cmd.getOptionValue(DURATION) != null) {
				builder = builder.duration(Long.valueOf(cmd.getOptionValue(DURATION)));
			}
			if (cmd.getOptionValue(TEMPERATURE) != null) {
				builder = builder.temperature(Double.valueOf(cmd.getOptionValue(TEMPERATURE)));
			}
			if (cmd.getOptionValue(COOLING) != null) {
				builder = builder.cooling(Double.valueOf(cmd.getOptionValue(COOLING)));
			}
			if (cmd.getOptionValue(BETA) != null) {
				builder = builder.beta(Double.valueOf(cmd.getOptionValue(BETA)));
			}
			if (cmd.getOptionValue(EPSILON) != null) {
				builder = builder.epsilon(Double.valueOf(cmd.getOptionValue(EPSILON)));
			}
			if (cmd.getOptionValue(RHO) != null) {
				builder = builder.rho(Double.valueOf(cmd.getOptionValue(RHO)));
			}
			if (cmd.getOptionValue(IMPROVEMENT) != null) {
				builder = builder.improvementType(
						(ImprovementType.valueOf(cmd.getOptionValue(IMPROVEMENT).toUpperCase())));
			}
			if (cmd.getOptionValue(STOCHASTIC_LOCAL_SEARCH) != null) {
				builder = builder.stochasticLocalSearch((StochasticLocalSearch
						.valueOf(cmd.getOptionValue(STOCHASTIC_LOCAL_SEARCH).toUpperCase())));
			}
			if (cmd.getOptionValue(INSTANCE) != null) {
				builder = builder.instanceFile(cmd.getOptionValue(INSTANCE));
			}

			return builder.build();
		} catch (ParseException e) {
			LOGGER.error(e);
		}
		return null;
	}

	/**
	 * Method to get the default parameters to execute the application in case
	 * none are provided in the command line interface
	 * 
	 * @return
	 */
	private static HeuristicSolverBuilder getDefaultParameters() {
		HeuristicSolverBuilder builder = new HeuristicSolverBuilder();
		builder.constructiveHeuristic(ConstructiveHeuristic.CH1).re(false).improvementType(null)
				.seed(1).stochasticLocalSearch(null).cooling(0.95).temperature(800.0).maxLoops(1000);
		return builder;
	}
}
