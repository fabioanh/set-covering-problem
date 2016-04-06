package be.ac.optimization.heuristic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;

/**
 * Class to represent the set covering problem
 * 
 * @author Fabio Navarrete
 *
 */
public final class SetCoveringProblem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6650501831301162392L;

	private final static Logger LOGGER = Logger.getLogger(SetCoveringProblem.class);

	/**
	 * Number of sets in the problem. Aka: Number of columns
	 * 
	 * @serial
	 */
	private Integer nSets = new Integer(0);
	/**
	 * Number of elements in the problem
	 * 
	 * @serial
	 */
	private Integer nElements = new Integer(0);
	/**
	 * Path to the instance in the file system
	 * 
	 * @serial
	 */
	private final String instanceFile;

	/**
	 * Key with the identifier of the element mapping to a list of the subsets
	 * that contain the element in the key.
	 * 
	 * @serial
	 */
	private final HashMap<Integer, ArrayList<Integer>> elementSetMap;
	/**
	 * Key with the identifier of the set mapping to a list of the elements
	 * contained in the given set and their total cost.
	 * 
	 * @serial
	 */
	private final HashMap<Integer, Subset> setElementMap;
	/**
	 * Contains the number of sets that contain the element in the given index
	 * in the array.
	 * 
	 * @serial
	 */
	private final ArrayList<Integer> countNSets;

	/**
	 * Matrix to trace the coverage according to the rows read from the instance
	 * text file
	 * 
	 * @serial
	 */
	private boolean[][] map;

	/**
	 * List of covered/uncovered elements for the optimization process. Elements
	 * are also known as rows
	 * 
	 * @serial
	 */
	private HashSet<Integer> coveredElements;
	private HashSet<Integer> uncoveredElements;
	/**
	 * List of used/unused sets for the solution. Elements are also known as
	 * columns in the implementation
	 * 
	 * @serial
	 */
	private HashSet<Integer> coveredSets;
	private HashSet<Integer> uncoveredSets;

	/**
	 * List containing the sets ordered by their cost from minimum to maximum
	 * 
	 * @serial
	 */
	// TODO: Check if this field is useful. May not be used.
	private List<Integer> costOrderedSets;

	public SetCoveringProblem(String instanceFile) {
		this.setElementMap = new HashMap<>();
		this.elementSetMap = new HashMap<>();
		this.countNSets = new ArrayList<>();
		this.instanceFile = instanceFile;
		readInstance();
	}

	/**
	 * Reads the instance values using the instance file location
	 */
	private final void readInstance() {
		BufferedReader reader;
		try {
			reader = Files.newBufferedReader(Paths.get(instanceFile), Charset.defaultCharset());
			String line = null;
			String[] splitLine = reader.readLine().trim().split(" ");
			nElements = Integer.valueOf(splitLine[0]);
			nSets = Integer.valueOf(splitLine[1]);
			LOGGER.trace("Number of Elements for the instance: " + nElements);
			LOGGER.trace("Number of Sets for the instance: " + nSets);
			line = readCosts(reader);
			readRows(reader, line);
			countAndLoadElementsPerSet();
			initCoveredLists();
			initCostOrderedSets();
			LOGGER.info("Load of instance done successfully");
		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

	/**
	 * Fills the list costOrderedSets with the values for the sets ordered from
	 * minimum to maximum according to the cost of each of them.
	 */
	private void initCostOrderedSets() {
		costOrderedSets = setElementMap.entrySet().stream().sorted(
				(s1, s2) -> Integer.compare(s1.getValue().getCost(), s2.getValue().getCost()))
				.map(s -> s.getKey()).collect(Collectors.toList());
	}

	private void initCoveredLists() {
		initCoveredElements();
		initCoveredSets();
	}

	private void initCoveredElements() {
		coveredElements = new HashSet<>();
		uncoveredElements = new HashSet<>();
		for (int i = 0; i < nElements; i++) {
			uncoveredElements.add(i);
		}
	}

	private void initCoveredSets() {
		coveredSets = new HashSet<>();
		uncoveredSets = new HashSet<>();
		for (int i = 0; i < nSets; i++) {
			uncoveredSets.add(i);
		}

	}

	/**
	 * Helper method used to populate the elements contained in a set and count
	 * the elements in a set using the information loaded for the sets (coming
	 * from the rows in the input file) and the map (matrix) created to analyze
	 * the problem
	 */
	private void countAndLoadElementsPerSet() {
		ArrayList<Integer> tmpColValues;
		for (int j = 0; j < nSets; j++) {
			tmpColValues = new ArrayList<>();
			for (int i = 0; i < nElements; i++) {
				if (map[i][j]) {
					tmpColValues.add(i);
				}
			}
			setElementMap.get(j).setElems(tmpColValues);
			setElementMap.get(j).setNumElems(tmpColValues.size());
			// countNElements.add(count);
		}
	}

	/**
	 * Helper function used to read the row values from the instance files
	 * 
	 * @param reader
	 * @param line
	 * @throws IOException
	 */
	private void readRows(BufferedReader reader, String line) throws IOException {
		String[] splitLine;
		int i;
		map = new boolean[nElements][nSets];
		int numElements = Integer.valueOf(line.trim()).intValue();
		ArrayList<Integer> tmpRowValues;
		// Read rows groups
		while ((line = reader.readLine()) != null) {
			tmpRowValues = new ArrayList<>();
			i = 0;
			do {
				splitLine = line.trim().split(" ");
				tmpRowValues.addAll(Arrays.asList(splitLine).stream()
						.map(s -> Integer.parseInt(s) - 1).collect(Collectors.toList()));
				i += splitLine.length;
			} while ((line = reader.readLine()) != null && i < numElements);
			for (Integer val : tmpRowValues) {
				map[elementSetMap.size()][val] = true;
			}
			elementSetMap.put(elementSetMap.size(), tmpRowValues);
			countNSets.add(numElements);
			if (line != null) {
				numElements = Integer.valueOf(line.trim()).intValue();
			}
		}
		LOGGER.trace("Elements read successfully");
	}

	/**
	 * Helper function used to read the cost values from the instance file
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	private String readCosts(BufferedReader reader) throws IOException {
		String line;
		String[] splitLine;
		int i = 0;
		// Initializes only the costs for the sets.
		while ((line = reader.readLine()) != null && i < nSets) {
			splitLine = line.trim().split(" ");
			for (String str : splitLine) {
				setElementMap.put(Integer.valueOf(i), new Subset(Integer.valueOf(str)));
				i++;
			}
		}
		LOGGER.trace("costs read successfully");
		return line;
	}

	public Integer getnSets() {
		return nSets;
	}

	public void setnSets(Integer nSets) {
		this.nSets = nSets;
	}

	public Integer getnElements() {
		return nElements;
	}

	public void setnElements(Integer nElements) {
		this.nElements = nElements;
	}

	public boolean[][] getMap() {
		return map;
	}

	public void setMap(boolean[][] map) {
		this.map = map;
	}

	public HashMap<Integer, ArrayList<Integer>> getElementSetMap() {
		return elementSetMap;
	}

	public HashMap<Integer, Subset> getSetElementMap() {
		return setElementMap;
	}

	public ArrayList<Integer> getCountNSets() {
		return countNSets;
	}

	public HashSet<Integer> getCoveredElements() {
		return coveredElements;
	}

	public void setCoveredElements(HashSet<Integer> coveredElements) {
		this.coveredElements = coveredElements;
	}

	public HashSet<Integer> getUncoveredElements() {
		return uncoveredElements;
	}

	public void setUncoveredElements(HashSet<Integer> uncoveredElements) {
		this.uncoveredElements = uncoveredElements;
	}

	public HashSet<Integer> getCoveredSets() {
		return coveredSets;
	}

	public void setCoveredSets(HashSet<Integer> coveredSets) {
		this.coveredSets = coveredSets;
	}

	public HashSet<Integer> getUncoveredSets() {
		return uncoveredSets;
	}

	public void setUncoveredSets(HashSet<Integer> uncoveredSets) {
		this.uncoveredSets = uncoveredSets;
	}

	public List<Integer> getCostOrderedSets() {
		return costOrderedSets;
	}

	public void setCostOrderedSets(List<Integer> costOrderedSets) {
		this.costOrderedSets = costOrderedSets;
	}

	/**
	 * Logic to set an element as covered
	 * 
	 * @param element
	 */
	public void coverElement(Integer element) {
		uncoveredElements.remove(element);
		coveredElements.add(element);
	}

	/**
	 * Logic to establish a set as covered.
	 * 
	 * @param set
	 */
	public void coverSet(Integer set) {
		if (uncoveredSets.remove(set)) {
			coveredSets.add(set);
			for (Integer elem : setElementMap.get(set).getElems()) {
				coverElement(elem);
			}
		}
	}

	public void restoreCoveredSets(HashSet<Integer> coveredSetss) {
		initCoveredSets();
		coveredSets = new HashSet<>(coveredSetss);
		uncoveredSets.removeAll(coveredSets);
		updateCoveredElements();
	}

	public Boolean uncoverSet(Integer set) {
		if (coveredSets.remove(set)) {
			uncoveredSets.add(set);
			updateCoveredElements();
			return true;
		}
		return false;
	}

	private void updateCoveredElements() {
		initCoveredElements();
		for (Integer s : coveredSets) {
			coveredElements.addAll(setElementMap.get(s).getElems());
		}
		uncoveredElements.removeAll(coveredElements);
	}

	/**
	 * Returns the cost of the current covered sets
	 * 
	 * @return
	 */
	public Integer getCoveredSetsCost() {
		Integer totalCost = 0;
		for (Integer cs : coveredSets) {
			totalCost += setElementMap.get(cs).getCost();
		}
		return totalCost;
	}

	/**
	 * Returns the cost of the current covered sets
	 * 
	 * @return
	 */
	public Integer getSetsCost(List<Integer> covereddSets) {
		Integer totalCost = 0;
		for (Integer cs : covereddSets) {
			totalCost += setElementMap.get(cs).getCost();
		}
		return totalCost;
	}

	/**
	 * Returns the minimum cost value for the currently uncovered sets
	 * 
	 * @return
	 */
	public Integer getMinUncoveredSetCost() {
		IntSummaryStatistics summaryStatistics = setElementMap.entrySet().stream()
				.filter(s -> uncoveredSets.contains(s.getKey()))
				.filter(s -> uncoveredElements.stream()
						.anyMatch(p -> s.getValue().getElems().contains(p)))
				.mapToInt(s -> s.getValue().getCost()).summaryStatistics();
		return summaryStatistics.getMin();
	}

	/**
	 * Returns the minimum cost value for the currently uncovered sets
	 * 
	 * @return
	 */
	public Double getMinUncoveredSetCostElemsRatio() {

		DoubleSummaryStatistics summaryStatistics = setElementMap.entrySet().stream()
				.filter(s -> uncoveredSets.contains(s.getKey()))
				.filter(s -> uncoveredElements.stream()
						.anyMatch(p -> s.getValue().getElems().contains(p)))
				.mapToDouble(s -> Double.valueOf(s.getValue().getCost())
						/ Double.valueOf(s.getValue().getNumElems()))
				.summaryStatistics();
		return summaryStatistics.getMin();
	}

	/**
	 * Returns the minimum cost value for the currently uncovered sets
	 * 
	 * @return
	 */
	public Double getMinUncoveredSetCostAdditionalElemsRatio() {

		DoubleSummaryStatistics summaryStatistics = setElementMap.entrySet()
				.stream().filter(
						s -> uncoveredSets.contains(s.getKey()))
				.filter(s -> uncoveredElements.stream()
						.anyMatch(
								p -> s.getValue().getElems().contains(p)))
				.mapToDouble(
						s -> Double
								.valueOf(
										s.getValue().getCost())
								/ Double.valueOf(ListUtils.intersection(s.getValue().getElems(),
										new ArrayList<>(uncoveredElements)).size()))
				.summaryStatistics();
		return summaryStatistics.getMin();
	}

	/**
	 * Returns the best set for a given cost. Best set determined by the number
	 * of elements that a set covers. The more elements it covers the better it
	 * is considered. In case of a tie it is chosen randomly between the tied
	 * ones.
	 * 
	 * @param minCost
	 * 
	 * @return
	 */
	public Integer getBestUncoveredSetForCost(Integer cost) {
		List<Entry<Integer, Subset>> uncoveredFiltered = setElementMap.entrySet().stream()
				.filter(s -> s.getValue().getCost().equals(cost))
				.filter(s -> uncoveredElements.stream()
						.anyMatch(p -> s.getValue().getElems().contains(p)))
				.collect(Collectors.toList());
		return extractMaxNumElems(uncoveredFiltered);
	}

	/**
	 * 
	 * @param costElemsRatio
	 * @return
	 */
	public Integer getBestUncoveredSetForCostElemsRatio(Double costElemsRatio) {
		List<Entry<Integer, Subset>> uncoveredFiltered = setElementMap.entrySet().stream()
				.filter(s -> Double
						.valueOf(Double.valueOf(s.getValue().getCost())
								/ Double.valueOf(s.getValue().getNumElems()))
						.equals(costElemsRatio))
				.filter(s -> uncoveredElements.stream()
						.anyMatch(p -> s.getValue().getElems().contains(p)))
				.collect(Collectors.toList());
		return extractMaxNumElems(uncoveredFiltered);
	}

	/**
	 * 
	 * @param costAdditionalElemsRatio
	 * @return
	 */
	public Integer getBestUncoveredSetForCostAdditionalElemsRatio(Double costAdditionalElemsRatio) {
		List<Entry<Integer, Subset>> uncoveredFiltered = setElementMap.entrySet().stream()
				.filter(s -> Double
						.valueOf(
								Double.valueOf(
										s.getValue().getCost()) / Double
												.valueOf(
														ListUtils
																.intersection(
																		s.getValue().getElems(),
																		new ArrayList<>(
																				uncoveredElements))
																.size()))
						.equals(costAdditionalElemsRatio))
				.filter(s -> uncoveredElements.stream()
						.anyMatch(p -> s.getValue().getElems().contains(p)))
				.collect(Collectors.toList());
		return extractMaxNumElems(uncoveredFiltered);
	}

	/**
	 * Returns the identifier of the set which contains the biggest number of
	 * elements associated to it. Method used as second filter to refine the
	 * heuristics
	 * 
	 * @param uncoveredFiltered
	 * @return
	 */
	private Integer extractMaxNumElems(List<Entry<Integer, Subset>> uncoveredFiltered) {
		List<Entry<Integer, Subset>> maxNumElemsFiltered = uncoveredFiltered.stream()
				.filter(s -> uncoveredFiltered.stream().mapToInt(p -> p.getValue().getNumElems())
						.summaryStatistics().getMax() == s.getValue().getNumElems().intValue())
				.collect(Collectors.toList());
		return maxNumElemsFiltered
				.get(RandomUtils.getInstance(null).getRandomInt(maxNumElemsFiltered.size()))
				.getKey();
	}

	/**
	 * Starting from the highest cost set tries to eliminate sets checking that
	 * the current coverage of elements is not changed
	 */
	@SuppressWarnings("unchecked")
	public void redundancyElimination() {
		ArrayList<Entry<Integer, Subset>> orderedSets = (ArrayList<Entry<Integer, Subset>>) setElementMap
				.entrySet().stream()
				.filter(s -> coveredSets.contains(s.getKey())).sorted((s1, s2) -> Integer
						.compare(s2.getValue().getCost(), s1.getValue().getCost()))
				.collect(Collectors.toList());

		List<Entry<Integer, Subset>> filteredList;
		Iterator<Entry<Integer, Subset>> iter = orderedSets.iterator();
		Entry<Integer, Subset> tmp;
		while (iter.hasNext()) {
			tmp = iter.next();
			filteredList = ListUtils.removeAll(orderedSets, Arrays.asList(tmp));
			if (getCoveredElements(filteredList).containsAll(coveredElements)) {
				uncoveredSets.add(tmp.getKey());
				coveredSets.remove(tmp.getKey());
				iter.remove();
			}
		}

	}

	private HashSet<Integer> getCoveredElements(List<Entry<Integer, Subset>> sets) {
		HashSet<Integer> coveredElems = new HashSet<>();
		for (Entry<Integer, Subset> s : sets) {
			coveredElems.addAll(s.getValue().getElems());
		}
		return coveredElems;
	}

	public List<Integer> getOrderedUncoveredSets() {
		return this.getSetElementMap().entrySet().stream()
				.filter(s -> this.getUncoveredSets().contains(s.getKey()))
				.sorted((s1, s2) -> Integer.compare(s1.getValue().getCost(),
						s2.getValue().getCost()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
	}

	public String printableCoveredSets() {
		StringBuilder sb = new StringBuilder();
		for (Integer cs : coveredSets) {
			sb.append(cs).append(' ');
		}
		return sb.toString();
	}

	/**
	 * Contains the list of values for a given column together with the cost
	 * associated to these values
	 * 
	 * @author Fabio Navarrete
	 *
	 */
	public class Subset implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -667865852424882705L;

		/**
		 * @serial
		 */
		private ArrayList<Integer> elems = new ArrayList<>();
		/**
		 * @serial
		 */
		private Integer cost;
		/**
		 * Number of elements that the set contains
		 * 
		 * @serial
		 */
		private Integer numElems;

		public Subset(Integer cost) {
			this.cost = cost;
		}

		public Integer getNumElems() {
			return numElems;
		}

		public void setNumElems(Integer numElems) {
			this.numElems = numElems;
		}

		public ArrayList<Integer> getElems() {
			return elems;
		}

		public void setElems(ArrayList<Integer> set) {
			this.elems = set;
		}

		public Integer getCost() {
			return cost;
		}

		public void setCost(Integer cost) {
			this.cost = cost;
		}
	}

}
