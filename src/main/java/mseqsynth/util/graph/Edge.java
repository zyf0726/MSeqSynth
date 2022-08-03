package mseqsynth.util.graph;

import com.google.common.base.Preconditions;

public class Edge<N, L extends Comparable<L>> {
	N head, tail;
	L label;
	
	public Edge(N head, N tail, L label) {
		Preconditions.checkNotNull(label, "a non-null and comparable label expected");
		this.head = head;
		this.tail = tail;
		this.label = label;
	}
}
