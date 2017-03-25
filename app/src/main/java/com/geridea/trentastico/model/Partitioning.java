package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class Partitioning {

    public static final Partitioning NONE = new Partitioning(PartitioningType.NONE, "");

    private PartitioningType type;

    private final HashSet<PartitioningCase> cases = new HashSet<>(6);

    public Partitioning(PartitioningType type, String aCase) {
        this.type = type;

        addPartitionCase(aCase);
    }

    private boolean addPartitionCase(String aCase) {
        return cases.add(new PartitioningCase(aCase, true));
    }

    private boolean addPartitionCase(PartitioningCase aCase) {
        return cases.add(aCase);
    }

    public Partitioning(PartitioningType type) {
        this.type = type;
    }

    public void mergePartitionCases(Partitioning anotherPartitioning) {
        cases.addAll(anotherPartitioning.getCases());
    }

    public Collection<PartitioningCase> getCases() {
        return cases;
    }

    public Collection<PartitioningCase> getSortedCases() {
        ArrayList<PartitioningCase> cases = new ArrayList<>(getCases());
        Collections.sort(cases);
        return cases;
    }

    public PartitioningType getType() {
        return type;
    }

    public int getPartitioningCasesSize() {
        return cases.size();
    }

    public int getNumVisiblePartitioningCases() {
        int numVisible = 0;
        for (PartitioningCase aCase : cases) {
            if (aCase.isVisible()) {
                numVisible++;
            }
        }
        return numVisible;
    }

    public void hidePartitioningsInList(ArrayList<String> partitioningsToHide) {
        for (PartitioningCase aCase : cases) {
            for (String partitioningCaseToHide : partitioningsToHide) {
                if (aCase.getCase().equals(partitioningCaseToHide)) {
                    aCase.setVisible(false);
                    break;
                }
            }
        }

    }

    public boolean applyVisibilityToPartitionings(boolean newVisibility) {
        boolean somethingChanged = false;

        for (PartitioningCase aCase : getCases()) {
            if (aCase.isVisible() != newVisibility) {
                aCase.setVisible(newVisibility);
                somethingChanged = true;
            }
        }

        return somethingChanged;
    }

    public boolean hasAllPartitioningsInvisible() {
        boolean allInvisible = true;
        for (PartitioningCase aCase : getCases()) {
            if (aCase.isVisible()) {
                allInvisible = false;
                break;
            }
        }

        return allInvisible;
    }

    public boolean hasAtLeastOnePartitioningVisible() {
        boolean hasOneVisible = false;
        for (PartitioningCase aCase : getCases()) {
            if (aCase.isVisible()) {
                hasOneVisible = true;
                break;
            }
        }

        return hasOneVisible;

    }
}
