package org.clueminer.hclust;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import org.clueminer.clustering.api.dendrogram.DendroNode;

/**
 *
 * @author Tomas Barton
 */
public class DTreeNode implements DendroNode {

    private boolean root = false;
    protected DendroNode left;
    protected DendroNode right;
    protected DendroNode parent;
    private double height;
    private double position;
    private int level = -1;
    private int id;
    private int label;
    private double min;

    public DTreeNode() {
    }

    public DTreeNode(int id) {
        this.id = id;
    }

    public DTreeNode(DendroNode parent) {
        this.parent = parent;
    }

    public DTreeNode(boolean root) {
        this.root = root;
    }

    /**
     * Leaf doesn't have any children
     *
     * @return
     */
    @Override
    public boolean isLeaf() {
        return !hasLeft() && !hasRight();
    }

    @Override
    public boolean isRoot() {
        return root;
    }

    @Override
    public DendroNode getLeft() {
        return left;
    }

    @Override
    public boolean hasLeft() {
        return left != null;
    }

    @Override
    public DendroNode getRight() {
        return right;
    }

    @Override
    public boolean hasRight() {
        return right != null;
    }

    @Override
    public void setLeft(DendroNode left) {
        this.left = left;
        if (left != null) {
            left.setParent(this);
        }
    }

    @Override
    public void setRight(DendroNode right) {
        this.right = right;
        if (right != null) {
            right.setParent(this);
        }
    }

    @Override
    public int level() {
        if (level == -1) {
            if (hasLeft() && hasRight()) {
                return 1 + Math.max(getLeft().level(), getRight().level());
            } else if (hasLeft() && !hasRight()) {
                return 1 + getLeft().level();
            } else if (!hasLeft() && hasRight()) {
                return 1 + getRight().level();
            } else {
                return 0;
            }
        } else {
            return level;
        }
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public DendroNode getParent() {
        return parent;
    }

    @Override
    public void setParent(DendroNode parent) {
        this.parent = parent;
    }

    @Override
    public int childCnt() {
        int cnt = 0;
        if (hasLeft()) {
            cnt += 1 + getLeft().childCnt();
        }
        if (hasRight()) {
            cnt += 1 + getRight().childCnt();
        }
        return cnt;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setHeight(double height) {
        this.height = height;
    }

    @Override
    public double getPosition() {
        return position;
    }

    @Override
    public void setPosition(double position) {
        this.position = position;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append("#").append(getId()).append(", ")
                .append(String.format("%.2f", getHeight()))
                .append(", ").append(level).append(" ]");
        return sb.toString();
    }

    protected void printNodeValue(OutputStreamWriter out) throws IOException {
        out.write("#" + getId() + " (" + String.format("%.2f", getHeight()) + ")");
        out.write('\n');
    }

    // use string and not stringbuffer on purpose as we need to change the indent at each recursion
    @Override
    public void printTree(OutputStreamWriter out, boolean isRight, String indent) throws IOException {
        if (left != null) {
            left.printTree(out, false, indent + (isRight ? " |      " : "        "));
        }

        out.write(indent);
        if (isRight) {
            out.write(" \\");
        } else {
            out.write(" /");
        }
        out.write("----- ");
        printNodeValue(out);
        if (right != null) {
            right.printTree(out, true, indent + (isRight ? "        " : " |      "));
        }
    }

    /**
     * Valid only for leaves
     *
     * @return
     */
    @Override
    public int getIndex() {
        return -1;
    }

    @Override
    public int getLabel() {
        return label;
    }

    @Override
    public void setLabel(int label) {
        this.label = label;
    }

    @Override
    public void printCanonicalTree(OutputStreamWriter out, boolean isRight, String indent) throws IOException {
        if (left != null) {
            left.printCanonicalTree(out, false, indent + (isRight ? " |      " : "        "));
        }

        out.write(indent);
        if (isRight) {
            out.write(" \\");
        } else {
            out.write(" /");
        }
        out.write("----- ");
        printCanonicalValue(out);
        if (right != null) {
            right.printCanonicalTree(out, true, indent + (isRight ? "        " : " |      "));
        }
    }

    protected String printBinary(int number, int padding) {
        String binString = Integer.toBinaryString(number);
        if (padding > 0) {
            int length = padding - binString.length();
            char[] padArray = new char[length];
            Arrays.fill(padArray, '0');
            String buff = new String(padArray);
            return buff + binString;
        }
        return Integer.toBinaryString(number);
    }

    protected void printCanonicalValue(OutputStreamWriter out) throws IOException {
        //System.out.println(label + " x " + level);
        out.write(printBinary(label, level));
        out.write('\n');
    }

    /**
     * {@inheritDoc}
     *
     * @param min
     */
    @Override
    public void setMin(double min) {
        this.min = min;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public double getMin() {
        return min;
    }
}
