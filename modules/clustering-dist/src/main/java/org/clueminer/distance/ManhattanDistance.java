package org.clueminer.distance;

import org.clueminer.distance.api.DistanceMeasure;
import org.clueminer.math.Matrix;
import org.openide.util.lookup.ServiceProvider;

/**
 * Manhattan (or city block distance) is the distance you would have to walk
 * between two points in a city with regular grid schema (like Manhattan)
 *
 * Generalization of this distance metric is Minkowski distance, in this case (n
 * = 1)
 *
 * @author Tomas Barton
 */
@ServiceProvider(service = DistanceMeasure.class)
public class ManhattanDistance extends MinkowskiDistance implements DistanceMeasure {

    private static final String name = "Manhattan";
    private static float similarityFactor = 1.0f;
    private static int offset = 0;
    private static final long serialVersionUID = 3287053682318427095L;

    public ManhattanDistance() {
        super(1.0);
    }

    @Override
    public String getName() {
        return name;
    }

    public double columns(Matrix matrix, int e1, int e2) {
        int n, j;
        double sum = 0.0;
        n = matrix.rowsCount();
        for (j = 0; j < n; j++) {
            if ((!Double.isNaN(matrix.get(j, e1))) && (!Double.isNaN(matrix.get(j, e2)))) {
                sum += Math.abs(matrix.get(j, e1) - matrix.get(j, e2));
            }
        }
        return sum;
    }

    public double rows(Matrix A, Matrix B, int g1, int g2) {
        int j;
        double sum = 0.0;
        int n = A.columnsCount();
        for (j = 0; j < n; j++) {
            if ((!Double.isNaN(A.get(g1, j))) && (!Double.isNaN(B.get(g2, j)))) {
                sum += Math.abs(A.get(g1, j) - A.get(g2, j));
            }
        }
        return sum;
    }

    @Override
    public float getSimilarityFactor() {
        return similarityFactor;
    }

    @Override
    public int getNodeOffset() {
        return offset;
    }

    @Override
    public boolean compare(double x, double y) {
        return x < y;
    }

    @Override
    public double getMinValue() {
        return 0;
    }

    @Override
    public double getMaxValue() {
        return Double.MAX_VALUE;
    }

}
