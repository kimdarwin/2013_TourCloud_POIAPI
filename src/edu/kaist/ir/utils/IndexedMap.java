package edu.kaist.ir.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IndexedMap<K, E, V> implements Serializable {

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

	protected Map<K, Map<E, V>> entries;

	public IndexedMap() {
		entries = new HashMap<K, Map<E, V>>();
	}

	protected Map<E, V> ensure(K key) {
		Map<E, V> map = entries.get(key);
		if (map == null) {
			map = new HashMap<E, V>();
			entries.put(key, map);
		}
		return map;
	}

	public V get(K key, E elem) {
		Map<E, V> map = entries.get(key);
		V ret = null;
		if (map != null) {
			ret = map.get(elem);
		}
		return ret;
	}

	public Map<E, V> getMap(K key) {
		return entries.get(key);
	}

	public Set<K> keySet() {
		return entries.keySet();
	}

	public Set<E> keySet(K key) {
		return entries.get(key).keySet();
	}

	public void put(K key, E elem, V value) {
		Map<E, V> map = ensure(key);
		map.put(elem, value);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (K key : entries.keySet()) {
			sb.append(key.toString() + " => ");
			Map<E, V> map = entries.get(key);
			int size = map.size();
			int num = 0;
			for (E elem : map.keySet()) {
				V value = map.get(elem);
				sb.append(elem.toString() + ":" + value.toString() + (++num >= size ? "" : " "));
			}
			sb.append("\n");
		}

		return sb.toString();
	}

}
