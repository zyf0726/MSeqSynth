package mseqsynth.util.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Preconditions;

// TODO refactor using ValueGraph

public class GraphAnalyzer<N, L extends Comparable<L>> {
	
	private ArrayList<N> allNodes;
	private Map<N, Integer> node2id;
	private ArrayList<SortedMap<L, Integer>> adjMap;
	
	private int sccID[];	
	private ArrayList<ArrayList<Integer>> allSCCs;
	
	private ArrayList<FeatureNodewise> featNode;
	private FeatureGraphwise featGraph;
	
	public GraphAnalyzer(Collection<N> nodes, Collection<Edge<N, L>> edges) {
		this.allNodes = new ArrayList<>();
		this.node2id = new HashMap<>();
		this.adjMap = new ArrayList<>();
		if (nodes != null) {
			for (N anode : nodes) {
				if (this.node2id.containsKey(anode))
					continue;
				this.node2id.put(anode, this.allNodes.size());
				this.allNodes.add(anode);
				this.adjMap.add(new TreeMap<>());
			}
		}
		if (edges != null) {
			for (Edge<N, L> e : edges) {
				Integer head = this.node2id.get(e.head);
				Integer tail = this.node2id.get(e.tail);
				Preconditions.checkNotNull(head, "the head of an edge in 'edges' not in 'nodes'");
				Preconditions.checkNotNull(tail, "the tail of an edge in 'edges' not in 'nodes'");
				this.adjMap.get(head).put(e.label, tail);
			}
		}
		this.tarjanSCCAlgorithm();
		this.featNode = null;
		this.featGraph = null;
	}
	
	// In Tarjan's Algorithm, SCCs are extracted in (reversed) topological order of the reduced DAG.
	void tarjanSCCAlgorithm() {
		final int numNodes = this.allNodes.size();
		this.allSCCs = new ArrayList<>();
		this.sccID = new int[numNodes];
		
		Deque<Integer> aStack = new ArrayDeque<>();
		boolean inStack[] = new boolean[numNodes];
		int dfsNum[] = new int[numNodes];
		int dfsLow[] = new int[numNodes];
		int timeStamp = 0;
		
		for (int u = 0; u < numNodes; ++u)
			if (dfsNum[u] == 0) { // not visited yet
				timeStamp = __tarjanDFS(u, timeStamp, dfsNum, dfsLow, aStack, inStack);
			}
	}
	
	private int __tarjanDFS(int u, int timeStamp, int[] dfsNum, int[] dfsLow,
			Deque<Integer> aStack, boolean[] inStack) {
		timeStamp += 1;
		dfsNum[u] = timeStamp;
		dfsLow[u] = timeStamp;
		aStack.push(u);
		inStack[u] = true;
		
		for (int v : this.adjMap.get(u).values()) {
			if (dfsNum[v] == 0) {  // tree edge
				timeStamp = __tarjanDFS(v, timeStamp, dfsNum, dfsLow, aStack, inStack);
				dfsLow[u] = Math.min(dfsLow[u], dfsLow[v]);
			} else if (inStack[v]) {  // back edge
				dfsLow[u] = Math.min(dfsLow[u], dfsNum[v]);
			} // otherwise cross edge, do nothing
		}
		
		// head node found, extract SCC 
		if (dfsNum[u] == dfsLow[u]) {
			ArrayList<Integer> scc = new ArrayList<>();
			while (true) {
				int v = aStack.pop();
				inStack[v] = false;
				this.sccID[v] = this.allSCCs.size();
				scc.add(v);
				if (v == u) break;
			}
			this.allSCCs.add(scc);
		}
		return timeStamp;
	}
	
	public Integer getSCCIdentifier(N anode) {
		Integer id = node2id.get(anode);
		if (id == null) {
			return null;
		}
		return sccID[id];
	}
	
	public ArrayList<N> getSCCMembers(N anode) {
		Integer id = node2id.get(anode);
		if (id == null) {
			return null;
		}
		ArrayList<N> sccMems = new ArrayList<>();
		for (int uid : allSCCs.get(sccID[id]))
			sccMems.add(allNodes.get(uid));
		return sccMems;
	}
	
	public FeatureNodewise getFeatureNodewise(N anode) {
		Integer id = this.node2id.get(anode);
		if (id == null) {
			return null;
		}
		if (this.featNode == null) {
			this.featNode = computeNodewiseFeature();
		}
		return this.featNode.get(id);
	}
	
	public FeatureGraphwise getFeatureGraphwise() {
		if (this.featGraph == null) {
			this.featGraph = new FeatureGraphwise();
			this.featGraph.numNodes = computeNumberNodes();
			this.featGraph.sizeSCCsRepr = computeSizeSCCsRepr();
		}
		return this.featGraph;
	}
	
	ArrayList<FeatureNodewise> computeNodewiseFeature() {
		ArrayList<FeatureNodewise> featNode = new ArrayList<>();
		for (int id = 0; id < allNodes.size(); ++id)
			featNode.add(new FeatureNodewise());
		/* compute in-degree */
		for (int uid = 0; uid < allNodes.size(); ++uid)
			for (int vid : this.adjMap.get(uid).values()) {
				featNode.get(vid).inDeg += 1;
			}
		/* compute size of the SCC located */
		for (int id = 0; id < allNodes.size(); ++id)
			featNode.get(id).sizeSCC = allSCCs.get(sccID[id]).size();
		return featNode;
	}
	
	int computeNumberNodes() {
		return this.allNodes.size();
	}
	
	String computeSizeSCCsRepr() {
		StringBuilder sb = new StringBuilder();
		ArrayList<Integer> sizeSCCs = new ArrayList<>();
		sizeSCCs.add(0);
		for (ArrayList<Integer> scc : allSCCs)
			sizeSCCs.add(scc.size());
		Collections.sort(sizeSCCs);
		for (int i = 1; i < sizeSCCs.size(); ++i) {
			sb.append(sizeSCCs.get(i) - sizeSCCs.get(i-1));
			sb.append(".");
		}
		return sb.toString();
	}
	
}
