package edu.kaist.ir.utils;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class CounterUtils<K> {

	public static <K> double cosine(Counter<K> counter1, Counter<K> counter2) {
		double ret = 0;

		double norm1 = normL2(counter1);
		double norm2 = normL2(counter2);
		double dotProduct = dotProduct(counter1, counter2);

		if (norm1 > 0 && norm2 > 0) {
			ret = dotProduct / (norm1 * norm2);
		}

		if (ret < 0) {
			ret = 0;
		}

		if (ret > 1) {
			ret = 1;
		}
		return ret;

	}

	public static Counter<String> counter(String text) {
		Counter<String> ret = new Counter<String>();

		for (String part : text.split(" ")) {
			String[] two = StrUtils.split2Two(":", part);
			if (two.length == 2) {
				ret.incrementCount(two[0], Double.parseDouble(two[1]));
			}
		}
		return ret;
	}

	public static Counter<String> counter(String[] parts, int start) {
		return counter(parts, start, parts.length);
	}

	public static Counter<String> counter(String[] parts, int start, int end) {
		return counter(parts, start, end, -Double.MAX_VALUE);
	}

	public static Counter<String> counter(String[] parts, int start, int end, double minCount) {
		if (start < 0) {
			start = 0;
		}

		if (end > parts.length) {
			end = parts.length;
		}

		Counter<String> ret = new Counter<String>();

		for (int i = start; i < end; i++) {
			String[] two = StrUtils.split2Two(":", parts[i]);
			if (two.length != 2) {
				continue;
			}
			String term = two[0];
			double count = Double.parseDouble(two[1]);
			if (count < minCount) {
				break;
			}
			ret.incrementCount(term, count);
		}

		return ret;
	}

	public static <K> double dotProduct(Counter<K> x, Counter<K> y) {
		return x.size() > y.size() ? y.dotProduct(x) : x.dotProduct(y);
	}

	public static Counter<String> ngrams(int order, String[] terms) {
		Counter<String> ret = new Counter<String>();
		for (int j = 0; j < terms.length - order + 1; j++) {
			StringBuffer sb = new StringBuffer();

			for (int k = j; k < j + order; k++) {
				sb.append(terms[k]);
				if (k == (j + order) - 1) {

				} else {
					sb.append("_");
				}
			}

			String ngram = sb.toString();
			ret.incrementCount(ngram, 1);
		}
		return ret;
	}

	public static <K> Counter<K> normalize(Counter<K> counter) {
		Counter<K> ret = new Counter<K>();
		double sum = counter.totalCount();
		for (Entry<K, Double> entry : counter.entrySet()) {
			K key = entry.getKey();
			double count = entry.getValue();
			ret.setCount(key, count / sum);
		}
		return ret;
	}

	public static <K> Counter<K> normalize(Counter<K> counter, double low, double high) {
		double max = -Double.MAX_VALUE;
		double min = Double.MAX_VALUE;

		for (Entry<K, Double> entry : counter.entrySet()) {
			double value = entry.getValue();

			if (value > max) {
				max = value;
			}

			if (value < min) {
				min = value;
			}
		}

		Counter<K> ret = new Counter<K>();

		for (Entry<K, Double> entry : counter.entrySet()) {
			K key = entry.getKey();
			double value = entry.getValue();
			double newValue = low + (high - low) / (max - min) * (value - min);
			ret.incrementCount(key, newValue);
		}
		return ret;
	}

	public static <K, V> CounterMap<K, V> normalize(CounterMap<K, V> counterMap) {
		CounterMap<K, V> ret = new CounterMap<K, V>(counterMap);
		ret.normalize();
		return ret;
	}

	public static <K> Counter<K> normalizeByL2NormTo(Counter<K> counter) {
		double norm = normL2(counter);
		Counter<K> ret = new Counter<K>();
		for (K key : counter.keySet()) {
			double count = counter.getCount(key);
			ret.setCount(key, count / norm);
		}
		return ret;
	}

	public static <K> Counter<K> normalizeBySoftmax(Counter<K> counter) {
		double sum = 0;
		for (Entry<K, Double> entry : counter.entrySet()) {
			sum += Math.exp(entry.getValue());
		}

		Counter<K> ret = new Counter<K>();
		for (Entry<K, Double> entry : counter.entrySet()) {
			K key = entry.getKey();
			double value = entry.getValue();
			double newValue = Math.exp(value) / sum;
			ret.setCount(key, newValue);

		}
		return ret;
	}

	public static <K> Counter<K> normalizeLogByExp(Counter<K> counter) {
		Counter<K> ret = new Counter<K>(counter);

		// Get the scores in the range near zero, where exp() is more accurate
		double maxLogProb = ret.max();

		for (Entry<K, Double> entry : ret.entrySet()) {
			K key = entry.getKey();
			double logProb = entry.getValue();
			logProb = logProb - maxLogProb;
			ret.setCount(key, logProb);
		}

		// Exponentiate and normalize
		double probSum = 0;
		for (Entry<K, Double> entry : ret.entrySet()) {
			K key = entry.getKey();
			double logProb = entry.getValue();
			double prob = Math.exp(logProb);
			probSum += prob;
			ret.setCount(key, prob);
		}

		for (Entry<K, Double> entry : ret.entrySet()) {
			K key = entry.getKey();
			double prob = entry.getValue();
			prob /= probSum;
			ret.setCount(key, prob);
		}

		return ret;
	}

	public static <K> Counter<K> normalizeLogByExp2(Counter<K> counter) {
		Counter<K> ret = new Counter<K>(counter);

		// // Get the scores in the range near zero, where exp() is more
		// accurate
		// double maxLogProb = ret.max();
		//
		// for (Entry<K, Double> entry : ret.entrySet()) {
		// K key = entry.getKey();
		// double logProb = entry.getValue();
		// logProb = logProb - maxLogProb;
		// ret.setCount(key, logProb);
		// }

		// Exponentiate and normalize
		double probSum = 0;
		for (Entry<K, Double> entry : ret.entrySet()) {
			K key = entry.getKey();
			double logProb = entry.getValue();
			double prob = Math.exp(logProb);
			probSum += prob;
			ret.setCount(key, prob);
		}

		for (Entry<K, Double> entry : ret.entrySet()) {
			K key = entry.getKey();
			double prob = entry.getValue();
			prob /= probSum;
			ret.setCount(key, prob);
		}

		return ret;
	}

	public static <K> double normL2(Counter<K> counter) {
		return normL2(counter, false);
	}

	public static <K> double normL2(Counter<K> counter, boolean normalize) {
		double norm = 0;
		for (K key : counter.keySet()) {
			double count = (normalize ? counter.getProbability(key) : counter.getCount(key));
			norm += (count * count);
		}
		norm = Math.sqrt(norm);
		return norm;
	}

	public static <K> Counter<K> rankTo(Counter<K> counter) {
		Counter<K> ret = new Counter<K>();
		int rank = 0;
		for (Entry<K, Double> entry : counter.getEntriesSortedByDecreasingCount()) {
			ret.setCount(entry.getKey(), ++rank);
		}
		return ret;
	}

	public static <K> Counter<K> scale(Counter<K> counter, double scale) {
		Counter<K> ret = new Counter<K>();
		for (Entry<K, Double> entry : counter.entrySet()) {
			K key = entry.getKey();
			double count = entry.getValue();
			ret.setCount(key, count * scale);
		}
		return ret;
	}

	public static <K> Counter<K> subCounterTo(Collection<K> collection, Counter<K> counter) {
		Counter<K> ret = new Counter<K>();
		Iterator<K> iter = collection.iterator();
		while (iter.hasNext()) {
			K key = iter.next();
			if (counter.containsKey(key)) {
				ret.setCount(key, counter.getCount(key));
			}
		}
		return ret;
	}

	public static Counter<String> termCounts(String text) {
		String[] parts = text.split("[\\s]+");
		return counter(parts, 0, parts.length);
	}

	public static CounterMap<String, String> tfidf(CounterMap<String, String> counterMap) {
		CounterMap<String, String> ret = new CounterMap<String, String>();

		Counter<String> key2_docFreq = new Counter<String>();
		for (String key1 : counterMap.keySet()) {
			for (String key2 : counterMap.getCounter(key1).keySet()) {
				key2_docFreq.incrementCount(key2, 1);
			}
		}

		for (String key1 : counterMap.keySet()) {
			Counter<String> counter = counterMap.getCounter(key1);
			Counter<String> counter2 = new Counter<String>();
			double norm = 0;
			for (String key2 : counter.keySet()) {
				double count = counter.getCount(key2);
				double numDocs = counterMap.keySet().size();
				double docFreq = key2_docFreq.getCount(key2);
				double tfidf = count * Math.log(numDocs / docFreq);
				counter2.setCount(key2, tfidf);
				norm += tfidf;
			}
			norm = Math.sqrt(norm);
			counter2.scale(1f / norm);
			ret.setCounter(key1, counter2);
		}
		return ret;
	}

	public static <K> String toString(String delim, Counter<K> counter, NumberFormat nf) {
		if (nf == null) {
			nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(4);
			nf.setGroupingUsed(false);
		}

		StringBuffer sb = new StringBuffer();
		List<K> keys = counter.getSortedKeys();
		for (int i = 0; i < keys.size(); i++) {
			K key = keys.get(i);
			double value = counter.getCount(key);
			sb.append(String.format("%s:%s", key.toString(), nf.format(value)));

			if (i != keys.size() - 1) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

}
