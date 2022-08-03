package examples.demo;

public class Node {
    private Node next;
    private int value;
    private Node(Node n, int v) {
        this.next = n; this.value = v;
    }

    public static Node create(int val, boolean b) {
        if (b == true)
            return new Node(null, val * 2 + 1);
        else return new Node(null, val * 2);
    }

    public Node getNext() { return this.next; }
    public int getValue() { return this.value; }
    public void addAfter(int v) {
        this.next = new Node(null, v);
    }
    public Node addBefore(int v) {
        return new Node(this, v);
    }
}