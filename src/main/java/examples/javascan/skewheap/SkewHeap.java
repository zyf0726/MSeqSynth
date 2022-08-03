package examples.javascan.skewheap;

import java.util.*;

public class SkewHeap {
	
	private class SkewNode {
		private int element;
		private SkewNode left, right;

		private SkewNode(int val) {
			this.element = val;
			this.left = null;
			this.right = null;
		}
	}

	private SkewNode root;
	private int size;

	private SkewHeap() {
		root = null;
		size = 0;
	}
	
	public static SkewHeap __new__() {
		return new SkewHeap();
	}

	public boolean isEmpty() {
		return root == null;
	}

	public void clear() {
		root = null;
		size = 0;
	}

	public int getSize() {
		return size;
	}

	public void insert(int val) {
		root = merge(root, new SkewNode(val));
		size++;
	}

	public void remove() {
		if (root == null)
			throw new NoSuchElementException();
		root = merge(root.left, root.right);
		size--;
	}

	private SkewNode merge(SkewNode x, SkewNode y) {
		if (x == null)
			return y;
		if (y == null)
			return x;

		if (x.element < y.element) {
			SkewNode temp = x.left;
			x.left = merge(x.right, y);
			x.right = temp;
			return x;
		} else {
			SkewNode temp = y.right;
			y.right = merge(y.left, x);
			y.left = temp;
			return y;
		}
	}

/*
	
	public void displayHeap() {
		System.out.print("\nHeap : ");
		displayHeap(root);
		System.out.println("\n");
	}

	private void displayHeap(SkewNode r) {
		if (r != null) {
			displayHeap(r.left);
			System.out.print(r.element + " ");
			displayHeap(r.right);
		}
	}
	
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		System.out.println("Skew Heap Test\n\n");

		SkewHeap sh = new SkewHeap();

		char ch;

		do {
			System.out.println("\nSkewHeap Operations\n");
			System.out.println("1. insert ");
			System.out.println("2. delete ");
			System.out.println("3. size");
			System.out.println("4. check empty");
			System.out.println("5. clear");

			int choice = scan.nextInt();
			switch (choice) {
			case 1:
				System.out.println("Enter integer element to insert");
				sh.insert(scan.nextInt());
				break;
			case 2:
				sh.remove();
				break;
			case 3:
				System.out.println("Size = " + sh.getSize());
				break;
			case 4:
				System.out.println("Empty status = " + sh.isEmpty());
				break;
			case 5:
				sh.clear();
				System.out.println("Heap Cleared\n");
				break;
			default:
				System.out.println("Wrong Entry \n ");
				break;
			}

			sh.displayHeap();
			System.out.println("\nDo you want to continue (Type y or n) \n");
			ch = scan.next().charAt(0);
		} while (ch == 'Y' || ch == 'y');
	}
*/

}
