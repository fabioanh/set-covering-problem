package be.ac.optimization.heuristic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.log4j.Logger;

public class Utils {

	private static final String OUTPUT_DIRECTORY = "analysis/output/";
	private final static Logger LOGGER = Logger.getLogger(Utils.class);

	public static <T> String printableCollection(Collection<T> collection) {
		StringBuilder sb = new StringBuilder();
		for (T cs : collection) {
			sb.append(cs).append(' ');
		}
		return sb.toString();
	}

	public static void logRuntimeDistributionValue(Logger logger, Long startTime,
			Integer loopCounter, Integer cost) {
		Long elapsedTime = System.currentTimeMillis() - startTime;
		logger.warn(elapsedTime + ";" + cost);
	}

	public static String runtimeDistributionTextValue(Long startTime, Integer loopCounter,
			Integer cost) {
		Long elapsedTime = System.currentTimeMillis() - startTime;
		return elapsedTime + ";" + cost + "\n";
	}

	/**
	 * Method used to output the values intended to be used for the quality
	 * runtime distribution
	 * 
	 * @param qrtdBuffer
	 * @param filename
	 */
	public static void outputQRTD(StringBuilder qrtdBuffer, String filename) {
		File outputFile = new File(filename);
		FileWriter fooWriter;
		try {
			LOGGER.info(outputFile.getAbsolutePath());
			fooWriter = new FileWriter(outputFile, false);
			fooWriter.write(qrtdBuffer.toString());
			fooWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error(e);
		}
	}

	public static String getQRTDOutputFileName(Long startTime, String instanceFile) {
		return OUTPUT_DIRECTORY + startTime + Paths.get(instanceFile).getFileName().toString();
	}
}
