package org.clueminer.clustering.aggl;

import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.clueminer.clustering.algorithm.HClustResult;
import org.clueminer.clustering.api.AbstractClusteringAlgorithm;
import org.clueminer.clustering.api.AgglParams;
import org.clueminer.clustering.api.AgglomerativeClustering;
import org.clueminer.clustering.api.Cluster;
import org.clueminer.clustering.api.ClusterLinkage;
import org.clueminer.clustering.api.Clustering;
import org.clueminer.clustering.api.ClusteringAlgorithm;
import org.clueminer.clustering.api.CutoffStrategy;
import org.clueminer.clustering.api.HierarchicalResult;
import org.clueminer.clustering.api.InternalEvaluator;
import org.clueminer.clustering.api.config.annotation.Param;
import org.clueminer.clustering.api.dendrogram.DendroNode;
import org.clueminer.clustering.api.dendrogram.DendroTreeData;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.hclust.DLeaf;
import org.clueminer.hclust.DTreeNode;
import org.clueminer.hclust.DynamicTreeData;
import org.clueminer.math.Matrix;
import org.clueminer.utils.PropType;
import org.clueminer.utils.Props;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * Hierarchical agglomerative clustering - base algorithm, same for all linkage
 * methods
 *
 * memory complexity:
 * <li>
 * <ul>double array (n - 1) * n / 2 - for storing similarity matrix</ul>
 * <ul>hash maps O(n^2)</ul>
 * <ul>tree structure (2 * n - 1 objects)</ul>
 * </li>
 * time complexity - omega n^2 * ( log n)
 *
 * Note: In order to avoid concurrency issues, the algorithm shouldn't keep
 * state
 *
 * @see
 * http://nlp.stanford.edu/IR-book/html/htmledition/time-complexity-of-hac-1.html
 * @author Tomas Barton
 */
@ServiceProvider(service = ClusteringAlgorithm.class)
public class HAC extends AbstractClusteringAlgorithm implements AgglomerativeClustering {

    private final static String name = "HAC";
    private static final Logger logger = Logger.getLogger(HAC.class.getName());

    @Param(name = AgglParams.LINKAGE,
           factory = "org.clueminer.clustering.api.factory.LinkageFactory",
           type = org.clueminer.clustering.params.ParamType.STRING)
    protected ClusterLinkage linkage;

    @Param(name = AgglParams.CUTOFF_STRATEGY,
           factory = "org.clueminer.clustering.api.factory.CutoffStrategyFactory",
           type = org.clueminer.clustering.params.ParamType.STRING)
    protected CutoffStrategy cutoffStrategy;

    @Param(name = AgglParams.CUTOFF_SCORE,
           factory = "org.clueminer.clustering.api.factory.InternalEvaluatorFactory",
           type = org.clueminer.clustering.params.ParamType.STRING)
    protected InternalEvaluator cutoffScore;

    @Override
    public String getName() {
        return name;
    }

    /**
     * Computes hierarchical clustering with specified linkage and stores
     * dendrogram tree structure. However final clustering is not computed yet,
     * it will be formed later based on cut-off function.
     *
     * @param dataset
     * @param pref
     * @return
     */
    @Override
    public HierarchicalResult hierarchy(Dataset<? extends Instance> dataset, Props pref) {
        int n;
        HierarchicalResult result = new HClustResult(dataset, pref);
        pref.put(AgglParams.ALG, getName());
        checkParams(pref);
        AgglParams params = new AgglParams(pref);
        Matrix similarityMatrix;
        distanceFunction = params.getDistanceMeasure();
        if (params.clusterRows()) {
            n = dataset.size();
        } else {
            //columns clustering
            n = dataset.attributeCount();
        }
        logger.log(Level.FINE, "{0} clustering: {1}", new Object[]{getName(), pref.toString()});
        int items = triangleSize(n);
        //TODO: we might track clustering by estimated time (instead of counters)
        PriorityQueue<Element> pq = new PriorityQueue<>(items);

        Matrix input = dataset.asMatrix();
        if (params.clusterRows()) {
            similarityMatrix = AgglClustering.rowSimilarityMatrix(input, distanceFunction, pq);
        } else {
            logger.log(Level.INFO, "matrix columns: {0}", input.columnsCount());
            similarityMatrix = AgglClustering.columnSimilarityMatrix(input, distanceFunction, pq);
        }
        //whether to keep reference to proximity matrix (could be memory exhausting)
        if (pref.getBoolean(PropType.PERFORMANCE, AgglParams.KEEP_PROXIMITY, true)) {
            result.setProximityMatrix(similarityMatrix);
        }

        DendroTreeData treeData = computeLinkage(pq, similarityMatrix, dataset, params, n);
        treeData.createMapping(n, treeData.getRoot());
        result.setTreeData(treeData);
        return result;
    }

    /**
     * Could be overridden by inherited method to check where algorithm is
     * capable of running with requested parameters (otherwise throw an
     * Exception)
     *
     * @param props
     */
    protected void checkParams(Props props) {

    }

    /**
     * Find most closest items and merges them into one cluster (subtree)
     *
     * @param pq               queue with sorted distances (lowest distance pops
     *                         out first)
     * @param similarityMatrix
     * @param dataset
     * @param params
     * @param n                number of items to cluster
     * @return
     */
    protected DendroTreeData computeLinkage(AbstractQueue<Element> pq, Matrix similarityMatrix, Dataset<? extends Instance> dataset, AgglParams params, int n) {
        //binary tree, we know how many nodes we have
        DendroNode[] nodes = new DendroNode[(2 * n - 1)];
        //each instance will form a cluster
        Map<Integer, Set<Integer>> assignments = initialAssignment(n, dataset, params, nodes);

        Element curr;
        HashSet<Integer> blacklist = new HashSet<>();
        HashMap<Integer, Double> cache = new HashMap<>();
        DendroNode node = null;
        Set<Integer> left, right;
        int nodeId = n;
        int ma, mb;
        /**
         * queue of distances, each time join 2 items together, we should remove
         * (n-1) items from queue (but removing is too expensive)
         */
        while (!pq.isEmpty() && assignments.size() > 1) {
            curr = pq.poll();
            //System.out.println(curr.toString() + " remain: " + pq.size() + ", height: " + String.format("%.3f", curr.getValue()));
            if (!blacklist.contains(curr.getRow()) && !blacklist.contains(curr.getColumn())) {
                node = getOrCreate(nodeId++, nodes);
                node.setLeft(nodes[curr.getRow()]);
                node.setRight(nodes[curr.getColumn()]);
                node.setHeight(curr.getValue());

                //System.out.println("node " + node.getId() + " left: " + node.getLeft() + " right: " + node.getRight());
                blacklist.add(curr.getRow());
                blacklist.add(curr.getColumn());

                //remove old clusters
                left = assignments.remove(curr.getRow());
                right = assignments.remove(curr.getColumn());
                ma = left.size();
                mb = right.size();
                //merge together and add as a new cluster
                left.addAll(right);
                updateDistances(node.getId(), left, similarityMatrix, assignments,
                        pq, params.getLinkage(), cache, curr.getRow(), curr.getColumn(), ma, mb);
                //when assignment have size == 1, all clusters are merged into one
            }
        }

        //last node is the root
        DendroTreeData treeData = new DynamicTreeData(node);
        return treeData;
    }

    /**
     * Compute size of triangular matrix (n x n) minus diagonal
     *
     * @param n
     * @return
     */
    protected int triangleSize(int n) {
        return ((n - 1) * n) >>> 1;
    }

    /**
     * Ensure obtaining node for given ID
     *
     * @param id
     * @param nodes
     * @return an inner node of the dendrogram tree
     */
    protected DendroNode getOrCreate(int id, DendroNode[] nodes) {
        if (nodes[id] == null) {
            DendroNode node = new DTreeNode(id);
            nodes[id] = node;
        }
        return nodes[id];
    }

    /**
     *
     * @param mergedId         id of newly created cluster
     * @param mergedCluster    id of all items in merged cluster
     * @param similarityMatrix matrix of distances
     * @param assignments
     * @param pq
     * @param linkage
     * @param cache
     * @param leftId           left cluster ID
     * @param rightId          right cluster ID
     * @param ma
     * @param mb
     */
    protected void updateDistances(int mergedId, Set<Integer> mergedCluster,
            Matrix similarityMatrix, Map<Integer, Set<Integer>> assignments,
            AbstractQueue<Element> pq, ClusterLinkage linkage,
            HashMap<Integer, Double> cache, int leftId, int rightId, int ma, int mb) {
        Element current;
        double distance;
        for (Map.Entry<Integer, Set<Integer>> cluster : assignments.entrySet()) {
            distance = linkage.similarity(similarityMatrix, cluster.getValue(), mergedCluster);
            current = new Element(distance, mergedId, cluster.getKey());
            pq.add(current);
        }
        //System.out.println("adding " + mergedId + " -> " + mergedCluster.toString());
        //finaly add merged cluster
        assignments.put(mergedId, mergedCluster);
    }

    /**
     * Each data point forms an individual cluster
     *
     * @param n       the number of data points
     * @param dataset
     * @param params
     * @param nodes
     * @return
     */
    protected Map<Integer, Set<Integer>> initialAssignment(int n, Dataset<? extends Instance> dataset,
            AgglParams params, DendroNode[] nodes) {
        Map<Integer, Set<Integer>> clusterAssignment = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            //cluster contain all its members (in final step, its size is equal to n)
            HashSet<Integer> cluster = new HashSet<>();
            cluster.add(i);
            clusterAssignment.put(i, cluster);
            //each cluster is also a dendrogram leaf
            if (params.clusterRows()) {
                nodes[i] = new DLeaf(i, dataset.get(i));
            } else {
                nodes[i] = new DLeaf(i, dataset.getAttribute(i));
            }

        }
        return clusterAssignment;
    }

    @Override
    public Clustering<Cluster> cluster(Dataset<? extends Instance> dataset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Clustering<Cluster> cluster(Dataset<? extends Instance> dataset, Props props) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isLinkageSupported(String linkage) {
        switch (linkage) {
            case "Ward's Linkage":
                return false;
            default:
                return true;
        }
    }

}
