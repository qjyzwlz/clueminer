package org.clueminer.eval;

import java.util.HashMap;
import org.clueminer.clustering.api.Cluster;
import org.clueminer.clustering.api.InternalEvaluator;
import org.clueminer.clustering.api.Clustering;
import org.clueminer.dataset.api.Instance;
import org.clueminer.distance.EuclideanDistance;
import org.clueminer.distance.api.DistanceMeasure;
import org.clueminer.utils.Props;
import org.openide.util.lookup.ServiceProvider;

/**
 * Davies-Bouldin index the value of the DB index between [0, infinity) zero
 * being a sign for a good cluster
 *
 * @cite Davies, David L., and Donald W. Bouldin. "A cluster separation
 * measure." Pattern Analysis and Machine Intelligence, IEEE Transactions on 2
 * (1979): 224-227.
 *
 * @author Tomas Barton
 */
@ServiceProvider(service = InternalEvaluator.class)
public class DaviesBouldin extends AbstractEvaluator {

    private static final long serialVersionUID = -6973489229802690101L;
    private static final String name = "Davies-Bouldin";

    public DaviesBouldin() {
        dm = EuclideanDistance.getInstance();
    }

    public DaviesBouldin(DistanceMeasure dist) {
        this.dm = dist;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double score(Clustering<? extends Cluster> clusters, Props params) {
        double db = 0;
        Cluster<Instance> x, y;
        double intraX, intraY, max, interGroup, dij;
        Instance centroidX, centroidY;
        HashMap<Integer, Double> intraDists = new HashMap<>();
        for (int i = 0; i < clusters.size(); i++) {
            x = clusters.get(i);
            centroidX = x.getCentroid();
            max = Double.MIN_VALUE;

            intraX = getClusterIntraDistance(i, x, intraDists);

            for (int j = i + 1; j < clusters.size(); j++) {
                y = clusters.get(j);
                centroidY = y.getCentroid();
                intraY = getClusterIntraDistance(j, y, intraDists);
                /**
                 * this is average linkage distance, also complete distance
                 * could be used - TODO check reference implementation
                 */
                interGroup = dm.measure(centroidX, centroidY);
                dij = (intraX + intraY) / interGroup;
                if (dij > max) {
                    max = dij;
                }
            }
            db += max;

        }
        db /= clusters.size();

        return db;
    }

    private double getClusterIntraDistance(int i, Cluster<Instance> x, HashMap<Integer, Double> intraDists) {
        if (!intraDists.containsKey(i)) {
            double val = intraDistance(x);
            intraDists.put(i, val);
            return val;
        }
        return intraDists.get(i);
    }

    private double intraDistance(Cluster<Instance> x) {
        double intraDist = 0.0;
        for (Instance elem : x) {
            intraDist += dm.measure(elem, x.getCentroid());
        }
        intraDist /= x.size();
        return intraDist;
    }

    /**
     * Should be minimized
     *
     * @param score1
     * @param score2
     * @return
     */
    @Override
    public boolean isBetter(double score1, double score2) {
        return score1 < score2;
    }

    @Override
    public boolean isMaximized() {
        return false;
    }

    @Override
    public double getMin() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double getMax() {
        return 0;
    }
}
