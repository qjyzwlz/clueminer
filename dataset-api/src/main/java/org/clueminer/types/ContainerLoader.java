package org.clueminer.types;

import org.clueminer.dataset.api.Dataset;
import org.clueminer.dataset.api.Instance;
/**
 *
 * @author Tomas Barton
 */
public interface ContainerLoader {

    public void setDataset(Dataset<? extends Instance> dataset);

    public Dataset<? extends Instance> getDataset();

    /**
     * Text representation of source
     *
     * @return
     */
    public String getSource();

    /* public void addInstance(Instance instance);

     public void addAttribute(Attribute attr);*/
}
