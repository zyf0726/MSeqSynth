package mseqsynth.heap;

import mseqsynth.util.Bijection;

public interface ActionIfFound {
	
	boolean emitMapping(Bijection<ObjectH, ObjectH> ret);

}
