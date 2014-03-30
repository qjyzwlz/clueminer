package org.clueminer.importer.impl;

import org.clueminer.io.importer.api.AttributeDraft;
import org.clueminer.io.importer.api.InstanceDraft;

/**
 *
 * @author Tomas Barton
 */
public class InstanceDraftImpl implements InstanceDraft {

    private String id;
    private String label;
    private Object type;
    private Object[] values;
    private ImportContainerImpl container;

    public InstanceDraftImpl(ImportContainerImpl parent) {
        this.values = new Object[0];
        this.container = parent;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public void setType(Object type) {
        this.type = type;
    }

    @Override
    public Object getType() {
        return type;
    }

    @Override
    public Object getValue(String key) {
        return null;
    }

    @Override
    public void setValue(String key, Object value) {
        AttributeDraft attr = container.getAttribute(key, value.getClass());
        setValue(attr.getIndex(), value);
    }

    @Override
    public void setValue(int index, Object value) {
        if (index >= values.length) {
            Object[] newArray = new Object[index + 1];
            System.arraycopy(values, 0, newArray, 0, values.length);
            values = newArray;
        }
        values[index] = value;
    }

    @Override
    public Object getValue(int i) {
        return values[i];
    }

}