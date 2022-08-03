package dsclasses;

import java.lang.reflect.Method;

import dsclasses.ListNode;
import mseqsynth.heap.ClassH;
import mseqsynth.heap.FieldH;

public class ListNode {
	
	public static ClassH classH;
	public static FieldH fNext, fElem;
	public static Method mNew, mGetNext, mGetElem, mSetElem, mAddAfter, mAddBefore; 
	
	static {
		try {
			classH = ClassH.of(ListNode.class);
			fNext = FieldH.of(ListNode.class.getDeclaredField("next"));
			fElem = FieldH.of(ListNode.class.getDeclaredField("elem"));
			mNew = ListNode.class.getMethod("__new__", int.class);
			mGetNext = ListNode.class.getMethod("getNext", boolean.class);
			mGetElem = ListNode.class.getMethod("getElem");
			mSetElem = ListNode.class.getMethod("setElem", int.class);
			mAddAfter = ListNode.class.getMethod("addAfter", int.class);
			mAddBefore = ListNode.class.getMethod("addBefore", int.class);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private ListNode next;
	private int elem;
	
	private ListNode(ListNode next, int elem) {
		this.next = next;
		this.elem = elem;
	}
	
	public static ListNode __new__(int elem) {
		ListNode node = new ListNode(null, elem);
		return node;
	}
	
	public ListNode getNext(boolean dummy) {
		return this.next;
	}
	
	public int getElem() {
		return this.elem;
	}
	
	public boolean setElem(int elem) {
		if (this.elem == elem) {
			return true;
		} else {
			this.elem = elem;
			return false;
		}
	}
	
	public void addAfter(int elem) {
		ListNode next = new ListNode(null, elem);
		this.next = next;
	}
	
	public ListNode addBefore(int elem) {
		ListNode prev = new ListNode(this, elem);
		return prev;
	}
	
}