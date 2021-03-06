package org.clueminer.importer.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import org.clueminer.attributes.BasicAttrRole;
import org.clueminer.fixtures.CommonFixture;
import org.clueminer.io.importer.api.Container;
import org.clueminer.io.importer.api.ContainerLoader;
import org.clueminer.io.importer.api.InstanceDraft;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author deric
 */
public class CsvImporterTest {

    private CsvImporter subject;
    private static final CommonFixture fixtures = new CommonFixture();

    public CsvImporterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        subject = new CsvImporter();
        subject.setLoader(new ImportContainerImpl());
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getName method, of class CsvImporter.
     */
    @Test
    public void testGetName() {
        assertEquals("CSV", subject.getName());
    }

    /**
     * Test of getSeparator method, of class CsvImporter.
     */
    @Test
    public void testGetSeparator() {
        //default separator should be comma
        assertEquals(',', subject.getSeparator());
    }

    /**
     * Test of setSeparator method, of class CsvImporter.
     */
    @Test
    public void testSetSeparator() {
        subject.setSeparator(';');
        assertEquals(';', subject.getSeparator());
    }

    /**
     * Test of setReader method, of class CsvImporter.
     */
    @Test
    public void testSetReader() {
    }

    /**
     * Test of getFile method, of class CsvImporter.
     */
    @Test
    public void testGetFile() {
    }

    /**
     * Test of isAccepting method, of class CsvImporter.
     */
    @Test
    public void testIsAccepting() {
    }

    /**
     * Test of execute method, of class CsvImporter.
     *
     * @throws java.io.IOException
     */
    //   @Test
    public void testExecute() throws IOException {
        Container container = new ImportContainerImpl();

        subject.execute(container, FileUtil.toFileObject(fixtures.irisData()));
        assertEquals(150, container.getLoader().getNumberOfLines());
    }

    /**
     * Test of getContainer method, of class CsvImporter.
     */
    @Test
    public void testGetContainer() {
    }

    /**
     * Test of getReport method, of class CsvImporter.
     */
    @Test
    public void testGetReport() {
    }

    /**
     * Test of getFileTypes method, of class CsvImporter.
     */
    @Test
    public void testGetFileTypes() {
    }

    /**
     * Test of isMatchingImporter method, of class CsvImporter.
     */
    @Test
    public void testIsMatchingImporter() {
    }

    /**
     * Test of cancel method, of class CsvImporter.
     */
    @Test
    public void testCancel() {
    }

    /**
     * Test of setProgressTicket method, of class CsvImporter.
     */
    @Test
    public void testSetProgressTicket() {
    }

    @Test
    public void testParseLine() throws Exception {
        String nextItem[] = subject.parseLine("This, is, a, test.");
        assertEquals(4, nextItem.length);
        assertEquals("This", nextItem[0]);
        assertEquals(" is", nextItem[1]);
        assertEquals(" a", nextItem[2]);
        assertEquals(" test.", nextItem[3]);
    }

    @Test
    public void parseSimpleString() throws IOException {
        String[] nextLine = subject.parseLine("a,b,c");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertFalse(subject.isPending());
    }

    @Test
    public void parseSimpleQuotedString() throws IOException {
        String[] nextLine = subject.parseLine("\"a\",\"b\",\"c\"");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertFalse(subject.isPending());
    }

    @Test
    public void parseSimpleQuotedStringWithSpaces() throws IOException {
        subject.setStrictQuotes(true);
        subject.setIgnoreLeadingWhiteSpace(true);

        String[] nextLine = subject.parseLine(" \"a\" , \"b\" , \"c\" ");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertFalse(subject.isPending());
    }

    /**
     * Tests quotes in the middle of an element.
     *
     * @throws IOException if bad things happen
     */
    @Test
    public void testParsedLineWithInternalQuota() throws IOException {
        String[] nextLine = subject.parseLine("a,123\"4\"567,c");
        assertEquals(3, nextLine.length);
        assertEquals("123\"4\"567", nextLine[1]);
    }

    @Test
    public void parseQuotedStringWithCommas() throws IOException {
        String[] nextLine = subject.parseLine("a,\"b,b,b\",c");
        assertEquals("a", nextLine[0]);
        assertEquals("b,b,b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertEquals(3, nextLine.length);
    }

    @Test
    public void parseQuotedStringWithDefinedSeperator() throws IOException {
        subject.setSeparator(':');

        String[] nextLine = subject.parseLine("a:\"b:b:b\":c");
        assertEquals("a", nextLine[0]);
        assertEquals("b:b:b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertEquals(3, nextLine.length);
    }

    @Test
    public void parseQuotedStringWithDefinedSeperatorAndQuote() throws IOException {
        subject.setSeparator(':');
        subject.setQuotechar('\'');

        String[] nextLine = subject.parseLine("a:'b:b:b':c");
        assertEquals("a", nextLine[0]);
        assertEquals("b:b:b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertEquals(3, nextLine.length);
    }

    @Test
    public void parseEmptyElements() throws IOException {
        String[] nextLine = subject.parseLine(",,");
        assertEquals(3, nextLine.length);
        assertEquals("", nextLine[0]);
        assertEquals("", nextLine[1]);
        assertEquals("", nextLine[2]);
    }

    @Test
    public void parseMultiLinedQuoted() throws IOException {
        String[] nextLine = subject.parseLine("a,\"PO Box 123,\nKippax,ACT. 2615.\nAustralia\",d.\n");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("PO Box 123,\nKippax,ACT. 2615.\nAustralia", nextLine[1]);
        assertEquals("d.\n", nextLine[2]);
    }

    @Test
    public void testADoubleQuoteAsDataElement() throws IOException {
        String[] nextLine = subject.parseLine("a,\"\"\"\",c");// a,"""",c

        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals(1, nextLine[1].length());
        assertEquals("\"", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    @Test
    public void testEscapedDoubleQuoteAsDataElement() throws IOException {
        subject.setStrictQuotes(true);
        //subject.setIgnoreQuotations(true);
        String[] nextLine = subject.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); // "test","this,test,is,good","\"test\",\"quote\""

        assertEquals(4, nextLine.length);

        assertEquals("test", nextLine[0]);
        assertEquals("this,test,is,good", nextLine[1]);
        // assertEquals("\"test\"", nextLine[2]);
        //assertEquals("\"quote\"", nextLine[3]);

    }

    @Test
    public void testImportData() throws Exception {
    }

    @Test
    public void testLineRead() throws Exception {
    }

    @Test
    public void testParseType() {
        subject.getLoader().createAttribute(0, "test");
        subject.parseType("Double", 0);
    }

    @Test
    public void testIsPending() {
    }

    @Test
    public void testIsNextCharacterEscapable() {
    }

    @Test
    public void testIsAllWhiteSpace() {
    }

    @Test
    public void testIsHasHeader() {
    }

    @Test
    public void testSetHasHeader() {
    }

    @Test
    public void testIsSkipHeader() {
    }

    @Test
    public void testSetSkipHeader() {
    }

    @Test
    public void testGetQuotechar() {
    }

    @Test
    public void testSetQuotechar() {
    }

    @Test
    public void testIsCancel() {
    }

    @Test
    public void testSetCancel() {
    }

    @Test
    public void testIsIgnoreQuotations() {
    }

    @Test
    public void testSetIgnoreQuotations() {
    }

    @Test
    public void testIsStrictQuotes() {
    }

    @Test
    public void testSetStrictQuotes() {
    }

    @Test
    public void testGetEscape() {
    }

    @Test
    public void testSetEscape() {
    }

    @Test
    public void testIsIgnoreLeadingWhiteSpace() {
    }

    @Test
    public void testSetIgnoreLeadingWhiteSpace() {
    }

    @Test
    public void testGetLineReader() {
    }

    @Test
    public void testSetLineReader() {
    }

    @Test
    public void testMissingValues() {
        CsvImporter importer = new CsvImporter();
        Container container = new ImportContainerImpl();
        importer.setSeparator(';');
        importer.setHasHeader(false);
        List<String> missing = new LinkedList<String>();
        missing.add("NA");
        importer.setMissing(missing);
        String line = "id-123;1;NA;NA;1;1;1;1;1;1;1;NA;1;1;1;1;1";
        Reader reader = new StringReader(line);
        try {
            importer.execute(container, reader);

            ContainerLoader loader = container.getLoader();
            assertEquals(1, loader.getNumberOfLines());
            assertEquals(BasicAttrRole.INPUT, loader.getAttribute(0).getRole());
            assertEquals(17, loader.getAttributeCount());
            assertEquals(1, loader.getInstanceCount());
            InstanceDraft inst = loader.getInstance(0);
            System.out.println("inst " + inst.toString());
            assertEquals(1.0, inst.getValue(1));
            assertEquals(Double.NaN, inst.getValue(2));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    @Test
    public void testParsingHeader() {
        CsvImporter importer = new CsvImporter();
        Container container = new ImportContainerImpl();
        importer.setSeparator(',');
        importer.setHasHeader(true);
        String line = "id,meta_1,input_1,foo,bar";
        Reader reader = new StringReader(line);
        try {
            importer.execute(container, reader);

            ContainerLoader loader = container.getLoader();
            assertEquals(1, loader.getNumberOfLines());
            assertEquals(BasicAttrRole.ID, loader.getAttribute(0).getRole());
            assertEquals(BasicAttrRole.META, loader.getAttribute(1).getRole());
            assertEquals(BasicAttrRole.INPUT, loader.getAttribute(2).getRole());
            assertEquals(5, loader.getAttributeCount());
            assertEquals(0, loader.getInstanceCount());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Test
    public void testParsingTypes() {
        CsvImporter importer = new CsvImporter();
        Container container = new ImportContainerImpl();
        importer.setSeparator(',');
        importer.setHasHeader(true);
        String line = "attr1,attr2,attr3\ndouble,double,string";
        Reader reader = new StringReader(line);
        try {
            importer.execute(container, reader);

            ContainerLoader loader = container.getLoader();
            assertEquals(2, loader.getNumberOfLines());
            assertEquals(Double.class, loader.getAttribute(0).getType());
            assertEquals(Double.class, loader.getAttribute(1).getType());
            assertEquals(String.class, loader.getAttribute(2).getType());
            assertEquals(3, loader.getAttributeCount());
            assertEquals(0, loader.getInstanceCount());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
