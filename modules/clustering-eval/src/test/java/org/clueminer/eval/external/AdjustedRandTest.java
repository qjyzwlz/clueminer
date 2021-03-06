package org.clueminer.eval.external;

import com.google.common.collect.Table;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.clueminer.clustering.api.Cluster;
import org.clueminer.clustering.api.Clustering;
import org.clueminer.eval.utils.CountingPairs;
import org.clueminer.eval.utils.PairMatch;
import org.clueminer.fixtures.clustering.FakeClustering;
import org.clueminer.utils.Dump;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author deric
 */
public class AdjustedRandTest extends ExternalTest {

    public AdjustedRandTest() {
        subject = new AdjustedRand();
    }

    /**
     * Test of isBetter method, of class AdjustedRand.
     */
    @Test
    public void testCompareScore() {
        assertTrue(subject.isBetter(1.0, 0.5));
    }

    /**
     * Test of score method, of class AdjustedRand.
     */
    @Test
    public void testScore_Clustering_Clustering() {
        double score;

        score = measure(FakeClustering.wineClustering(), FakeClustering.wineCorrect(), 0.21052631578947367);
        measure(FakeClustering.wineClustering(), score);
    }

    /**
     * Test based on Details of the Adjusted Rand index and Clustering
     * algorithms Supplement to the paper “An empirical study on Principal
     * Component Analysis for clustering gene expression data” (to appear in
     * Bioinformatics)
     *
     * @see http://faculty.washington.edu/kayee/pca/supp.pdf
     * @see http://faculty.washington.edu/kayee/pca/pca.pdf
     *
     */
    @Test
    public void testCountScore() {
        //Table<String, String, Integer> table = CountingPairs.newTable();
        //fake contingency table

        int[][] mat = new int[4][4];
        mat[0][0] = 1;
        mat[0][1] = 1;
        mat[0][2] = 0;
        mat[0][3] = 2; //sum of row[0]
        mat[1][0] = 1;
        mat[1][1] = 2;
        mat[1][2] = 1;
        mat[1][3] = 4; //sum of row[1]
        mat[2][0] = 0;
        mat[2][1] = 0;
        mat[2][2] = 4;
        mat[2][3] = 4;//sum of row[2]
        //col sums
        mat[3][0] = 2;
        mat[3][1] = 3;
        mat[3][2] = 5;
        mat[3][3] = 10;

        AdjustedRand rand = (AdjustedRand) subject;
        double score = rand.countScore(mat);
        System.out.println("score: " + score);
        assertEquals(0.31257344300822565, score, delta);
    }

    @Test
    public void testScoreDataset() {
        measure(FakeClustering.irisWrong4(), 0.873083475298126);
    }

    @Test
    public void testIris() {
        Clustering<Cluster> clustering = FakeClustering.irisWrong5();
        Table<String, String, Integer> table = CountingPairs.contingencyTable(clustering);
        CountingPairs.dumpTable(table);
        AdjustedRand rand = (AdjustedRand) subject;
        double score = rand.countScore(table);
        //value based on experiments (not verified yet) - just to verify that we didnt break the functionality
        //assertEquals(0.14754877843024122, score, delta);
        assertEquals(0.15018942476428662, score, delta);
        System.out.println("clust(5) = " + score);
    }

    //@Test
    public void testIris2() {
        AdjustedRand ari = (AdjustedRand) subject;
        System.out.println("==== computing better");
        double scoreBetter;
        Table<String, String, Integer> table = CountingPairs.contingencyTable(FakeClustering.iris());
        int[][] extc = ari.extendedContingency(table);
        Dump.matrix(extc, "better extc", 2);
        scoreBetter = ari.countScore(extc);
        assertEquals(150, extc[extc.length - 1][extc[0].length - 1]);
        System.out.println("better table ");
        CountingPairs.dumpTable(table);
        System.out.println("better = " + scoreBetter);
        System.out.println("==== computing worser");
        double scoreWorser;
        table = CountingPairs.contingencyTable(FakeClustering.irisMostlyWrong());
        extc = ari.extendedContingency(table);
        Dump.matrix(extc, "worser extc", 2);
        scoreWorser = ari.countScore(extc);
        //last cell in table should sum all counts in the table
        assertEquals(150, extc[extc.length - 1][extc[0].length - 1]);
        System.out.println("worser table ");
        CountingPairs.dumpTable(table);
        System.out.println("worser = " + scoreWorser);

        //should recognize better clustering
        assertEquals(true, subject.isBetter(scoreBetter, scoreWorser));
    }

    @Test
    public void testContingency() {
        AdjustedRand ari = (AdjustedRand) subject;
        Table<String, String, Integer> table = CountingPairs.contingencyTable(FakeClustering.iris());
        int[][] extCont = ari.extendedContingency(table);
        //should be eq to number of items in the dataset
        assertEquals(150, extCont[extCont.length - 1][extCont[0].length - 1]);
    }

    @Test
    public void testAri() {
        AdjustedRand ari = (AdjustedRand) subject;
        double score = ari.score(FakeClustering.iris());
        assertEquals(1.0, score, delta);

        //score = ari.score(FakeClustering.irisWrong());
        //assertEquals(1.0, score, delta);
    }

    @Test
    public void testTwoClusterings() {
        AdjustedRand ari = (AdjustedRand) subject;
        PairMatch pm = CountingPairs.matchPairs(FakeClustering.irisWrong(), FakeClustering.iris());

        pm.dump();
        //number of pairs we can draw from 150 instances (150 \over 2)
        assertEquals(CombinatoricsUtils.binomialCoefficient(150, 2), pm.sum());
        System.out.println("ARI: " + ari.score(pm));
    }

    /**
     * Based on Details of the Adjusted Rand index and Clustering algorithms
     * Supplement to the paper “An empirical study on Principal
     * Component Analysis for clustering gene expression data” (to
     * appear in Bioinformatics)
     *
     * Ka Yee Yeung, Walter L. Ruzzo, 2001
     *
     */
    @Test
    public void testPcaData() {
        Clustering<? extends Cluster> clust = pcaData();
        double score = subject.score(clust);
        assertEquals(0.3125734430082256, score, delta);
    }

    @Test
    public void testCombinationOfTwo() {
        int n = 5;
        AdjustedRand ari = (AdjustedRand) subject;
        assertEquals(CombinatoricsUtils.binomialCoefficient(n, 2), ari.combinationOfTwo(n));
        n = 15;
        assertEquals(CombinatoricsUtils.binomialCoefficient(n, 2), ari.combinationOfTwo(n));
    }

}
