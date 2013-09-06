package edu.kaist.ir.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IndexedSet<K, V> implements Serializable {

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

	protected Map<K, Set<V>> entries;

	public IndexedSet() {
		entries = new HashMap<K, Set<V>>();
	}

	protected Set<V> ensure(K key) {
		Set<V> set = entries.get(key);
		if (set == null) {
			set = new HashSet<V>();
			entries.put(key, set);
		}
		return set;
	}

	public Set<V> get(K key) {
		return entries.get(key);
	}

	public Set<K> keySet() {
		return entries.keySet();
	}

	public boolean containsKey(K key) {
		return entries.containsKey(key);
	}

	public boolean contains(K key, V value) {
		boolean ret = false;
		Set<V> set = entries.get(key);
		if (set != null && set.contains(value)) {
			ret = true;
		}
		return ret;
	}

	public void put(K key, V value) {
		ensure(key).add(value);
	}

	public void put(K key, Set<V> values) {
		for (V value : values) {
			put(key, value);
		}
	}

	public int size() {
		return entries.size();
	}

	public String toString() {
		return toString(20);
	}

	public String toString(int printSize) {
		StringBuffer sb = new StringBuffer();
		for (K key : entries.keySet()) {
			sb.append(key.toString() + " => ");
			Set<V> set = entries.get(key);
			int size = set.size();
			int numElems = 0;
			for (V value : set) {
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

	public IndexedSet<V, K> invert() {
		IndexedSet<V, K> ret = new IndexedSet<V, K>();
		for (K key : keySet()) {
			for (V value : get(key)) {
				ret.put(value, key);
			}
		}
		return ret;
	}

}
