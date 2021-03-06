package org.clueminer.cluster;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.clueminer.clustering.algorithm.KMeans;
import org.clueminer.clustering.api.Cluster;
import org.clueminer.clustering.api.Clustering;
import org.clueminer.clustering.api.PartitioningClustering;
import org.clueminer.clustering.struct.BaseCluster;
import org.clueminer.clustering.struct.ClusterList;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.dataset.plugin.SampleDataset;
import org.clueminer.distance.EuclideanDistance;
import org.clueminer.fixtures.CommonFixture;
import org.clueminer.io.ARFFHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.util.Exceptions;

/**
 *
 * @author deric
 */
public class ClusterListTest {

    private Clustering<Cluster> clusters;
    private static Dataset<? extends Instance> data;

    public ClusterListTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        CommonFixture tf = new CommonFixture();
        data = new SampleDataset();
        ARFFHandler arff = new ARFFHandler();
        try {
            arff.load(tf.irisArff(), data, 4);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        PartitioningClustering km = new KMeans(3, 100, new EuclideanDistance());
        clusters = km.partition(data);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of hasAt method, of class ClusterList.
     */
    @Test
    public void testHasAt() {
        assertEquals(true, clusters.hasAt(0));
        assertEquals(true, clusters.hasAt(1));
        assertEquals(true, clusters.hasAt(2));
    }

    /**
     * Test of getClusterLabel method, of class ClusterList.
     */
    @Test
    public void testGetClusterLabel() {
    }

    /**
     * Test of put method, of class ClusterList.
     */
    @Test
    public void testPut_Cluster() {
    }

    /**
     * Test of put method, of class ClusterList.
     */


    @Test
    public void testPut_rand_idx() {
        ClusterList subject = new ClusterList(10);
        assertEquals(0, subject.size());
        subject.put(1, new BaseCluster(1));
        assertEquals(true, subject.hasAt(1));
        assertEquals(false, subject.hasAt(0));
        subject.put(3, new BaseCluster(1));
        assertEquals(2, subject.size());
        subject.put(0, new BaseCluster(1));
        assertEquals(3, subject.size());
    }

    @Test
    public void testPut_cluster() {
        ClusterList subject = new ClusterList(2);
        assertEquals(0, subject.size());

        //add first cluster at pos 1
        subject.put(1, new BaseCluster(1));
        assertEquals(true, subject.hasAt(1));
        assertEquals(1, subject.size());

        //add second at 0
        subject.put(0, new BaseCluster(5));
        assertEquals(true, subject.hasAt(0));
        assertEquals(2, subject.size());

        //add third at 2
        subject.put(2, new BaseCluster(5));
        assertEquals(true, subject.hasAt(2));
        assertEquals(3, subject.size());
        System.out.println("capacity: " + subject.getCapacity());
    }

    @Test
    public void testPut_int_cluster() {
        ClusterList subject = new ClusterList(3);
        assertEquals(0, subject.size());
        System.out.println("capacity: " + subject.getCapacity());
        //add third at 2
        subject.put(2, new BaseCluster(5));
        assertEquals(true, subject.hasAt(2));
        assertEquals(1, subject.size());
        assertEquals(false, subject.hasAt(99));

    }

    /**
     * Test of merge method, of class ClusterList.
     */
    @Test
    public void testMerge() {
    }

    /**
     * Test of instancesCount method, of class ClusterList.
     */
    @Test
    public void testInstancesCount() {
        assertEquals(data.size(), clusters.instancesCount());
    }

    /**
     * Test of getCentroid method, of class ClusterList.
     */
    @Test
    public void testGetCentroid() {
        Instance centroid = clusters.getCentroid();
        System.out.println("centroid: " + centroid);
    }

    /**
     * Test of ensureCapacity method, of class ClusterList.
     */
    @Test
    public void testEnsureCapacity() {
    }

    /**
     * Test of add method, of class ClusterList.
     */
    @Test
    public void testAdd() {
    }

    /**
     * Test of getCapacity method, of class ClusterList.
     */
    @Test
    public void testGetCapacity() {
    }

    /**
     * Test of size method, of class ClusterList.
     */
    @Test
    public void testSize() {
        assertEquals(3, clusters.size());
    }

    /**
     * Test of clusterSizes method, of class ClusterList.
     */
    @Test
    public void testClusterSizes() {
    }

    /**
     * Test of assignedCluster method, of class ClusterList.
     */
    @Test
    public void testAssignedCluster() {
    }

    /**
     * Test of get method, of class ClusterList.
     */
    @Test
    public void testGet() {
    }

    /**
     * Test of isEmpty method, of class ClusterList.
     */
    @Test
    public void testIsEmpty() {
        assertEquals(false, clusters.isEmpty());
    }

    /**
     * Test of toArray method, of class ClusterList.
     */
    @Test
    public void testToArray_0args() {
        Object[] ary = clusters.toArray();
        assertNotNull(ary);
        assertEquals(3, ary.length);
    }

    /**
     * Test of toArray method, of class ClusterList.
     */
    @Test
    public void testToArray_GenericType() {
    }

    /**
     * Test of remove method, of class ClusterList.
     */
    @Test
    public void testRemove() {
    }

    /**
     * Test of containsAll method, of class ClusterList.
     */
    @Test
    public void testContainsAll() {
    }

    /**
     * Test of addAll method, of class ClusterList.
     */
    @Test
    public void testAddAll() {
    }

    /**
     * Test of removeAll method, of class ClusterList.
     */
    @Test
    public void testRemoveAll() {
    }

    /**
     * Test of retainAll method, of class ClusterList.
     */
    @Test
    public void testRetainAll() {
    }

    /**
     * Test of clear method, of class ClusterList.
     */
    @Test
    public void testClear() {
    }

    @Test
    public void testCreateCluster_int() {
        int size = clusters.size();
        Cluster c = clusters.createCluster(size + 1);
        //ID start from 1
        assertEquals(size + 1, c.getClusterId());
        assertEquals(size + 1, clusters.size());
    }

    @Test
    public void testCreateCluster_0args() {
        int size = clusters.size();
        Cluster c = clusters.createCluster();
        assertEquals(size, c.getClusterId());
    }

    @Test
    public void testToString() {
        String str = clusters.toString();
        assertNotNull(str);
        System.out.println(str);
    }

}
