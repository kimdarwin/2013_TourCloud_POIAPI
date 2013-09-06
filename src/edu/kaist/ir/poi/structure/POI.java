package edu.kaist.ir.poi.structure;

import java.util.Arrays;

/**
 * This class hold information of POI
 * 
 * @author Heung-Seon Oh
 * 
 */
public class POI {
	/**
	 * parent locations of POI hierarchy *
	 */
	public String[] parentLocations;

	public Sentence poiSent;

	public POI(String[] parentLocations, Sentence poiSent) {
		this.parentLocations = parentLocations;
		this.poiSent = poiSent;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		POI other = (POI) obj;
		if (!Arrays.equals(parentLocations, other.parentLocations))
			return false;
		if (poiSent == null) {
			if (other.poiSent != null)
				return false;
		} else if (!poiSent.equals(other.poiSent))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(parentLocations);
		result = prime * result + ((poiSent == null) ? 0 : poiSent.hashCode());
		return result;
	}

	public String[] parentLocations() {
		return parentLocations;
	}

	public Sentence poiSentence() {
		return poiSent;
	}

	public String text() {
		return poiSent.text();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < parentLocations.length; i++) {
			sb.append(parentLocations[i] + "-");
		}
		sb.append(poiSent.text());
		return sb.toString();
	}
	
	/**
	 * @added Eunyoung Kim
	 * 
	 * compute the number of edges from this POI node to another node in the trie
	 * 
	 */
	public int distance(POI q)
	{
		String[] pParentLocations = parentLocations;
		String[] qParentLocations = q.parentLocations();
		int distance = 8; // level 4
		
		for (int i = 0; i < pParentLocations.length; i++)
		{
			if (pParentLocations[i].equals(qParentLocations[i]))
				distance -= 2;
			else
				break;
		}
		
		return distance;
	}
}
