package org.clueminer.importer.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.clueminer.importer.Issue;
import org.clueminer.io.importer.api.Report;
import org.clueminer.longtask.spi.LongTask;
import org.clueminer.spi.FileImporter;
import org.clueminer.types.ContainerLoader;
import org.clueminer.types.FileType;
import org.clueminer.utils.progress.Progress;
import org.clueminer.utils.progress.ProgressTicket;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Barton
 */
@ServiceProvider(service = FileImporter.class)
public class CsvImporter implements FileImporter, LongTask {

    private boolean hasHeader = true;
    private boolean skipHeader = false;
    private char separator = ',';
    private char quotechar = '"';
    private static final String name = "CSV";
    private File file;
    private Reader reader;
    private ContainerLoader container;
    private Report report;
    private ProgressTicket progressTicket;
    private boolean cancel = false;
    private static final Logger logger = Logger.getLogger(CsvImporter.class.getName());
    private static final int INITIAL_READ_SIZE = 128;
    private String pending;
    private boolean ignoreQuotations = false;
    private boolean strictQuotes = false;
    private char escape;
    private boolean inField = false;
    //white space in front of a quote in a field is ignored
    private boolean ignoreLeadingWhiteSpace = false;
    private int prevColCnt = -1;

    public CsvImporter() {
        separator = ',';
    }

    @Override
    public String getName() {
        return name;
    }

    public char getSeparator() {
        return separator;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    @Override
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public boolean isAccepting(Collection mimeTypes) {
        String mime = mimeTypes.toString();
        //this will match pretty much anything
        return mime.contains("text") || mime.contains("octet-stream");
    }

    @Override
    public boolean execute(ContainerLoader loader) {
        this.container = loader;
        this.report = new Report();
        LineNumberReader lineReader;
        this.file = container.getFile();
        try {
            if (reader != null) {
                lineReader = ImportUtils.getTextReader(reader);
            } else if (file != null) {
                lineReader = getReader(file);
            } else {
                throw new RuntimeException("importer wasn't provided with any readable source");
            }

            importData(lineReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return !cancel;
    }

    @Override
    public ContainerLoader getContainer() {
        return container;
    }

    @Override
    public Report getReport() {
        return report;
    }

    @Override
    public FileType[] getFileTypes() {
        FileType ft = new FileType(".csv", NbBundle.getMessage(getClass(), "fileType_CSV_Name"));
        return new FileType[]{ft};
    }

    @Override
    public boolean isMatchingImporter(FileObject fileObject) {
        return fileObject.getExt().equalsIgnoreCase("csv");
    }

    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }

    protected void importData(LineNumberReader reader) throws IOException {
        Progress.start(progressTicket);        //Progress

        /*it.setSkipBlanks(true);
         it.setCommentIdentifier("#");
         it.setSkipComments(true);*/
        int count = 0;
        for (; reader.ready();) {
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                lineRead(count, line);
                count++;
            }
        }
        container.setNumberOfLines(count);

        //   Progress.switchToDeterminate(progressTicket, lines.size());
        //if we know number of lines
        //    Progress.progress(progressTicket);      //Progress
    }

    protected void lineRead(int num, String line) throws IOException {
        String[] columns = parseLine(line);
        if (prevColCnt != columns.length && prevColCnt > -1) {
            report.logIssue(new Issue(NbBundle.getMessage(CsvImporter.class, "CsvImporter_error_differentLineLength", num), Issue.Level.WARNING));
        } else {
            prevColCnt = columns.length;
        }

        if (hasHeader && !skipHeader) {
            // parseHeader(iter.next());
            ///Dump.array(((CSVIterator) iter).showNext(), "next line: ");
        } else if (skipHeader) {
            //    iter.next(); // just skip it
        }

    }

    private void addInstance(String data) {
        //container.
    }

    /**
     * @return true if something was left over from last call(s)
     */
    public boolean isPending() {
        return pending != null;
    }

    protected String[] parseLine(String line) throws IOException {
        List<String> tokensOnThisLine = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(INITIAL_READ_SIZE);
        boolean inQuotes = false;
        if (pending != null) {
            sb.append(pending);
            pending = null;
            inQuotes = !this.ignoreQuotations;//true;
        }
        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);
            if (c == this.escape) {
                if (isNextCharacterEscapable(line, (inQuotes && !ignoreQuotations) || inField, i)) {
                    sb.append(line.charAt(i + 1));
                    i++;
                }
            } else if (c == quotechar) {
                if (isNextCharacterEscapedQuote(line, (inQuotes && !ignoreQuotations) || inField, i)) {
                    sb.append(line.charAt(i + 1));
                    i++;
                } else {
                    inQuotes = !inQuotes;
                    // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                    if (!strictQuotes) {
                        if (i > 2 //not on the beginning of the line
                                && line.charAt(i - 1) != this.separator //not at the beginning of an escape sequence
                                && line.length() > (i + 1)
                                && line.charAt(i + 1) != this.separator //not at the	end of an escape sequence
                                ) {

                            if (ignoreLeadingWhiteSpace && sb.length() > 0 && isAllWhiteSpace(sb)) {
                                sb = new StringBuilder(INITIAL_READ_SIZE);  //discard white space leading up to quote
                            } else {
                                sb.append(c);
                            }
                        }
                    }
                }
                inField = !inField;
            } else if (c == separator && !(inQuotes && !ignoreQuotations)) {
                tokensOnThisLine.add(sb.toString());
                sb = new StringBuilder(INITIAL_READ_SIZE); // start work on next token
                inField = false;
            } else {
                if (!strictQuotes || (inQuotes && !ignoreQuotations)) {
                    sb.append(c);
                    inField = true;
                }
            }
        }
        tokensOnThisLine.add(sb.toString());

        return tokensOnThisLine.toArray(new String[tokensOnThisLine.size()]);
    }

    /**
     * precondition: the current character is a quote or an escape
     *
     * @param nextLine the current line
     * @param inQuotes true if the current context is quoted
     * @param i        current index in line
     * @return true if the following character is a quote
     */
    private boolean isNextCharacterEscapedQuote(String nextLine, boolean inQuotes, int i) {
        return inQuotes // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > (i + 1) // there is indeed another character to check.
                && nextLine.charAt(i + 1) == quotechar;
    }

    /**
     * precondition: the current character is an escape
     *
     * @param nextLine the current line
     * @param inQuotes true if the current context is quoted
     * @param i        current index in line
     * @return true if the following character is a quote
     */
    protected boolean isNextCharacterEscapable(String nextLine, boolean inQuotes, int i) {
        return inQuotes // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > (i + 1) // there is indeed another character to check.
                && (nextLine.charAt(i + 1) == quotechar || nextLine.charAt(i + 1) == this.escape);
    }

    /**
     * precondition: sb.length() > 0
     *
     * @param sb A sequence of characters to examine
     * @return true if every character in the sequence is whitespace
     */
    protected boolean isAllWhiteSpace(CharSequence sb) {
        boolean result = true;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);

            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return result;
    }

    public boolean isHasHeader() {
        return hasHeader;
    }

    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public boolean isSkipHeader() {
        return skipHeader;
    }

    public void setSkipHeader(boolean skipHeader) {
        this.skipHeader = skipHeader;
    }

    public char getQuotechar() {
        return quotechar;
    }

    public void setQuotechar(char quotechar) {
        this.quotechar = quotechar;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isIgnoreQuotations() {
        return ignoreQuotations;
    }

    public void setIgnoreQuotations(boolean ignoreQuotations) {
        this.ignoreQuotations = ignoreQuotations;
    }

    public boolean isStrictQuotes() {
        return strictQuotes;
    }

    public void setStrictQuotes(boolean strictQuotes) {
        this.strictQuotes = strictQuotes;
    }

    public char getEscape() {
        return escape;
    }

    public void setEscape(char escape) {
        this.escape = escape;
    }

    public boolean isIgnoreLeadingWhiteSpace() {
        return ignoreLeadingWhiteSpace;
    }

    public void setIgnoreLeadingWhiteSpace(boolean ignoreLeadingWhiteSpace) {
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
    }

    /**
     * Conversion to FileObject might fail, so we have a backup BufferedReader
     *
     * @param file
     * @return
     * @throws IOException
     */
    private LineNumberReader getReader(File file) throws IOException {
        FileObject fileObject = FileUtil.toFileObject(file);
        if (fileObject == null) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            return new LineNumberReader(br);
        }
        return ImportUtils.getTextReader(fileObject);
    }

}