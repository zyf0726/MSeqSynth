package mseqsynth.util;

import java.io.Serializable;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;

// TODO replace this class by BiMap

public class Bijection<U, V> implements Serializable {
	
	private static final long serialVersionUID = -8116641884674969898L;
	
	
	private BiMap<U, V> bimap;
	
	public Bijection() {
		this.bimap = HashBiMap.create();
	}
	
	public Bijection(Bijection<U, V> other) {
		this.bimap = HashBiMap.create(other.bimap);
	}
	
	public boolean putUV(U keyU, V keyV) {
		Preconditions.checkNotNull(keyU);
		Preconditions.checkNotNull(keyV);
		if (this.bimap.containsKey(keyU))
			return keyV.equals(this.bimap.get(keyU));
		if (this.bimap.containsValue(keyV))
			return false;
		this.bimap.put(keyU, keyV);
		return true;
	}
	
	public V getV(U keyU) {
		return this.bimap.get(keyU);
	}
	
	public U getU(V keyV) {
		return this.bimap.inverse().get(keyV);
	}
	
	public boolean containsU(U keyU) {
		return this.bimap.containsKey(keyU);
	}
	
	public boolean containsV(V keyV) {
		return this.bimap.containsValue(keyV);
	}
	
	public Map<U, V> getMapU2V() {
		return ImmutableMap.copyOf(this.bimap);
	}
	
	public Map<V, U> getMapV2U() {
		return ImmutableMap.copyOf(this.bimap.inverse());
	}
	
	public int size() {
		return this.bimap.size();
	}
	
	public void clear() {
		this.bimap.clear();
	}

}
