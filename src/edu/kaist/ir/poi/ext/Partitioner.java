package edu.kaist.ir.poi.ext;

import java.util.ArrayList;
import java.util.List;

import edu.kaist.ir.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * 
 * This class provides a method for partitioning a text string into a set of segments.
 *
 */
public class Partitioner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("process begins.");
		String entity = "vanateshe";
		String entity2 = "surajit_chaudri";
		String entity3 = "일자산 해맞이 광장";
		Partitioner p = new Partitioner(2, false, 0);
		p.partition(entity);
		// System.out.println(StrUtils.join("|", p.partition(entity)));
		// System.out.println();
		// System.out.println(StrUtils.join("|", p.partition(entity2)));
		// System.out.println();
		// System.out.println(StrUtils.join("|", p.partition(entity3)));
		System.out.println();

		System.out.println("process ends.");

	}

	/**
	 * 
	 */
	private int minEditDist;

	/**
	 * a token is segmented after tokenized by a white space if true.
	 */
	private boolean partitionToken;

	/**
	 * a text is not segmented if its length is lower than the minimum.
	 */
	private int minPartitionLen;

	public Partitioner(int minEditDist, boolean partitionToken, int minPartitionLen) {
		this.minEditDist = minEditDist;
		this.partitionToken = partitionToken;
		this.minPartitionLen = minPartitionLen;
	}

	/**
	 * Partition an entity into a set of segments
	 * 
	 * 
	 * @param entity
	 * @return
	 */
	public Segment[] partition(String entity) {
		Segment[] ret = null;
		if (partitionToken) {
			List<Segment> list = new ArrayList<Segment>();

			for (String tok : StrUtils.split(entity)) {
				for (Segment segment : partition2(tok)) {
					list.add(segment);
				}
			}

			ret = new Segment[list.size()];
			for (int i = 0; i < list.size(); i++) {
				ret[i] = list.get(i);
			}

		} else {
			ret = partition2(entity);
		}

		return ret;
	}

	private Segment[] partition2(String entity) {
		Segment[] ret = null;
		int eLen = entity.length();

		if (eLen < minPartitionLen) {
			ret = new Segment[1];
			ret[0] = new Segment(entity, 0, entity.length());
		} else {
			int numSegments = minEditDist + 1;
			ret = new Segment[numSegments];
			int k = eLen - (int) Math.floor(eLen / numSegments) * numSegments;

			for (int i = 1, start = 0; i <= numSegments; i++) {
				int segmentLen = 0;
				if (i <= k) {
					segmentLen = (int) Math.ceil(1f * eLen / numSegments);

				} else {
					segmentLen = (int) Math.floor(1f * eLen / numSegments);
				}
				int end = start + segmentLen;
				ret[i - 1] = new Segment(entity.substring(start, end), start, end);

				// System.out.printf("%d: %s [%d-%d]\n", i, ret[i - 1], start,
				// end);
				start += segmentLen;
			}
		}

		return ret;
	}
}
