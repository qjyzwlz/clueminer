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
package org.clueminer.processor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
import org.clueminer.fixtures.TimeseriesFixture;
import org.clueminer.importer.impl.CsvImporter;
import org.clueminer.importer.impl.ImportContainerImpl;
import org.clueminer.importer.impl.ImportUtils;
import org.clueminer.io.importer.api.Container;
import org.clueminer.io.importer.api.ContainerLoader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author deric
 */
public class TimeseriesProcessorTest {

    private final CsvImporter csv = new CsvImporter();
    private final TimeseriesProcessor subject;

    public TimeseriesProcessorTest() {
        subject = new TimeseriesProcessor();
    }

    @Test
    public void testTimeSeries() throws IOException {
        TimeseriesFixture tf = new TimeseriesFixture();
        File tsFile = tf.ap01();
        Container container = new ImportContainerImpl();
        csv.execute(container, tsFile);
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(tsFile.getAbsolutePath()));
        Reader reader = ImportUtils.getTextReader(stream);
        //run import
        csv.execute(container, reader);
        ContainerLoader loader = container.getLoader();

        subject.setContainer(loader);
        //convert preloaded data to a real dataset
        subject.run();

        //name of relation from ARFF
        Dataset<? extends Instance> dataset = loader.getDataset();
        //assertEquals("AP01", dataset.getName());
        /**
         * TODO: fix creating attributes
         */
        assertEquals(15, dataset.attributeCount());
        assertEquals(1536, dataset.size());
        //there are 4 classes in the dataset
        assertNotNull(loader.getDataset());
        //assertEquals(4, loader.getDataset().getClasses().size());
    }

}
