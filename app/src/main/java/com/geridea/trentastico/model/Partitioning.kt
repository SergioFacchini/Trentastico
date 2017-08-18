package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

import java.util.*

class Partitioning {

    var type: PartitioningType? = null
        private set

    val cases = HashSet<PartitioningCase>(6)

    constructor(type: PartitioningType, aCase: String) {
        this.type = type

        addPartitionCase(aCase)
    }

    private fun addPartitionCase(aCase: String): Boolean = cases.add(PartitioningCase(aCase, true))

    private fun addPartitionCase(aCase: PartitioningCase): Boolean = cases.add(aCase)

    constructor(type: PartitioningType) {
        this.type = type
    }

    fun mergePartitionCases(anotherPartitioning: Partitioning) {
        if (anotherPartitioning.type != PartitioningType.NONE) {
            cases.addAll(anotherPartitioning.getCases())
        }
    }

    fun getCases(): Collection<PartitioningCase> = cases

    val sortedCases: Collection<PartitioningCase>
        get() {
            val cases = ArrayList(getCases())
            Collections.sort(cases)
            return cases
        }

    val partitioningCasesSize: Int
        get() = cases.size

    val numVisiblePartitioningCases: Int
        get() {
            var numVisible = 0
            for (aCase in cases) {
                if (aCase.isVisible) {
                    numVisible++
                }
            }
            return numVisible
        }

    fun hidePartitioningsInList(partitioningsToHide: ArrayList<String>) {
        for (aCase in cases) {
            for (partitioningCaseToHide in partitioningsToHide) {
                if (aCase.case == partitioningCaseToHide) {
                    aCase.isVisible = false
                    break
                }
            }
        }

    }

    fun applyVisibilityToPartitionings(newVisibility: Boolean): Boolean {
        var somethingChanged = false

        for (aCase in getCases()) {
            if (aCase.isVisible != newVisibility) {
                aCase.isVisible = newVisibility
                somethingChanged = true
            }
        }

        return somethingChanged
    }

    fun hasAllPartitioningsInvisible(): Boolean {
        var allInvisible = true
        for (aCase in getCases()) {
            if (aCase.isVisible) {
                allInvisible = false
                break
            }
        }

        return allInvisible
    }

    fun hasAtLeastOnePartitioningVisible(): Boolean {
        var hasOneVisible = false
        for (aCase in getCases()) {
            if (aCase.isVisible) {
                hasOneVisible = true
                break
            }
        }

        return hasOneVisible

    }

    fun hasMoreThanOneCase(): Boolean = getCases().size > 1

    companion object {

        val NONE = Partitioning(PartitioningType.NONE, "")
    }
}
