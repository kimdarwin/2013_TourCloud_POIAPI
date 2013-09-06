package edu.kaist.ir.tree.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Node<K> {
	protected Map<K, Node<K>> children;
	protected double count;
	protected int depth;
	protected K key;
	protected Node<K> parent;
	protected Object data;
	protected int id;

	// public Node(Node<K> parent, K key, int depth, int id) {
	// this(parent, key, depth, null, id);
	// }

	public Node(Node<K> parent, K key, int depth, Object data, int id) {
		this.parent = parent;
		this.key = key;
		this.depth = depth;
		this.children = null;
		this.data = data;
		this.count = 1;
	}

	public void addChild(Node<K> node) {
		if (children == null) {
			children = new HashMap<K, Node<K>>();
		}
		children.put(node.key(), node);
	}

	public List<Node<K>> allNodesExceptUnder(Node<K> exceptNode) {
		Set<Node<K>> visited = new HashSet<Node<K>>();
		Stack<Node<K>> fringe = new Stack<Node<K>>();
		fringe.add(this);

		while (!fringe.empty()) {
			Node<K> node = fringe.pop();

			if (!visited.contains(node) && !node.equals(exceptNode)) {
				visited.add(node);
				for (Node<K> child : node.children()) {
					fringe.push(child);
				}
			}
		}
		return new ArrayList<Node<K>>(visited);
	}

	public List<Node<K>> allNodesUnder() {
		Set<Node<K>> visited = new HashSet<Node<K>>();
		Stack<Node<K>> fringe = new Stack<Node<K>>();
		fringe.add(this);

		while (!fringe.empty()) {
			Node<K> node = fringe.pop();

			if (!visited.contains(node)) {
				visited.add(node);
				for (Node<K> child : node.children()) {
					fringe.push(child);
				}
			}
		}
		visited.remove(this);
		return new ArrayList<Node<K>>(visited);
	}

	public Node<K> child(K key) {
		return children.get(key);
	}

	public List<Node<K>> children() {
		List<Node<K>> ret = new ArrayList<Node<K>>();
		if (hasChildren()) {
			ret.addAll(children.values());
		}
		return ret;
	}

	public double count() {
		return count;
	}

	public Object data() {
		return data;
	}

	public int depth() {
		return depth;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Node)) {
			return false;
		}
		Node other = (Node) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		return true;
	}

	public boolean hasChild(K key) {
		return children == null || !children.containsKey(key) ? false : true;
	}

	public boolean hasChildren() {
		return children == null || children.size() == 0 ? false : true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (key == null ? 0 : key.hashCode());
		result = prime * result + (parent == null ? 0 : parent.hashCode());
		return result;
	}

	public boolean hasParent() {
		return parent == null ? false : true;
	}

	public int id() {
		return id;
	}

	public void incrementCount() {
		count++;
	}

	public boolean isLeaf() {
		return children == null ? true : false;
	}

	public boolean isRoot() {
		return depth == 0 ? true : false;
	}

	public K key() {
		return key;
	}

	public List<K> keyPath() {
		List<K> ret = new ArrayList<K>();
		for (Node<K> parent : path()) {
			ret.add(parent.key());
		}
		return ret;
	}

	public String keyPath(String delim) {
		List<K> keyPath = keyPath();
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < keyPath.size(); i++) {
			K key = keyPath.get(i);
			if (i == 0) {
				sb.append(key.toString());
			} else {
				sb.append(delim + key.toString());
			}
		}
		return sb.toString();
	}

	public List<Node<K>> leafNodesUnder() {
		List<Node<K>> ret = new ArrayList<Node<K>>();
		Set<Node<K>> visited = new HashSet<Node<K>>();
		Stack<Node<K>> fringe = new Stack<Node<K>>();
		fringe.add(this);

		while (!fringe.empty()) {
			Node<K> node = fringe.pop();
			if (node.isLeaf()) {
				ret.add(node);
			}

			if (!visited.contains(node)) {
				visited.add(node);
				for (Node<K> child : node.children()) {
					fringe.push(child);
				}
			}
		}
		return ret;
	}

	public List<Node<K>> path() {
		List<Node<K>> ret = parents();
		ret.add(this);
		return ret;
	}

	public Node<K> parent() {
		return parent;
	}

	public List<Node<K>> parents() {
		return parents(-1);
	}

	public List<Node<K>> parents(int distance) {
		List<Node<K>> ret = new ArrayList<Node<K>>();
		Node<K> node = this;

		while (node.hasParent() && !node.parent().isRoot()) {
			if (distance != -1 && ret.size() == distance) {
				break;
			}
			node = node.parent();
			ret.add(node);
		}

		Collections.reverse(ret);
		return ret;
	}

	public void setData(Object data) {
		this.data = data;
	}

	/**
	 * @added Eunyoung Kim
	 * 
	 * compute the number of edges from this node to another node
	 * 
	 */
	public int distance(Node<K> q) {
		int distance = 0;
		List pPath = this.path();
		List qPath = q.path();

		if (pPath.size() > qPath.size()) {
			List temp = pPath;
			pPath = qPath;
			qPath = temp;
		}

		for (int i = 0; i < pPath.size(); i++) {
			if (!pPath.get(i).equals(qPath.get(i)))
				distance += 2;
		}

		distance += (qPath.size() - pPath.size());

		return distance;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		String type = "non-leaf";
		if (isRoot()) {
			type = "root";
		} else if (isLeaf()) {
			type = "leaf";
		}

		sb.append(String.format("[id : %s]\n", id));
		sb.append(String.format("[type : %s]\n", type));
		sb.append(String.format("[depth: %d]\n", depth));
		sb.append(String.format("[key : %s]\n", key == null ? "null" : key.toString()));

		if (!isRoot()) {
			StringBuffer sb2 = new StringBuffer();
			List<Node<K>> nodes = path();
			for (int i = 0; i < nodes.size(); i++) {
				Node<K> node = nodes.get(i);
				sb2.append(i == nodes.size() - 1 ? node.key().toString() : node.key().toString() + "-");
			}
			sb.append(String.format("[key path : %s]\n", sb2.toString()));
		}

		sb.append(String.format("[children : %d]\n", children().size()));
		int no = 0;
		for (Node<K> child : children()) {
			sb.append(String.format("  [%dth %s -> %d children]\n", ++no, child.key, child.children().size()));
		}
		return sb.toString().trim();
	}
}
