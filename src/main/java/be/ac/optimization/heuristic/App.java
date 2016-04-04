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

	/**
	 * Main function called by to execute the application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		HeuristicSolver scpSolver = readArguments(args);
		scpSolver.execute();
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
			if (cmd.getOptionValue(IMPROVEMENT) != null) {
				builder = builder.improvementType(
						(ImprovementType.valueOf(cmd.getOptionValue(IMPROVEMENT).toUpperCase())));
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
				.seed(1);
		return builder;
	}
}
