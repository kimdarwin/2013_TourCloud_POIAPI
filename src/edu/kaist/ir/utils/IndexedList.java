package edu.kaist.ir.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexedList<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

	protected Map<K, List<V>> entries;

	public IndexedList() {
		entries = new HashMap<K, List<V>>();
	}

	protected List<V> ensure(K key) {
		List<V> list = entries.get(key);
		if (list == null) {
			list = new ArrayList<V>();
			entries.put(key, list);
		}
		return list;
	}

	public List<V> get(K key) {
		return ensure(key);
	}

	protected List<V> insure(K key) {
		List<V> list = entries.get(key);
		if (list == null) {
			list = new ArrayList<V>();
			entries.put(key, list);
		}
		return list;
	}

	public Set<K> keySet() {
		return entries.keySet();
	}

	public void put(K key, V value) {
		ensure(key).add(value);
	}

	public List<V> remove(K key) {
		return entries.remove(key);
	}

	public int size() {
		return entries.size();
	}

	public String toString() {
		return toString(20);
	}

	public boolean containsKey(K key) {
		return entries.containsKey(key);
	}

	public String toString(int printSize) {
		StringBuffer sb = new StringBuffer();
		for (K key : entries.keySet()) {
			sb.append(key.toString() + " => ");
			List<V> List = entries.get(key);
			int size = List.size();
			int numElems = 0;
			for (V value : List) {
				sb.append(value.toString() + (++numElems >= size ? "" : ", "));
				if (numElems > printSize) {
					sb.append("...");
					break;
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
