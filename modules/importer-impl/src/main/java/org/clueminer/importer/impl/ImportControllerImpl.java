package org.clueminer.importer.impl;

import eu.medsea.mimeutil.MimeUtil2;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.clueminer.importer.FileImporterFactory;
import org.clueminer.types.FileType;
import org.clueminer.importer.ImportController;
import org.clueminer.io.importer.api.Container;
import org.clueminer.io.importer.api.Database;
import org.clueminer.io.importer.api.Report;
import org.clueminer.processor.spi.Processor;
import org.clueminer.project.api.Workspace;
import org.clueminer.spi.DatabaseImporter;
import org.clueminer.spi.FileImporter;
import org.clueminer.spi.Importer;
import org.clueminer.spi.ImporterUI;
import org.clueminer.spi.ImporterWizardUI;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Barton
 */
@ServiceProvider(service = ImportController.class)
public class ImportControllerImpl implements ImportController {

    private final List<FileImporter> fileImporters;
    private final ImporterUI[] uis;
    private MimeHelper helper;
    private static final Logger log = Logger.getLogger(ImportControllerImpl.class.getName());
    private final HashMap<String, Container> containers;

    public ImportControllerImpl() {
        this.containers = new HashMap<>();
        fileImporters = FileImporterFactory.getInstance().getAll();
        helper = new MimeHelper();
        //Get UIS
        uis = Lookup.getDefault().lookupAll(ImporterUI.class).toArray(new ImporterUI[0]);
        log.info("creating new ImportController");
    }

    @Override
    public Container importFile(File file) throws FileNotFoundException {
        FileObject fileObject = FileUtil.toFileObject(file);
        if (fileObject != null) {
            fileObject = getArchivedFile(fileObject);   //Unzip and return content file
            FileImporter importer = getMatchingImporter(fileObject);
            if (importer == null) {
                //try to find importer by MIME type
                importer = getMatchingImporter(helper.detectMIME(file));
            }
            if (fileObject != null && importer != null) {
                Container c = importFile(fileObject, fileObject.getInputStream(), importer, false);
                if (fileObject.getPath().startsWith(System.getProperty("java.io.tmpdir"))) {
                    try {
                        fileObject.delete();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                return c;
            }
        }
        return null;
    }

    @Override
    public Container importFile(File file, FileImporter importer) throws FileNotFoundException {
        FileObject fileObject = FileUtil.toFileObject(file);
        if (fileObject != null) {
            fileObject = getArchivedFile(fileObject);   //Unzip and return content file
            if (fileObject != null) {
                Container c = importFile(fileObject, fileObject.getInputStream(), importer, false);
                if (fileObject.getPath().startsWith(System.getProperty("java.io.tmpdir"))) {
                    try {
                        fileObject.delete();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                return c;
            }
        }
        return null;
    }

    @Override
    public Container importFile(FileObject file, Reader reader, FileImporter importer, boolean reload) {
        //Create Container
        Container container;
        String path = file.getPath();
        log.log(Level.INFO, "reload {0}, file: {1}", new Object[]{reload, path});
        // unique container for path
        if (containers.containsKey(path)) {
            container = containers.get(path);
        } else {
            log.log(Level.INFO, "did not find container for {0}, size = {1}", new Object[]{path, containers.size()});
            container = new ImportContainerImpl();
            containers.put(path, container);
            container.setFile(file);
        }

        //container = Lookup.getDefault().lookup(Container.class);
        log.log(Level.INFO, "importer contr num attr: {0}", container.getLoader().getAttributeCount());
        log.log(Level.INFO, "importer contr num inst: {0}", container.getLoader().getInstanceCount());
        //Report
        Report report = new Report();
        container.setReport(report);

        try {
            if (importer.execute(container, reader)) {
                if (importer.getReport() != null) {
                    report.append(importer.getReport());
                }
                return container;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    @Override
    public Container importFile(FileObject file, InputStream stream, FileImporter importer, boolean reload) {
        try {
            Reader reader = ImportUtils.getTextReader(stream);
            return importFile(file, reader, importer, reload);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public FileImporter getFileImporter(File file) {
        FileObject fileObject = FileUtil.toFileObject(file);
        fileObject = getArchivedFile(fileObject);   //Unzip and return content file
        //try to get importer by an extension
        FileImporter fi = getMatchingImporter(fileObject);
        if (fi == null) {
            //try to find importer by MIME type
            fi = getMatchingImporter(helper.detectMIME(file));
        }
        if (fileObject != null && fi != null) {
            if (fileObject.getPath().startsWith(System.getProperty("java.io.tmpdir"))) {
                try {
                    fileObject.delete();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            return fi;
        }
        return null;
    }

    @Override
    public FileImporter getFileImporter(String importerName) {
        FileImporter builder = getMatchingImporter(importerName);
        if (builder != null) {
            return builder;
        }
        return null;
    }

    @Override
    public void process(Container container) {
        Processor processor = Lookup.getDefault().lookup(Processor.class);
        if (processor == null) {
            throw new RuntimeException("Impossible to find Default Processor");
        }
        process(container, processor, null);
    }

    @Override
    public void process(Container container, Processor processor, Workspace workspace) {
        container.closeLoader();
        processor.setContainer(container.getLoader());
        processor.setWorkspace(workspace);
        processor.process();
    }

    @Override
    public FileType[] getFileTypes() {
        ArrayList<FileType> list = new ArrayList<FileType>();
        for (FileImporter im : fileImporters) {
            list.addAll(Arrays.asList(im.getFileTypes()));
        }
        return list.toArray(new FileType[0]);
    }

    @Override
    public boolean isFileSupported(File file) {
        FileObject fileObject = FileUtil.toFileObject(file);
        for (FileImporter im : fileImporters) {
            if (im.isMatchingImporter(fileObject)) {
                return true;
            }
        }
        return fileObject.getExt().equalsIgnoreCase("zip")
                || fileObject.getExt().equalsIgnoreCase("gz")
                || fileObject.getExt().equalsIgnoreCase("bz2");
    }

    @Override
    public boolean isAccepting(File file) {
        Collection mimeTypes = helper.detectMIME(file);
        for (FileImporter im : fileImporters) {
            if (im.isAccepting(mimeTypes)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ImporterUI getUI(Importer importer) {
        for (ImporterUI ui : uis) {
            if (ui.isUIForImporter(importer)) {
                return ui;
            }
        }
        return null;
    }

    @Override
    public ImporterWizardUI getWizardUI(Importer importer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Container importDatabase(Database database, DatabaseImporter importer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private FileObject getArchivedFile(FileObject fileObject) {
        if (fileObject == null) {
            return null;
        }
        // ZIP and JAR archives
        if (FileUtil.isArchiveFile(fileObject)) {
            fileObject = FileUtil.getArchiveRoot(fileObject).getChildren()[0];
        } else { // GZ or BZIP2 archives
            boolean isGz = fileObject.getExt().equalsIgnoreCase("gz");
            boolean isBzip = fileObject.getExt().equalsIgnoreCase("bz2");
            if (isGz || isBzip) {
                try {
                    String[] splittedFileName = fileObject.getName().split("\\.");
                    if (splittedFileName.length < 2) {
                        return fileObject;
                    }

                    String fileExt1 = splittedFileName[splittedFileName.length - 1];
                    String fileExt2 = splittedFileName[splittedFileName.length - 2];

                    File tempFile;
                    if (fileExt1.equalsIgnoreCase("tar")) {
                        String fname = fileObject.getName().replaceAll("\\.tar$", "");
                        fname = fname.replace(fileExt2, "");
                        tempFile = File.createTempFile(fname, "." + fileExt2);
                        // Untar & unzip
                        if (isGz) {
                            tempFile = ImportUtils.getGzFile(fileObject, tempFile, true);
                        } else {
                            tempFile = ImportUtils.getBzipFile(fileObject, tempFile, true);
                        }
                    } else {
                        String fname = fileObject.getName();
                        fname = fname.replace(fileExt1, "");
                        tempFile = File.createTempFile(fname, "." + fileExt1);
                        // Unzip
                        if (isGz) {
                            tempFile = ImportUtils.getGzFile(fileObject, tempFile, false);
                        } else {
                            tempFile = ImportUtils.getBzipFile(fileObject, tempFile, false);
                        }
                    }
                    tempFile.deleteOnExit();
                    tempFile = FileUtil.normalizeFile(tempFile);
                    fileObject = FileUtil.toFileObject(tempFile);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return fileObject;
    }

    private FileImporter getMatchingImporter(FileObject fileObject) {
        if (fileObject == null) {
            return null;
        }
        for (FileImporter im : fileImporters) {
            if (im.isMatchingImporter(fileObject)) {
                return im;
            }
        }
        return null;
    }

    protected FileImporter getMatchingImporter(Collection mimeTypes) {
        if (mimeTypes == null || mimeTypes.isEmpty()) {
            return null;
        }
        for (FileImporter im : fileImporters) {
            if (im.isAccepting(mimeTypes)) {
                return im;
            }
        }
        return null;
    }

    private FileImporter getMatchingImporter(String extension) {
        for (FileImporter im : fileImporters) {
            for (FileType ft : im.getFileTypes()) {
                for (String ext : ft.getExtensions()) {
                    if (ext.equalsIgnoreCase(extension)) {
                        return im;
                    }
                }
            }
        }
        return null;
    }
}
