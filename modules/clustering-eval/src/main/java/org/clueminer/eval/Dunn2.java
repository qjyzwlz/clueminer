/*
 * Copyright (C) 2011-2015 clueminer.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.clueminer.eval;

import org.clueminer.clustering.api.Cluster;
import org.clueminer.clustering.api.ClusterLinkage;
import org.clueminer.clustering.api.Clustering;
import org.clueminer.clustering.api.factory.LinkageFactory;
import org.clueminer.dataset.api.Instance;
import org.clueminer.distance.EuclideanDistance;
import org.clueminer.utils.Props;

/**
 *
 * @author deric
 */
public class Dunn2 extends DunnIndex {

    private static final String name = "Dunn2";
    private static final long serialVersionUID = 8016326307215384730L;

    public Dunn2() {
        dm = EuclideanDistance.getInstance();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double score(Clustering<? extends Cluster> clusters, Props params) {
        int k = clusters.size();
        if (k < 2) {
            //doesn't make much sense to compute index for one cluster
            return Double.NaN;
        }

        double maxIntraClusterdist = Double.MIN_VALUE, temp;
        double minClusterDistance = Double.MAX_VALUE;
        Cluster<? extends Instance> clusterX, clusterY;
        ClusterLinkage link = LinkageFactory.getInstance().getProvider("Single Linkage");
        link.setDistanceMeasure(dm);

        for (int i = 0; i < clusters.size(); i++) {
            clusterX = clusters.get(i);
            //find maximal distance in between each cluster
            temp = maxIntraClusterDistance(clusterX);
            if (temp > maxIntraClusterdist) {
                maxIntraClusterdist = temp;
            }
            //@todo verify implementation
            for (int j = i + 1; j < clusters.size(); j++) {
                clusterY = clusters.get(j);
                /*
                 * finding minimal distance between objects in both clusters
                 * corresponds to single linkage distance
                 */
                temp = link.distance(clusterX, clusterY);
                if (temp < minClusterDistance) {
                    minClusterDistance = temp;
                }
            }

        }
        return minClusterDistance / maxIntraClusterdist;
    }

}
