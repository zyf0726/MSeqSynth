package mseqsynth.util.graph;

public class FeatureNodewise {
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof FeatureNodewise))
			return false;
		FeatureNodewise other = (FeatureNodewise) obj;
		
		if (this.inDeg != other.inDeg)
			return false;
		if (this.sizeSCC != other.sizeSCC)
			return false;
		
		return true;
	}
	
	/* in-degree */
	int inDeg = 0;
	public int getInDegree() { return this.inDeg; }	
	
	/* size of the SCC located */
	int sizeSCC = 0;
	public int getSizeSCC() { return this.sizeSCC; }
	
}
