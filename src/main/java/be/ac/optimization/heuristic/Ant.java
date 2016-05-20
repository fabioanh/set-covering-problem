package be.ac.optimization.heuristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.SerializationUtils;

public class Ant {
	private SetCoveringProblem problem;
	private HashMap<Integer, Double> pheromone;
	private HashMap<Integer, Double> heuristicInformation;
	private Double beta;

	public Ant(SetCoveringProblem scp, HashMap<Integer, Double> pheromone,
			HashMap<Integer, Double> heuristicInformation, Double beta) {
		problem = scp;
		this.pheromone = pheromone;
		this.heuristicInformation = heuristicInformation;
		this.beta = beta;
	}

	/**
	 * Copy Constructor
	 * 
	 * @param ant
	 */
	public Ant(Ant ant) {
		problem = SerializationUtils.clone(ant.getProblem());
		this.pheromone = ant.pheromone;
		this.heuristicInformation = ant.getHeuristicInformation();
		this.beta = ant.getBeta();
	}

	/**
	 * Using as basis the method and probability computation proposed in
	 * "New ideas for applying ant colony optimization to the set covering problem"
	 * , Z. Ren 2010, executes the process to give a solution for the current
	 * ant to the SCP
	 */
	public void solve() {
		problem.uncoverAllSets();
		while (!problem.getUncoveredElements().isEmpty()) {
			coverNextSet();
		}
	}

	/**
	 * Updates the heuristic information based on the input set that has been
	 * just covered in the solution construction process. Maintains the
	 * heuristic information as a dynamic one
	 * 
	 * @param coveredSet
	 */
	@SuppressWarnings("unchecked")
	private void updateHeuristicInformation(Integer set) {
		List<Integer> uncoveredElementsForSet = ListUtils.removeAll(
				problem.getSetElementMap().get(set).getElems(), problem.getCoveredElements());
		// Based on the uncovered elements that the parameter set has updates
		// the heuristic information for the remaining uncovered sets in the
		// problem
		Set<Integer> setsToUpdate = new HashSet<>();
		for (Integer elem : uncoveredElementsForSet) {
			setsToUpdate.addAll(CollectionUtils.removeAll(problem.getElementSetMap().get(elem),
					problem.getCoveredSets()));
		}
		for (Integer s : setsToUpdate) {
			heuristicInformation.put(s,
					Double.valueOf(ListUtils.removeAll(problem.getSetElementMap().get(s).getElems(),
							problem.getCoveredElements()).size())
							/ Double.valueOf(problem.getSetElementMap().get(s).getCost()));
		}
	}

	/**
	 * Logic to cover the next set in the solution creation. After a set is
	 * selected to be covered, updates the information on the problem related to
	 * it
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private void coverNextSet() {
		Integer randomElement = RandomUtils.getInstance(null)
				.getRandomFromSet(problem.getUncoveredElements());
		problem.getElementSetMap().get(randomElement);
		ArrayList<Integer> availableSets = (ArrayList<Integer>) ListUtils
				.removeAll(problem.getElementSetMap().get(randomElement), problem.getCoveredSets());
		Integer setToCover = getSetToCover(availableSets);
		problem.coverSet(setToCover);
		updateHeuristicInformation(setToCover);
	}

	/**
	 * Gets the next set to be covered in the solution building process.
	 * 
	 * @param availableSets
	 * @return
	 */
	private Integer getSetToCover(ArrayList<Integer> availableSets) {
		Double rand = RandomUtils.getInstance(null).getRandomDouble();
		Double sum = 0.0;
		for (Integer set : availableSets) {
			sum += probabilityForSet(set, availableSets);
			if (sum >= rand) {
				return set;
			}
		}
		throw new RuntimeException(
				"Non Reachable Code. Probability for covering a set should be always reached at a given point");
	}

	/**
	 * Using the pheromone and heuristic information returns the probability for
	 * a set to be chosen from the list of available sets
	 * 
	 * @param set
	 * @param sets
	 * @return
	 */
	private Double probabilityForSet(Integer set, ArrayList<Integer> sets) {
		Double numerator = pheromone.get(set) * Math.pow(heuristicInformation.get(set), beta);
		Double denominator = 0.0;
		for (Integer s : sets) {
			denominator += pheromone.get(s) * Math.pow(heuristicInformation.get(s), beta);
		}
		return numerator / denominator;
	}

	public SetCoveringProblem getProblem() {
		return problem;
	}

	public void setProblem(SetCoveringProblem problem) {
		this.problem = problem;
	}

	public HashMap<Integer, Double> getPheromone() {
		return pheromone;
	}

	public void setPheromone(HashMap<Integer, Double> pheromone) {
		this.pheromone = pheromone;
	}

	public HashMap<Integer, Double> getHeuristicInformation() {
		return heuristicInformation;
	}

	public void setHeuristicInformation(HashMap<Integer, Double> heuristicInformation) {
		this.heuristicInformation = heuristicInformation;
	}

	public Double getBeta() {
		return beta;
	}

	public void setBeta(Double beta) {
		this.beta = beta;
	}

	public Integer getCost() {
		return this.getProblem().getCoveredSetsCost();
	}
}
