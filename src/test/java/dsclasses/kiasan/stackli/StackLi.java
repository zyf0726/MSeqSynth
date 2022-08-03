package dsclasses.kiasan.stackli;

import dsclasses.kiasan.common.Underflow;

// StackLi class
//
// CONSTRUCTION: with no initializer
//
// ******************PUBLIC OPERATIONS*********************
// void push( x )         --> Insert x
// void pop( )            --> Remove most recently inserted item
// Object top( )          --> Return most recently inserted item
// Object topAndPop( )    --> Return and remove most recent item
// boolean isEmpty( )     --> Return true if empty; else false
// boolean isFull( )      --> Always returns false
// void makeEmpty( )      --> Remove all items
// ******************ERRORS********************************
// pop on empty stack

/**
 * List-based implementation of the stack.
 * 
 * @author Mark Allen Weiss
 */
public class StackLi {
  public static void main(final String[] args) {
    final StackLi s = new StackLi();

    for (int i = 0; i < 10; i++) {
      s.push(new Integer(i));
    }

    while (!s.isEmpty()) {
      System.out.println(s.topAndPop());
    }
  }

  private ListNode topOfStack;

  /**
   * Construct the stack.
   */
  private StackLi() {
    this.topOfStack = null;
  }
  public static StackLi __new__() {
	  return new StackLi();
  }

/*
  private boolean contains(final Object e) {
    ListNode temp = this.topOfStack;
    while (temp != null) {
      if (e == temp.element) {
        return true;
      }
      temp = temp.next;
    }
    return false;
  }

  private boolean isAcyclic() {
    final StackLi ll = new StackLi();
    ListNode temp = this.topOfStack;
    while (temp != null) {
      if (ll.contains(temp)) {
        return false;
      }
      ll.topOfStack = new ListNode(temp, ll.topOfStack);
      temp = temp.next;
    }
    return true;
  }
*/

  /**
   * Test if the stack is logically empty.
   * 
   * @return true if empty, false otherwise.
   */
  public boolean isEmpty() {
    return this.topOfStack == null;
  }

  /**
   * Test if the stack is logically full.
   * 
   * @return false always, in this implementation.
   */
  public boolean isFull() {
    return false;
  }

  /**
   * Make the stack logically empty.
   */
  public void makeEmpty() {
    this.topOfStack = null;
  }

  /**
   * Remove the most recently inserted item from the stack.
   * 
   * @exception Underflow
   *              if the stack is empty.
   */
  //@ requires this.isAcyclic();
  //@ ensures this.isAcyclic();
  public void pop() throws Underflow {
    if (isEmpty()) {
      throw new Underflow();
    }
    this.topOfStack = this.topOfStack.next;
  }

  /**
   * Insert a new item into the stack.
   * 
   * @param x
   *          the item to insert.
   */
  //@ requires this.isAcyclic();
  //@ ensures this.isAcyclic() && this.topOfStack.element == x;
  public void push(final Object x) {
    this.topOfStack = new ListNode(x, this.topOfStack);
  }

  /**
   * Get the most recently inserted item in the stack. Does not alter the stack.
   * 
   * @return the most recently inserted item in the stack, or null, if empty.
   */
  public Object top() {
    if (isEmpty()) {
      return null;
    }
    return this.topOfStack.element;
  }

  /**
   * Return and remove the most recently inserted item from the stack.
   * 
   * @return the most recently inserted item in the stack, or null, if empty.
   */
  public Object topAndPop() {
    if (isEmpty()) {
      return null;
    }

    final Object topItem = this.topOfStack.element;
    this.topOfStack = this.topOfStack.next;
    return topItem;
  }
}
