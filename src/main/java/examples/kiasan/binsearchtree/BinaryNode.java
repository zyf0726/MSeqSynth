package examples.kiasan.binsearchtree;

// Basic node stored in unbalanced binary search trees
// Note that this class is not accessible outside
// of package DataStructures

class BinaryNode {
  // Friendly data; accessible by other package routines
  int element; // The data in the node

  BinaryNode left; // Left child

  BinaryNode right; // Right child

  BinaryNode() {
    this(-1);
  }

  // Constructors
  BinaryNode(final int theElement) {
    this(theElement, null, null);
  }

  BinaryNode(final int theElement, final BinaryNode lt, final BinaryNode rt) {
    this.element = theElement;
    this.left = lt;
    this.right = rt;
  }
}
