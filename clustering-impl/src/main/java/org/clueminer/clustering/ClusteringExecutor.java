package org.clueminer.clustering;

import java.util.prefs.Preferences;
import org.clueminer.clustering.api.Cluster;
import org.clueminer.clustering.api.Clustering;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.distance.api.DistanceMeasure;
import org.clueminer.math.Matrix;
import org.clueminer.std.Scaler;

/**
 * Executor should be responsible of converting dataset into appropriate input
 * (e.g. a dense matrix) and then joining the original inputs with appropriate
 * clustering result
 *
 * @author Tomas Barton
 */
public class ClusteringExecutor {

    public Clustering<Cluster> clusterRows(Dataset<? extends Instance> dataset, DistanceMeasure dm, Preferences params) {

        if (dataset == null || dataset.isEmpty()) {
            throw new NullPointerException("no data to process");
        }

        Matrix input = Scaler.standartize(dataset.arrayCopy(), params.get("std", null), params.getBoolean("log-scale", false));


        return null;
    }
}
