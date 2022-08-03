package dsclasses.kiasan.leftist;

import java.util.LinkedList;

import dsclasses.kiasan.common.Underflow;

// LeftistHeap class
//
// CONSTRUCTION: with a negative infinity sentinel
//
// ******************PUBLIC OPERATIONS*********************
// void insert( x )       --> Insert x
// Comparable deleteMin( )--> Return and remove smallest item
// Comparable findMin( )  --> Return smallest item
// boolean isEmpty( )     --> Return true if empty; else false
// void makeEmpty( )      --> Remove all items
// void merge( rhs )      --> Absorb rhs into this heap
// ******************ERRORS********************************
// Throws UnderflowException as appropriate

/**
 * Implements a leftist heap. Note that all "matching" is based on the compareTo
 * method.
 * 
 * @author Mark Allen Weiss
 */
public class LeftistHeap {
  static class LeftistNode {
    int element; // The data in the node

    LeftistNode left; // Left child

    LeftistNode right; // Right child
    int npl; // null path length

    // Constructors
    LeftistNode(final int theElement) {
      this(theElement, null, null);
    }

    LeftistNode(final int theElement, final LeftistNode lt, final LeftistNode rt) {
      this.element = theElement;
      this.left = lt;
      this.right = rt;
      this.npl = 0;
    }
  }

  public static void main(final String[] args) {
    final int numItems = 100;
    final LeftistHeap h = new LeftistHeap();
    final LeftistHeap h1 = new LeftistHeap();
    int i = 37;

    for (i = 37; i != 0; i = (i + 37) % numItems) {
      if (i % 2 == 0) {
        h1.insert(i);
      } else {
        h.insert(i);
      }
    }
    assert h.wellFormed();
    h.merge(h1);
    assert h.wellFormed();
    for (i = 1; i < numItems; i++) {
      if (h.deleteMin() != i) {
        System.out.println("Oops! " + i);
      }
    }
  }

  /**
   * Swaps t's two children.
   */
  private static void swapChildren(final LeftistNode t) {
    final LeftistNode tmp = t.left;
    t.left = t.right;
    t.right = tmp;
  }

  private LeftistNode root; // root

  /**
   * Construct the leftist heap.
   */
  private LeftistHeap() {
    this.root = null;
  }
  
  public static LeftistHeap __new__() {
	  return new LeftistHeap();
  }

  /**
   * Remove the smallest item from the priority queue.
   * 
   * @return the smallest item, or throw UnderflowException if empty.
   */
  //@ requires this.wellFormed();
  //@ ensures this.wellFormed();
  public int deleteMin() {
    if (isEmpty()) {
      throw new Underflow();
    }

    final int minItem = this.root.element;
    this.root = merge(this.root.left, this.root.right);

    return minItem;
  }

  /**
   * Find the smallest item in the priority queue.
   * 
   * @return the smallest item, or throw UnderflowException if empty.
   */
  //@ requires this.wellFormed();
  //@ ensures this.wellFormed();
  public int findMin() {
    if (isEmpty()) {
      throw new Underflow();
    }
    return this.root.element;
  }

  /**
   * Insert into the priority queue, maintaining heap order.
   * 
   * @param x
   *          the item to insert.
   */
  //@ requires this.wellFormed();
  //@ ensures (this.root != null) && this.wellFormed();
  public void insert(final int x) {
    this.root = merge(new LeftistNode(x), this.root);
  }

  private boolean isAcyclic(final LeftistNode start,
      final LinkedList<LeftistNode> seen) {
    if (start.left != null) {
      if (seen.contains(start.left)) {
        return false;
      }
      seen.addLast(start.left);
      if (!isAcyclic(start.left, seen)) {
        return false;
      }
    }
    if (start.right != null) {
      if (seen.contains(start.right)) {
        return false;
      }
      seen.addLast(start.right);
      if (!isAcyclic(start.right, seen)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Test if the priority queue is logically empty.
   * 
   * @return true if empty, false otherwise.
   */
  public boolean isEmpty() {
    return this.root == null;
  }

  private boolean isLeftist(final LeftistNode ln) {
    if (ln == null) {
      return true;
    }
    int lnpl = -1;
    if (ln.left != null) {
      lnpl = ln.left.npl;
    }
    int rnpl = -1;
    if (ln.right != null) {
      rnpl = ln.right.npl;
    }
    if ((lnpl < rnpl) || (ln.npl != rnpl + 1)) {
      return false;
    }
    return isLeftist(ln.left) && isLeftist(ln.right);
  }

  /**
   * Make the priority queue logically empty.
   */
  public void makeEmpty() {
    this.root = null;
  }

  /**
   * Merge rhs into the priority queue. rhs becomes empty. rhs must be different
   * from this.
   * 
   * @param rhs
   *          the other leftist heap.
   */
  //@ requires this.merge_preCondition(rhs);
  //@ ensures this.wellFormed();
  public void merge(final LeftistHeap rhs) {
    if (this == rhs) {
      return;
    }

    this.root = merge(this.root, rhs.root);
    rhs.root = null;
  }

  /**
   * Internal method to merge two roots. Deals with deviant cases and calls
   * recursive merge1.
   */
  private LeftistNode merge(final LeftistNode h1, final LeftistNode h2) {
    if (h1 == null) {
      return h2;
    }
    if (h2 == null) {
      return h1;
    }
    if (h1.element < h2.element) {
      return merge1(h1, h2);
    } else {
      return merge1(h2, h1);
    }
  }

/*
  private boolean merge_preCondition(final LeftistHeap rhs) {
    if (rhs == null) {
      return false;
    }
    final LinkedList<LeftistNode> seen = new LinkedList<LeftistNode>();
    if (this.root != null) {
      seen.addLast(this.root);
      final boolean result = isAcyclic(this.root, seen) && ordered(this.root)
          && isLeftist(this.root);
      if (!result) {
        return false;
      }
    }
    if (rhs.root != null) {
      if (seen.contains(rhs.root)) {
        return false;
      }
      seen.addLast(rhs.root);
      return isAcyclic(rhs.root, seen) && ordered(rhs.root)
          && isLeftist(rhs.root);
    }
    return true;
  }
*/

  /**
   * Internal method to merge two roots. Assumes trees are not empty, and h1's
   * root contains smallest item.
   */
  private LeftistNode merge1(final LeftistNode h1, final LeftistNode h2) {
    if (h1.left == null) {
      h1.left = h2; // Other fields in h1 already accurate
    } else {
      h1.right = merge(h1.right, h2);
      if (h1.left.npl < h1.right.npl) {
        LeftistHeap.swapChildren(h1);
      }
      h1.npl = h1.right.npl + 1;
    }
    return h1;
  }

  private boolean ordered(final LeftistNode ln) {
    if (ln == null) {
      return true;
    }
    if ((ln.left != null) && (ln.element > ln.left.element)) {
      return false;
    }
    if ((ln.right != null) && (ln.element > ln.right.element)) {
      return false;
    }
    return ordered(ln.left) && ordered(ln.right);
  }

  private boolean wellFormed() {
    if (this.root == null) {
      return true;
    }
    final LinkedList<LeftistNode> seen = new LinkedList<LeftistNode>();
    seen.addLast(this.root);
    return isAcyclic(this.root, seen) && ordered(this.root)
        && isLeftist(this.root);
  }
}
