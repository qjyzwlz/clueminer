package org.clueminer.clustering.aggl;

import java.util.AbstractQueue;
import java.util.PriorityQueue;
import org.clueminer.clustering.algorithm.HClustResult;
import org.clueminer.clustering.api.AgglParams;
import org.clueminer.clustering.api.HierarchicalResult;
import org.clueminer.clustering.api.dendrogram.DendroTreeData;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.math.Matrix;
import org.clueminer.utils.PropType;
import org.clueminer.utils.Props;

/**
 *
 * @author Tomas Barton
 */
public class HacLwMsPar extends HACLWMS {

    private final static String name = "HAC-LW-MS-PAR";
    private int threads = 4;

    public HacLwMsPar() {

    }

    public HacLwMsPar(int numThreads) {
        this.threads = numThreads;
    }

    @Override
    public String getName() {
        return name + "-" + threads;
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
        AgglParams params = new AgglParams(pref);
        Matrix similarityMatrix;
        distanceFunction = params.getDistanceMeasure();
        if (params.clusterRows()) {
            n = dataset.size();
        } else {
            //columns clustering
            n = dataset.attributeCount();
        }

        int items = triangleSize(n);
        //TODO: we might track clustering by estimated time (instead of counters)
        AbstractQueue<Element> pq = new PriorityQueue<>(items);

        Matrix input = dataset.asMatrix();
        if (params.clusterRows()) {
            if (distanceFunction.isSymmetric()) {
                similarityMatrix = AgglClustering.rowSimilarityMatrixParSym(input, distanceFunction, pq, threads);
            } else {
                similarityMatrix = AgglClustering.rowSimilarityMatrix(input, distanceFunction, pq);
            }
        } else {
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

}
