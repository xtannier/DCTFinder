package fr.limsi.tools.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class ListTools {
	public static <T extends Comparable<T>> TreeSet<T> mergeSortedSets(Collection<? extends SortedSet<T>> sortedLists) {
		TreeSet<T> result = new TreeSet<T>();
		int blockNumber = sortedLists.size();
		final List<Iterator<T>> listIterators = new ArrayList<Iterator<T>>(blockNumber);
		final List<T> listPointers = new ArrayList<T>(blockNumber);

		Iterator<T> iterator;
		int index = 0;

		// Création du buffer de priorité, avec pour critère de priorité l'ordre de la liste
		PriorityQueue<Integer> bufferList = new PriorityQueue<Integer>(blockNumber, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				int c = listPointers.get(o1).compareTo(listPointers.get(o2));
				if (c == 0) {
					return o1.compareTo(o2);
				} else {
					return c;
				}
			}
		});
		
		for (Collection<T> collection : sortedLists) {
//			System.out.println("Liste " + index + " : " + collection);
			iterator = collection.iterator();			
			if (iterator.hasNext()) {
				listIterators.add(iterator);
				listPointers.add(iterator.next());
				bufferList.add(index++);
			}

		}
		
//		boolean firstIteration;
//		T previousElementInList;
		T currentElement = null;
		T element;		

		
		while (!bufferList.isEmpty()) {
			index = bufferList.poll();
//			previousTermIdInFile = 0; //termIdPointers[index];
//			previousElementInList = null;
//			firstIteration = true;

			element = listPointers.get(index);

			// Si l'élément est différent de l'élément courant, c'est qu'on 
			// ne l'a pas déjà rencontré et donc qu'on l'ajoute 
			if (currentElement == null || !element.equals(currentElement)) {
				result.add(element);
			}

			// On met à jour
			iterator = listIterators.get(index);
			if (iterator.hasNext()) {
				listPointers.set(index, iterator.next());
				bufferList.add(index);
			}
		}

		return result;
	}
	
	public static <T> List<T>[] split(List<T> collection, int foldNumber) {
		double[] weights = new double[foldNumber];
		double weight = 1.0/(double)foldNumber;
		for (int i = 0 ; i < foldNumber ; i++) {
			weights[i] = weight;
		}
		return split(collection, weights);
	}
	
	public static <T> List<T>[] randomSplit(List<T> collection, int foldNumber) {
		double[] weights = new double[foldNumber];
		double weight = 1.0/(double)foldNumber;
		for (int i = 0 ; i < foldNumber ; i++) {
			weights[i] = weight;
		}
		return randomSplit(collection, foldNumber);
	}
	
	public static <T> List<T>[] randomSplit(List<T> collection, int foldNumber, long seed) {
		double[] weights = new double[foldNumber];
		double weight = 1.0/(double)foldNumber;
		for (int i = 0 ; i < foldNumber ; i++) {
			weights[i] = weight;
		}
		return randomSplit(collection, weights, seed);
	}

	
	public static <T> List<T>[] split(List<T> collection, double[] weights) {
		return split(collection, weights, true, null);
	}

	
	public static <T> List<T>[] randomSplit(List<T> collection, double[] weights) {
		return split(collection, weights, false, null);
	}

	public static <T> List<T>[] randomSplit(List<T> collection, double[] weights, long seed) {
		return split(collection, weights, false, seed);
	}

	
	private static <T> List<T>[] split(List<T> collection, double[] weights, boolean preserveOrder, Long seed) {
		int splitNumber = weights.length;
		@SuppressWarnings("unchecked")
		List<T>[] result = new List[splitNumber];
		double totalWeight = 0;
		double biggestWeight = 0.0;
		int biggestWeightIndex = 0;
		for (int splitIndex = 0 ; splitIndex < splitNumber ; splitIndex++) {
			totalWeight += weights[splitIndex];
			if (weights[splitIndex] > biggestWeight) {
				biggestWeight = weights[splitIndex];
				biggestWeightIndex = splitIndex;
			}
		}
		
		int elemNumber;
		int randomIndex = 0;
		
		Random random = null;
		if (!preserveOrder) {
			if (seed == null) {
				random = new Random();
			} else {
				random = new Random(seed);
			}
		}
			
		HashSet<Integer> selectedElems = new HashSet<Integer>();
		
		// n-1 first split parts
		for (int splitIndex = 0 ; splitIndex < splitNumber ; splitIndex++) {
			if (splitIndex != biggestWeightIndex) {
				elemNumber = (int)Math.round(weights[splitIndex] / totalWeight * collection.size());
				List<T> splitFeatures;
				if (collection instanceof ArrayList) {
					splitFeatures = new ArrayList<T>();
				} else {
					throw new RuntimeException("split is not implemented for type " + collection.getClass());
				}
				for (int elemIndex = 0 ; elemIndex < elemNumber ; elemIndex++) {
					if (preserveOrder) {
						randomIndex++;
					} else {
						do {
							randomIndex = random.nextInt(collection.size());
						} while (selectedElems.contains(randomIndex));
					}
					splitFeatures.add(collection.get(randomIndex));
					selectedElems.add(randomIndex);
				}
				result[splitIndex] = splitFeatures;
			}
		}
		
		// Last split part
		List<T> splitElems;
		if (collection instanceof ArrayList) {
			splitElems = new ArrayList<T>();
		} else {
			throw new RuntimeException("split is not implemented for type " + collection.getClass());
		}
		for (int elemIndex = 0 ; elemIndex < collection.size() ; elemIndex++) {
			if (!selectedElems.contains(elemIndex)) {
				splitElems.add(collection.get(elemIndex));
			}
		}
		result[biggestWeightIndex] = splitElems;		
		return result;
	}
	
	public static void main(String[] args) {
//		TreeSet<Integer> set1 = new TreeSet<Integer>();
//		set1.add(4);
//		set1.add(1);
//		set1.add(2);
//		set1.add(18);
//		set1.add(25);
//		set1.add(5);
//		set1.add(15);
//		TreeSet<Integer> set2 = new TreeSet<Integer>();
//		set2.add(2);
//		set2.add(6);
//		set2.add(35);
//		set2.add(14);
//		set2.add(14);
//		set2.add(25);
//		set2.add(4);
//		TreeSet<Integer> set3 = new TreeSet<Integer>();
//		set3.add(17);
//		set3.add(4);
//		set3.add(94);
//		set3.add(35);
//		set3.add(2);
//		TreeSet<Integer> set4 = new TreeSet<Integer>();
//		set4.add(100);
//		set4.add(12);
//		set4.add(2);
//		set4.add(14);
//		set4.add(8);
//		set4.add(3);
//		set4.add(13);
//		ArrayList<TreeSet<Integer>> sets = new ArrayList<TreeSet<Integer>>();
//		sets.add(set1);
//		sets.add(set2);
//		sets.add(set3);
//		sets.add(set4);
//		System.out.println(mergeSortedSets(sets));
		
		Pattern pattern = Pattern.compile("(mar)");
		System.out.println(pattern.matcher("mars").matches());
	}
}
