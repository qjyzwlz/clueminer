package org.clueminer.importer.impl;

import org.clueminer.io.importer.api.AttributeDraft;

/**
 *
 * @author Tomas Barton
 */
public class AttributeDraftImpl implements AttributeDraft {

    private String name;
    private boolean meta;
    private boolean unique;
    private boolean numerical;
    private Object type;
    private Object defaultValue;

    public AttributeDraftImpl() {

    }

    public AttributeDraftImpl(String name) {
        this.name = name;
    }

    @Override
    public void setType(Object t) {
        this.type = t;
    }

    @Override
    public Object getType() {
        return type;
    }

    @Override
    public boolean isMeta() {
        return meta;
    }

    @Override
    public void setMeta(boolean b) {
        this.meta = b;
    }

    @Override
    public boolean isNumerical() {
        return numerical;
    }

    @Override
    public void setNumerical(boolean b) {
        this.numerical = b;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    @Override
    public void setUnique(boolean b) {
        this.unique = b;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object value) {
        this.defaultValue = value;
    }

}
