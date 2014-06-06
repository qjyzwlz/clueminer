package org.clueminer.clustering.aggl;

import java.util.AbstractQueue;
import java.util.PriorityQueue;
import org.clueminer.distance.api.DistanceMeasure;
import org.clueminer.math.Matrix;
import org.clueminer.math.MatrixVector;
import org.clueminer.math.matrix.JMatrix;
import org.clueminer.math.matrix.SymmetricMatrix;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;

/**
 * Agglomerative clustering methods
 *
 * @author Tomas Barton
 */
public class AgglClustering {

    /**
     * Computes and returns the similarity matrix for {@code m} using the
     * specified similarity function
     *
     * @param m
     * @param dm
     * @return
     */
    public static Matrix rowSimilarityMatrix(Matrix m, DistanceMeasure dm) {
        return rowSimilarityMatrix(m, dm, null);
    }

    /**
     * Computes and returns the similarity matrix for {@code m} using the
     * specified similarity function. Moreover matrix values will be stored in
     * queue.
     *
     * @param m
     * @param dm
     * @param queue queue to store computed number
     * @return
     */
    public static Matrix rowSimilarityMatrix(Matrix m, DistanceMeasure dm, AbstractQueue<Element> queue) {
        Matrix similarityMatrix;
        int n = 0;
        double dist;
        ProgressHandle ph = ProgressHandleFactory.createHandle("Computing row similarity matrix (" + m.rowsCount() + " x " + m.rowsCount() + ")");
        int total = (m.rowsCount() - 1) * m.rowsCount() / 2;
        ph.start(total);
        if (dm.isSymmetric()) {

            similarityMatrix = new SymmetricMatrix(m.rowsCount(), m.rowsCount());
            for (int i = 0; i < m.rowsCount(); ++i) {
                for (int j = i + 1; j < m.rowsCount(); ++j) {
                    dist = dm.measure(m.getRowVector(i), m.getRowVector(j));
                    similarityMatrix.set(i, j, dist);
                    // when printing lower part of matrix this indexes should match
                    if (queue != null) {
                        queue.add(new Element(dist, i, j));
                    }
                    ph.progress(n++);
                }
            }
        } else {
            double dist2;
            MatrixVector vi, vj;
            similarityMatrix = new JMatrix(m.rowsCount(), m.rowsCount());
            for (int i = 0; i < m.rowsCount(); ++i) {
                for (int j = i + 1; j < m.rowsCount(); ++j) {
                    /**
                     * measure is not symmetrical, we have to compute distance
                     * from A to B and from B to A
                     */
                    vi = m.getRowVector(i);
                    vj = m.getRowVector(j);
                    dist = dm.measure(vi, vj);
                    similarityMatrix.set(i, j, dist);
                    dist2 = dm.measure(vj, vi);
                    similarityMatrix.set(j, i, dist2);
                    if (queue != null) {
                        queue.add(new Element(dist, i, j));
                        queue.add(new Element(dist2, j, i));
                    }
                    ph.progress(n++);
                }
            }
        }
        ph.finish();
        return similarityMatrix;
    }

    /**
     * Computes similarity matrix for columns
     *
     * @param m
     * @param dm
     * @return
     */
    public static Matrix columnSimilarityMatrix(Matrix m, DistanceMeasure dm) {
        return columnSimilarityMatrix(m, dm, null);
    }

    static Matrix columnSimilarityMatrix(Matrix m, DistanceMeasure dm, PriorityQueue<Element> queue) {
        Matrix similarityMatrix;
        int n = 0;
        double dist;
        ProgressHandle ph = ProgressHandleFactory.createHandle("Computing column similarity matrix (" + m.columnsCount() + " x " + m.columnsCount() + ")");
        int total = (m.columnsCount() - 1) * m.columnsCount() / 2;
        ph.start(total);
        if (dm.isSymmetric()) {
            similarityMatrix = new SymmetricMatrix(m.columnsCount(), m.columnsCount());
            for (int i = 0; i < m.columnsCount(); ++i) {
                for (int j = i + 1; j < m.columnsCount(); ++j) {
                    dist = dm.measure(m.getColumnVector(i), m.getColumnVector(j));
                    similarityMatrix.set(i, j, dist);
                    if (queue != null) {
                        // when printing lower part of matrix this indexes should match
                        queue.add(new Element(dist, i, j));
                    }
                    ph.progress(n++);
                }
            }
        } else {
            double dist2;
            MatrixVector vi, vj;
            similarityMatrix = new JMatrix(m.columnsCount(), m.columnsCount());
            for (int i = 0; i < m.rowsCount(); ++i) {
                for (int j = i + 1; j < m.rowsCount(); ++j) {
                    /**
                     * measure is not symmetrical, we have to compute distance
                     * from A to B and from B to A
                     */
                    vi = m.getColumnVector(i);
                    vj = m.getColumnVector(j);
                    dist = dm.measure(vi, vj);
                    similarityMatrix.set(i, j, dist);
                    //inversed distance
                    dist2 = dm.measure(vj, vi);
                    similarityMatrix.set(j, i, dist2);
                    if (queue != null) {
                        queue.add(new Element(dist, i, j));
                        queue.add(new Element(dist2, j, i));
                    }
                    ph.progress(n++);
                }
            }
        }
        ph.finish();
        return similarityMatrix;
    }

}
