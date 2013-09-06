package edu.kaist.ir.tree.trie;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import edu.kaist.ir.utils.StrUtils;

public class Trie<K> {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Process begins.");

		System.out.println("Process ends.");
	}

	private int depth;

	private int size;

	private Node<K> root;

	public Trie() {
		root = new Node<K>(null, null, 0, null, -1);
		depth = 0;
		size = 0;
	}

	public List<Node<K>> allNodes() {
		return root.allNodesUnder();
	}

	public void delete(K[] keys) {

	}

	public int depth() {
		return depth;
	}

	public Node<K> insert(K[] keys) {
		Node<K> node = root;
		for (K key : keys) {
			Node<K> child;
			if (node.hasChild(key)) {
				child = node.child(key);
				child.incrementCount();
			} else {
				child = new Node<K>(node, key, node.depth() + 1, null, size);
				node.addChild(child);
				size++;
			}
			node = child;
			if (node.depth() > depth) {
				depth = node.depth();
			}
		}
		return node;
	}

	public List<Node<K>> leafNodes() {
		return root.leafNodesUnder();
	}

	public Node<K> root() {
		return root;
	}

	public Node<K> search(K[] keys) {
		Node<K> node = root;

		for (K key : keys) {
			if (node.hasChild(key)) {
				node = node.child(key);
			} else {
				node = null;
				break;
			}
		}
		return node;
	}

	public int size() {
		return size;
	}
	
	@Override
	public String toString() {
		NumberFormat nf = NumberFormat.getInstance();
		List<String> list = new ArrayList<String>();
		list.add(String.format("[max depth: %s]", nf.format(depth)));
		list.add(String.format("[node size: %s]", nf.format(size)));
		list.add(root.toString());
		return StrUtils.join("\n", list);
	}
}
