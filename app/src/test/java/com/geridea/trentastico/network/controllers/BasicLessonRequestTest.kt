package com.geridea.trentastico.network.controllers

import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.request.RequestSender
import org.junit.Assert
import org.junit.Test


/*
 * Created with ♥ by Slava on 25/09/2017.
 */
class BasicLessonRequestTest {

    @Test
    fun `partitioning name is null when there are no dashes`() {
        Assert.assertNull(calculatePartitioning("Analisi I"))
    }

    @Test
    fun `partitioning name is present after dashes`() {
        assertPartitioningEquals("pari", "Analisi I - pari")
        assertPartitioningEquals("dispari", "Analisi I - dispari")
        assertPartitioningEquals("mod1", "Geometria A - mod1")
    }

    @Test
    fun `partitioning name is taking only last dash`() {
        assertPartitioningEquals("Pari", "Traduzioni Italiano - Russo - Pari")
    }

    @Test
    fun `partitioning name dashes without spaces`() {
        //Mediazione 1° anno
        assertPartitioningEquals(null, "Valorizzazione aristico-culturale")
    }

    @Test
    fun `partitioning name expection for "Interazione uomo - macchina`() {
        assertPartitioningEquals(null, "Interazione uomo - macchina")
    }

    @Test
    fun `partitioning name expection for "Interazione uomo-macchina`() {
        assertPartitioningEquals(null, "Interazione uomo-macchina")
    }

    @Test
    fun `partitioning name _LEZ _ESE _LAB`() {
        //Matematica - 1° anno
        assertPartitioningEquals("LEZ", "Informatica_LEZ")
        assertPartitioningEquals("ESE", "Informatica_ESE")
        assertPartitioningEquals("LAB", "Informatica_LAB")
    }

    @Test
    fun `partitioning name with dashed and _LEZ* should start after dash`() {
        //Matematica - 1° anno
        assertPartitioningEquals("mod1_LEZ", "Analisi matematica A - mod1_LEZ")
        assertPartitioningEquals("mod1_ESE", "Analisi matematica A - mod1_ESE")
        assertPartitioningEquals("mod1_LAB", "Analisi matematica A - mod1_LAB")
    }

    @Test
    fun `partitioning name ending with LT, LM, N0`() {
        //Filosofia - 1° anno politica
        assertPartitioningEquals(null, "Pensiero Ebraico I - LM")
        assertPartitioningEquals(null, "Pensiero Ebraico I - LT")
        assertPartitioningEquals(null, "Pensiero Ebraico I - n0")
    }

    private fun assertPartitioningEquals(expected: String?, teachingName: String) {
        Assert.assertEquals(expected, calculatePartitioning(teachingName))
    }

    private fun calculatePartitioning(teaching: String) = TestBasicLessonRequest().calculatePartitioningName(teaching)

    internal class TestBasicLessonRequest : BasicLessonRequest(dummyStudyCourse) {

        public override fun calculatePartitioningName(teachingName: String): String? =
                super.calculatePartitioningName(teachingName)

        override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
            //Not tested
        }

        override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
            //Not tested
        }

        override fun onTeachingsAndLessonsLoaded(
                lessonTypes: List<LessonType>,
                loadedLessons: List<LessonSchedule>) {
            //Not tested
        }

    }

}

private val dummyStudyCourse
        = StudyCourse("courseId", "courseName", "yearId", "yeatName")