package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 25/03/2017.
 */

import android.support.annotation.NonNull;

public final class PartitioningCase implements Comparable<PartitioningCase> {

    private final String caseString;
    private boolean isVisible;

    PartitioningCase(String caseString, boolean isVisible) {
        this.caseString = caseString;
        this.isVisible = isVisible;
    }

    public String getCase() {
        return caseString;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    @Override
    public int hashCode() {
        return caseString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PartitioningCase) {
            PartitioningCase aCase = (PartitioningCase) obj;
            return aCase.getCase().equals(getCase());
        }

        return false;
    }

    @Override
    public int compareTo(@NonNull PartitioningCase aCase) {
        return this.caseString.compareTo(aCase.caseString);
    }

    public PartitioningCase copy() {
        return new PartitioningCase(caseString, isVisible);
    }
}
