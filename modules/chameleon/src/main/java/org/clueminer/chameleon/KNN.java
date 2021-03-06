package org.clueminer.chameleon;

import java.util.ArrayList;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.distance.EuclideanDistance;
import org.clueminer.distance.api.DistanceMeasure;
import org.clueminer.graph.api.Graph;
import org.clueminer.graph.api.GraphFactory;
import org.clueminer.graph.api.Node;

/**
 *
 * @author Tomas Bruna
 */
public class KNN {

    /**
     * Number of neighbours for each item
     */
    private int k;

    /**
     * storage for neighbours of each node
     */
    int[][] nearests;

    Dataset<? extends Instance> input;

    private DistanceMeasure dm;

    public KNN() {
        this(3);
    }

    public KNN(int k) {
        this(k, new EuclideanDistance());
    }

    public KNN(int k, DistanceMeasure dm) {
        this.k = k;
        this.dm = dm;
    }

    /**
     * Find k neighbours of all items in the dataset
     *
     * @param dataset input dataset
     * @return
     */
    private int[][] findNeighbors(Dataset<? extends Instance> dataset) {
        input = dataset;
        if (k >= input.size()) {
            throw new RuntimeException("Too many neighbours, not enough nodes in dataset");
        }
        nearests = new int[input.size()][k];
        for (int i = 0; i < input.size(); i++) {
            //put first k neighbours into array and sort them
            int firsts = k;
            int index = 0;
            for (int j = 0; j < firsts; j++) {
                //skip self as neighbour
                if (i == j) {
                    firsts++;
                    continue;
                }
                nearests[i][index] = j;
                insert(index, i);
                index++;
            }
            double maxMinimalDistance = dm.measure(input.instance(i), input.instance(nearests[i][k - 1]));
            //neighbour array full, find closer neighbours from the rest of the dataset
            for (int j = firsts; j < input.size(); j++) {
                //skip self as neighbour
                if (i == j) {
                    continue;
                }
                //if distance to central node is smaller then of the furthest current neighbour, add this node to neighbours
                if (dm.measure(input.instance(i), input.instance(j)) < maxMinimalDistance) {
                    nearests[i][k - 1] = j;
                    insert(k - 1, i);
                    maxMinimalDistance = dm.measure(input.instance(i), input.instance(nearests[i][k - 1]));
                }
            }
        }
        return nearests;
    }

    /**
     * Sort neighbours in ascending order by distance to central node
     *
     * @param pos Position of the last element in array with neighbours
     * @param i Number of central cluster to which neighbours are assigned
     */
    private void insert(int pos, int i) {
        while (pos > 0 && dm.measure(input.instance(i), input.instance(nearests[i][pos]))
                < dm.measure(input.instance(i), input.instance(nearests[i][pos - 1]))) {
            int temp = nearests[i][pos];
            nearests[i][pos] = nearests[i][pos - 1];
            nearests[i][pos - 1] = temp;
            pos--;
        }
    }

    public int[][] getNeighborArray(Dataset<? extends Instance> dataset) {
        return findNeighbors(dataset);
    }

    /**
     * Create graph where connected nodes are neighbors
     *
     * @param dataset input dataset
     * @param g graph where output is be stored
     * @return neighbor graph
     */
    public Graph getNeighborGraph(Dataset<? extends Instance> dataset, Graph g) {
        findNeighbors(dataset);
        GraphFactory f = g.getFactory();
        ArrayList<Node> nodes = f.createNodesFromInput(dataset);
        g.addAllNodes(nodes);
        g.addEdgesFromNeigborArray(nearests, k);
        return g;
    }

}
