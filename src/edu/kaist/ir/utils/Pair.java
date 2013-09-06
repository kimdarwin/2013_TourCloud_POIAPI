package edu.kaist.ir.utils;

import java.io.Serializable;

/**
 * A generic-typed pair of objects.
 * 
 * @author Dan Klein
 */
public class Pair<F, S> implements Serializable {
	static final long serialVersionUID = 42;

	private F first;

	private S second;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	@Override
	public String toString() {
		return "(" + getFirst() + ", " + getSecond() + ")";
	}

	public Pair(final F first, final S second) {
		this.first = first;
		this.second = second;
	}

	public static <S, T> Pair<S, T> newPair(final S first, final T second) {
		return new Pair<S, T>(first, second);
	}
}
