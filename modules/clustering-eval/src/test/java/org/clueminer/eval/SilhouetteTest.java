package org.clueminer.eval;

import org.clueminer.clustering.api.Clustering;
import org.clueminer.clustering.api.HierarchicalResult;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.fixtures.clustering.FakeClustering;
import org.clueminer.fixtures.clustering.FakeDatasets;
import org.clueminer.math.Matrix;
import org.clueminer.math.matrix.JMatrix;
import org.clueminer.utils.Props;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Tomas Barton
 */
public class SilhouetteTest {

    private static final Silhouette test = new Silhouette();
    private static Dataset<? extends Instance> dataset;
    private static Clustering clustering;
    private static Clustering clusters;
    private static Props params;
    private static HierarchicalResult rowsResult;
    private static Matrix input;
    private static double delta = 1e-9;

    public SilhouetteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        clusters = FakeClustering.iris();
        dataset = FakeDatasets.irisDataset();

        params = getParams();
        input = new JMatrix(dataset.arrayCopy());

        /*  ClusteringAlgorithm algorithm = new HCL();
         algorithm.setDistanceFunction(new EuclideanDistance());
         params.setProperty("method-linkage", String.valueOf(0)); //-1=single, 0=complete, 1/2=average
         rowsResult = algorithm.hierarchy(input, dataset, params);*/
        ///clustering = rowsResult.getClustering(dendroData.getRowsMapping(), dataset);
    }

    private static Props getParams() {
        Props p = new Props();
        // alg name
        p.put("name", "HCL");
        p.putDouble("distance-factor", 1.0);
        p.putDouble("hcl-distance-absolute", 1.0);

        p.putBoolean("calculate-experiments", true);
        p.putBoolean("optimize-rows-ordering", true);
        p.putBoolean("optimize-cols-ordering", true);
        p.putBoolean("optimize-sample-ordering", true);
        p.putBoolean("calculate-rows", true);
        return p;
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of getName method, of class Silhouette.
     */
    @Test
    public void testGetName() {
    }

    /**
     * Test of score method, of class Silhouette.
     */
    //@Test
    public void testScore_Clustering() {
        double score;
        long start = System.currentTimeMillis();
        score = test.score(clustering);
        System.out.println("Silhouette= " + score);
        long end = System.currentTimeMillis();
        assertTrue(score != Double.NaN);
        System.out.println("computing took = " + (end - start) + " ms");
    }

    /**
     * Test of score method, of class Silhouette.
     */
    @Test
    public void testScore_ClusteringMatlab() {
        double score;
        long start = System.currentTimeMillis();
        score = test.score(clusters);
        System.out.println("Silhouette= " + score);
        double matlab = 0.6567;
        long end = System.currentTimeMillis();
        assertTrue(score != Double.NaN);
        /**
         * @TODO fix this
         */
        //assertEquals(matlab, score, delta);
        System.out.println("computing took = " + (end - start) + " ms");
    }

    /**
     * Test of score method, of class Silhouette.
     */
    @Test
    public void testScore_Clustering_Matrix() {
    }

    /**
     * Test of isBetter method, of class Silhouette.
     */
    @Test
    public void testCompareScore() {
    }
}
