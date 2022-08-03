package jbse.val;

import jbse.bc.ClassFile;
import jbse.common.exc.InvalidInputException;
import jbse.common.exc.UnexpectedInternalException;
import jbse.mem.Klass;
import jbse.mem.Objekt;
import jbse.val.exc.InvalidTypeException;

/**
 * A SymbolFactory creates {@link Symbolic} values for all the possible
 * origin sources of symbols.
 * 
 * @author Pietro Braione
 */
public final class SymbolFactory implements Cloneable {
    /** The next available identifier for a new reference-typed symbolic value. */
    private int nextIdRefSym;

    /** The next available identifier for a new primitive-typed symbolic value. */
    private int nextIdPrimSym;

    public SymbolFactory() {
        this.nextIdRefSym = 0;
        this.nextIdPrimSym = 0;
    }

    /**
     * A Factory Method for creating primitive symbolic values. 
     * The symbol has as origin a local variable in the current frame.
     * 
     * @param historyPoint the {@link HistoryPoint} of the symbol.
     * @param staticType a {@link String}, the static type of the
     *        local variable from which the symbol originates.
     * @param variableName a {@link String}, the name of the local 
     *        variable in the root frame the symbol originates from.
     * @return a {@link PrimitiveSymbolicLocalVariable}.
     * @throws InvalidTypeException if {@code staticType} is not a primitive type.
     * @throws InvalidInputException if {@code variableName == null || staticType == null || historyPoint == null}.
     */
    public PrimitiveSymbolicLocalVariable createSymbolLocalVariablePrimitive(HistoryPoint historyPoint, String staticType, String variableName) throws InvalidTypeException, InvalidInputException {
        return new PrimitiveSymbolicLocalVariable(variableName, getNextIdPrimitiveSymbolic(), staticType.charAt(0), historyPoint);
    }

    /**
     * A Factory Method for creating reference symbolic values. 
     * The symbol has as origin a local variable in the current frame.
     * 
     * @param historyPoint the {@link HistoryPoint} of the symbol.
     * @param staticType a {@link String}, the static type of the
     *        local variable from which the symbol originates.
     * @param genericSignatureType a {@link String}, the generic signature 
     *        type of the local variable from which the symbol originates.
     * @param variableName a {@link String}, the name of the local 
     *        variable in the root frame the symbol originates from.
     * @return a {@link PrimitiveSymbolic} or a {@link ReferenceSymbolic}
     *         according to {@code staticType}.
     * @throws InvalidTypeException if {@code staticType} is not a reference type.
     * @throws InvalidInputException if {@code variableName == null || staticType == null || historyPoint == null}.
     */
    public ReferenceSymbolicLocalVariable createSymbolLocalVariableReference(HistoryPoint historyPoint, String staticType, String genericSignatureType, String variableName) throws InvalidTypeException, InvalidInputException {
    	return new ReferenceSymbolicLocalVariable(variableName, getNextIdReferenceSymbolic(), staticType, genericSignatureType, historyPoint);
    }

    /**
     * A Factory Method for creating symbolic values. The symbol
     * is a (pseudo)reference to a {@link Klass}.
     * 
     * @param historyPoint the current {@link HistoryPoint}.
     * @param classFile the {@link ClassFile} for the {@link Klass} to be referred.
     * @return a {@link KlassPseudoReference}.
     * @throws InvalidInputException if {@code historyPoint == null || classFile == null}.
     */
    public KlassPseudoReference createSymbolKlassPseudoReference(HistoryPoint historyPoint, ClassFile classFile) throws InvalidInputException {
        final KlassPseudoReference retVal = new KlassPseudoReference(classFile, historyPoint);
        return retVal;
    }

    /**
     * A Factory Method for creating primitive symbolic values. 
     * The symbol has as origin a field in an object (not an array). 
     * 
     * @param staticType a {@link String}, the static type of the
     *        local variable from which the symbol originates.
     * @param container a {@link ReferenceSymbolic}, the container object
     *        the symbol originates from. It must not refer an array.
     * @param fieldName a {@link String}, the name of the field in the 
     *        container object the symbol originates from. It must not be {@code null}.
     * @param fieldClass a {@link String}, the name of the class where the 
     *        field is declared. It must not be {@code null}.
     * @return a {@link PrimitiveSymbolicMemberField}.
     * @throws InvalidTypeException if {@code staticType} is not a primitive type.
     * @throws InvalidInputException if {@code fieldName == null || staticType == null}.
     * @throws NullPointerException if {@code container == null}.
     */
    public PrimitiveSymbolicMemberField createSymbolMemberFieldPrimitive(String staticType, ReferenceSymbolic container, String fieldName, String fieldClass)
    throws InvalidTypeException, InvalidInputException {
    	return new PrimitiveSymbolicMemberField(container, fieldName, fieldClass, getNextIdPrimitiveSymbolic(), staticType.charAt(0));
    }

    /**
     * A Factory Method for creating reference symbolic values. 
     * The symbol has as origin a field in an object (not an array). 
     * 
     * @param staticType a {@link String}, the static type of the
     *        local variable from which the symbol originates.
     * @param genericSignatureType a {@link String}, the generic signature 
     *        type of the local variable from which the symbol originates.
     * @param container a {@link ReferenceSymbolic}, the container object
     *        the symbol originates from. It must not refer an array.
     * @param fieldName a {@link String}, the name of the field in the 
     *        container object the symbol originates from. It must not be {@code null}.
     * @param fieldClass a {@link String}, the name of the class where the 
     *        field is declared. It must not be {@code null}.
     * @return a {@link PrimitiveSymbolic} or a {@link ReferenceSymbolic}
     *         according to {@code staticType}.
     * @throws InvalidTypeException if {@code staticType} is not a reference type.
     * @throws InvalidInputException if {@code fieldName == null || staticType == null || genericSignatureType == null}.
     * @throws NullPointerException if {@code container == null}.
     */
    public ReferenceSymbolicMemberField createSymbolMemberFieldReference(String staticType, String genericSignatureType, ReferenceSymbolic container, String fieldName, String fieldClass)
    throws InvalidTypeException, InvalidInputException {
    	return new ReferenceSymbolicMemberField(container, fieldName, fieldClass, getNextIdReferenceSymbolic(), staticType, genericSignatureType);
    }

    /**
     * A Factory Method for creating primitive symbolic values. 
     * The symbol has as origin a slot in an array.  
     * 
     * @param staticType a {@link String}, the static type of the
     *        local variable from which the symbol originates.
     * @param container a {@link ReferenceSymbolic}, the container object
     *        the symbol originates from. It must refer an array.
     * @param index a {@link Primitive}, the index of the slot in the 
     *        container array this symbol originates from.
     * @return a {@link PrimitiveSymbolicMemberArray}.
     * @throws InvalidTypeException if {@code staticType} is not a valid type.
     * @throws InvalidInputException if {@code index == null || staticType == null}.
     * @throws NullPointerException if {@code container == null}.
     */
    public PrimitiveSymbolicMemberArray createSymbolMemberArrayPrimitive(String staticType, ReferenceSymbolic container, Primitive index) throws InvalidTypeException, InvalidInputException {
        return new PrimitiveSymbolicMemberArray(container, index, getNextIdPrimitiveSymbolic(), staticType.charAt(0));
    }

    /**
     * A Factory Method for creating reference symbolic values. 
     * The symbol has as origin a slot in an array.  
     * 
     * @param staticType a {@link String}, the static type of the
     *        local variable from which the symbol originates.
     * @param genericSignatureType a {@link String}, the generic signature 
     *        type of the local variable from which the symbol originates.
     * @param container a {@link ReferenceSymbolic}, the container object
     *        the symbol originates from. It must refer an array.
     * @param index a {@link Primitive}, the index of the slot in the 
     *        container array this symbol originates from.
     * @return a {@link ReferenceSymbolicMemberArray}.
     * @throws InvalidTypeException if {@code staticType} is not a valid type.
     * @throws InvalidInputException if {@code index == null || staticType == null || genericSignatureType == null}.
     * @throws NullPointerException if {@code container == null}.
     */
    public ReferenceSymbolicMemberArray createSymbolMemberArrayReference(String staticType, String genericSignatureType, ReferenceSymbolic container, Primitive index) throws InvalidTypeException, InvalidInputException {
        return new ReferenceSymbolicMemberArray(container, index, getNextIdReferenceSymbolic(), staticType, genericSignatureType);
    }

    /**
     * A Factory Method for creating symbolic values. The symbol
     * has as origin the length of an array.  
     * 
     * @param container a {@link ReferenceSymbolic}, the container object
     *        the symbol originates from. It must refer an array.
     * @return a {@link PrimitiveSymbolic}.
     * @throws NullPointerException if {@code container == null}.
     */
    public PrimitiveSymbolicMemberArrayLength createSymbolMemberArrayLength(ReferenceSymbolic container) {
        try {
            final PrimitiveSymbolicMemberArrayLength retVal = new PrimitiveSymbolicMemberArrayLength(container, getNextIdPrimitiveSymbolic());
            return retVal;
        } catch (InvalidInputException | InvalidTypeException e) {
            //this should never happen
            throw new UnexpectedInternalException(e);
        }
    }

    /**
     * A Factory Method for creating symbolic values. The symbol
     * has as origin the key slot of an entry in a map associated
     * to a given value.  
     * 
     * @param container a {@link ReferenceSymbolic}, the container object
     *        the symbol originates from. It must refer an initial map.
     * @param value a {@link Reference}, the value of the entry in the 
     *        container this symbol originates from. It can be {@code null}, 
     *        in such case it can be set later with the 
     *        {@link ReferenceSymbolicMemberMapKey#setAssociatedValue(Reference) setAssociatedValue}
     *        method.
     * @param valueHistoryPoint the {@link HistoryPoint} of {@code value} 
     *        (to identify its state). If {@code null}, the history point
     *        is assumed to be that of {@code container}.
     * @return a {@link ReferenceSymbolicMemberMapKey}.
     * @throws InvalidInputException if {@code container == null}.
     */
    public ReferenceSymbolicMemberMapKey createSymbolMemberMapKey(ReferenceSymbolic container, Reference value, HistoryPoint valueHistoryPoint) 
    throws InvalidInputException {
    	try {
    		final ReferenceSymbolicMemberMapKey retVal = new ReferenceSymbolicMemberMapKey(container, value, valueHistoryPoint, getNextIdReferenceSymbolic());
    		return retVal;
    	} catch (NullPointerException e) {
    		throw new InvalidInputException("Invoked SymbolFactory.createSymbolMemberMapKey with null container parameter");
    	} catch (InvalidInputException | InvalidTypeException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
    	}
    }

    /**
     * A Factory Method for creating symbolic values. The symbol
     * has as origin the value slot of an entry in a map associated
     * to a given key.  
     * 
     * @param container a {@link ReferenceSymbolic}, the container object
     *        the symbol originates from. It must refer an initial map.
     * @param key a {@link Reference}, the key of the entry in the 
     *        container map this symbol originates from. It must not be {@code null}.
     * @param keyHistoryPoint the {@link HistoryPoint} of {@code key} (to identify its 
     *        state). If {@code null}, the history point
     *        is assumed to be that of {@code container}.
     * @return a {@link ReferenceSymbolicMemberMapValue}.
     * @throws InvalidInputException if {@code container == null || key == null}.
     */
    public ReferenceSymbolicMemberMapValue createSymbolMemberMapValue(ReferenceSymbolic container, Reference key, HistoryPoint keyHistoryPoint) 
    throws InvalidInputException {
    	try {
    		final ReferenceSymbolicMemberMapValue retVal = new ReferenceSymbolicMemberMapValue(container, key, keyHistoryPoint, getNextIdReferenceSymbolic());
    		return retVal;
    	} catch (NullPointerException e) {
    		throw new InvalidInputException("Invoked SymbolFactory.createSymbolMemberMapValue with null container parameter");
    	} catch (InvalidTypeException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
    	}
    }

    /**
     * A Factory Method for creating symbolic values. The symbol
     * has as origin the identity hash code of a symbolic object.  
     * 
     * @param object a symbolic  {@link Objekt}, the object whose identity hash 
     *        code is this symbol. It must refer an instance or an array.
     * @return a {@link PrimitiveSymbolic}.
     * @throws InvalidInputException if {@code object == null}, or {@code object} has
     *         both its origin and its history point set to {@code null} (note that in 
     *         such case {@code object} is ill-formed).
     */
    public PrimitiveSymbolic createSymbolIdentityHashCode(Objekt object) throws InvalidInputException {
        if (object == null) {
            throw new InvalidInputException("Attempted the creation of an identity hash code by invoking " + this.getClass().getName() + ".createSymbolIdentityHashCode with null object.");
        }
        try {
            final PrimitiveSymbolicHashCode retVal = new PrimitiveSymbolicHashCode(object.getOrigin(), this.getNextIdPrimitiveSymbolic(), object.historyPoint());
            return retVal;
        } catch (InvalidTypeException e) {
            //this should never happen
            throw new UnexpectedInternalException(e);
        }
    }

    private int getNextIdPrimitiveSymbolic() {
        final int retVal = this.nextIdPrimSym++;
        return retVal;
    }

    private int getNextIdReferenceSymbolic() {
        final int retVal = this.nextIdRefSym++;
        return retVal;
    }

    @Override
    public SymbolFactory clone() {
        final SymbolFactory o;
        try {
            o = (SymbolFactory) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnexpectedInternalException(e);
        }
        return o;
    }
}
