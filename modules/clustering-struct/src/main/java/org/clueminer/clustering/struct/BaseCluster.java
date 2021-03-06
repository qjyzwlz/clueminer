package org.clueminer.clustering.struct;

import com.google.common.collect.Sets;
import java.awt.Color;
import java.util.Set;
import org.clueminer.attributes.AttributeFactoryImpl;
import org.clueminer.clustering.api.Cluster;
import org.clueminer.dataset.api.AttributeBuilder;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.dataset.api.InstanceBuilder;
import org.clueminer.dataset.plugin.ArrayDataset;
import org.clueminer.dataset.plugin.DoubleArrayFactory;
import org.clueminer.stats.AttrNumStats;

/**
 *
 * @author Tomas Barton
 * @param <E>
 */
public class BaseCluster<E extends Instance> extends ArrayDataset<E> implements Cluster<E>, Set<E> {

    private static final long serialVersionUID = -6931127664256794410L;
    private int clusterId;
    private Color color;
    private E centroid;
    private final Set<Integer> mapping = Sets.newHashSet();

    public BaseCluster(int capacity) {
        super(capacity, 5);
    }

    public BaseCluster(int capacity, int attrSize) {
        super(capacity, attrSize);
    }

    public BaseCluster(Dataset<? extends Instance> dataset) {
        //some guess about future cluster size
        super((int) Math.sqrt(dataset.size()), dataset.attributeCount());
        setAttributes(dataset.getAttributes());
    }

    @Override
    public boolean add(Instance inst) {
        if (super.add(inst)) {
            mapping.add(inst.getIndex());
            centroid = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(int origId) {
        return mapping.contains(origId);
    }

    @Override
    public void setClusterId(int id) {
        this.clusterId = id;
    }

    @Override
    public int getClusterId() {
        return clusterId;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Centroid is computed from stored cluster statistics
     *
     * @return artificial instance representing center of cluster
     */
    @Override
    public E getCentroid() {
        if (centroid == null) {
            int attrCount = this.attributeCount();
            if (attrCount == 0) {
                throw new RuntimeException("number of attributes should not be 0");
            }
            double value;
            Instance avg = this.builder().build(attrCount);
            for (int i = 0; i < attrCount; i++) {
                //use pre-computed average for each attribute
                value = getAttribute(i).statistics(AttrNumStats.AVG);
                avg.set(i, value);
            }
            centroid = (E) avg;
        }

        return centroid;
    }

    /**
     * Counting is based in instance index which must be unique in dataset
     *
     * @param c
     * @return
     */
    @Override
    public int countMutualElements(Cluster c) {
        int mutual = 0;
        for (Instance inst : this) {
            //System.out.println("looking for: " + inst.getIndex() + " found? " + c.contains(inst.getIndex()));
            if (c.contains(inst.getIndex())) {
                mutual++;
            }
        }
        return mutual;
    }

    @Override
    public InstanceBuilder builder() {
        if (builder == null) {
            builder = new DoubleArrayFactory(this, '.');
        }
        return builder;
    }

    @Override
    public AttributeBuilder attributeBuilder() {
        if (attributeBuilder == null) {
            attributeBuilder = new AttributeFactoryImpl<>(this);
        }
        return attributeBuilder;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BaseCluster ");
        sb.append(getName());
        sb.append(" (").append(size()).append(") ");
        sb.append(" [ ");
        E elem;
        for (int i = 0; i < this.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            elem = this.get(i);
            sb.append(elem.getIndex());
        }
        sb.append(" ]");
        return sb.toString();
    }

    /**
     * Hash code for clusters should not depend on order of elements in the
     * cluster.
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 0;
        for (E elem : this) {
            hash += elem.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BaseCluster<?> other = (BaseCluster<?>) obj;
        if (this.size() != other.size()) {
            return false;
        }
        return this.hashCode() == other.hashCode();
    }
}
