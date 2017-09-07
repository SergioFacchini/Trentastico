package com.geridea.trentastico.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object ColorDispenser {

    private val COLOR_INDEX_KEY  = "currentColorIndex"
    private val ASSOCIATIONS_KEY = "colorAssociationsJson"

    private lateinit var preferences: SharedPreferences

    private val typeColorAssociations: MutableMap<String, Int> = hashMapOf()
    private var currentColorIndex: Int = 0


    fun init(context: Context) {
        preferences = context.getSharedPreferences("ColorDispenser", Context.MODE_PRIVATE)

        //Loading index
        currentColorIndex = preferences.getInt(COLOR_INDEX_KEY, 0)

        //Loading associations
        val colorAssociationsJson = JSONObject(preferences.getString(ASSOCIATIONS_KEY, "{}"))
        colorAssociationsJson.keys().forEach {
            typeId -> typeColorAssociations.put(typeId, colorAssociationsJson.getInt(typeId))
        }

    }

    fun associateColorToTypeIfNeeded(lessonTypeId: String) {
        if(!typeColorAssociations.containsKey(lessonTypeId)){
            currentColorIndex = (currentColorIndex + 1) % teachingsColors.size
            typeColorAssociations.put(lessonTypeId, teachingsColors[currentColorIndex])

            save()
        }

    }

    private fun save() {
        preferences.edit().putInt(COLOR_INDEX_KEY, currentColorIndex).apply()

        val colorAssociationsString = JSONObject(typeColorAssociations).toString()
        preferences.edit().putString(ASSOCIATIONS_KEY, colorAssociationsString).apply()
    }

    fun getColor(lessonTypeId: String): Int =
            typeColorAssociations[lessonTypeId] ?:
                    throw RuntimeException("A color that is not associated has been requested!")


    fun dissociateColorFromType(lessonTypeId: String) {
        typeColorAssociations.remove(lessonTypeId)
        save()
    }

}

private val teachingsColors = arrayOf(
        0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(), 0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(),
        0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(), 0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(),
        0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(), 0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(),
        0xFF80D886.toInt(), 0xFFA6E2FF.toInt(), 0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(),
        0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(), 0xFFDC8080.toInt(), 0xFFFFB480.toInt(),
        0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(), 0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(),
        0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(), 0xFF56C878.toInt(), 0xFFE9D6B2.toInt(),
        0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(), 0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(),
        0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(), 0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(),
        0xFFBFCFCF.toInt(), 0xFF59B726.toInt(), 0xFFDE7083.toInt(), 0xFF68BBD9.toInt(),
        0xFFFF8147.toInt(), 0xFF87D21E.toInt(), 0xFF44D36F.toInt(), 0xFFCA9D00.toInt(),
        0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(), 0xFF00BD09.toInt(), 0xFF4EC300.toInt(),
        0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(), 0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(),
        0xFFFF8D01.toInt(), 0xFFCAC300.toInt(), 0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(),
        0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(), 0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(),
        0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(), 0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(),
        0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(), 0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(),
        0xFF29D984.toInt(), 0xFF99FF99.toInt(), 0xFF9999FF.toInt(), 0xFF47BAAD.toInt(),
        0xFFFF9FFA.toInt(), 0xFF00C161.toInt(), 0xFFB093FF.toInt(), 0xFF57B3FF.toInt(),
        0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(), 0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(),
        0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(), 0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(),
        0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(), 0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(),
        0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(), 0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(),
        0xFFFFA3FF.toInt(), 0xFFC5FFB1.toInt(), 0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(),
        0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(), 0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(),
        0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(), 0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(),
        0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(), 0xFF80D886.toInt(), 0xFFA6E2FF.toInt(),
        0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(), 0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(),
        0xFFDC8080.toInt(), 0xFFFFB480.toInt(), 0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(),
        0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(), 0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(),
        0xFF56C878.toInt(), 0xFFE9D6B2.toInt(), 0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(),
        0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(), 0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(),
        0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(), 0xFFBFCFCF.toInt(), 0xFF59B726.toInt(),
        0xFFDE7083.toInt(), 0xFF68BBD9.toInt(), 0xFFFF8147.toInt(), 0xFF87D21E.toInt(),
        0xFF44D36F.toInt(), 0xFFCA9D00.toInt(), 0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(),
        0xFF00BD09.toInt(), 0xFF4EC300.toInt(), 0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(),
        0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(), 0xFFFF8D01.toInt(), 0xFFCAC300.toInt(),
        0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(), 0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(),
        0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(), 0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(),
        0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(), 0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(),
        0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(), 0xFF29D984.toInt(), 0xFF99FF99.toInt(),
        0xFF9999FF.toInt(), 0xFF47BAAD.toInt(), 0xFFFF9FFA.toInt(), 0xFF00C161.toInt(),
        0xFFB093FF.toInt(), 0xFF57B3FF.toInt(), 0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(),
        0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(), 0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(),
        0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(), 0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(),
        0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(), 0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(),
        0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(), 0xFFFFA3FF.toInt(), 0xFFC5FFB1.toInt(),
        0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(), 0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(),
        0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(), 0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(),
        0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(), 0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(),
        0xFF80D886.toInt(), 0xFFA6E2FF.toInt(), 0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(),
        0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(), 0xFFDC8080.toInt(), 0xFFFFB480.toInt(),
        0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(), 0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(),
        0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(), 0xFF56C878.toInt(), 0xFFE9D6B2.toInt(),
        0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(), 0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(),
        0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(), 0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(),
        0xFFBFCFCF.toInt(), 0xFF59B726.toInt(), 0xFFDE7083.toInt(), 0xFF68BBD9.toInt(),
        0xFFFF8147.toInt(), 0xFF87D21E.toInt(), 0xFF44D36F.toInt(), 0xFFCA9D00.toInt(),
        0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(), 0xFF00BD09.toInt(), 0xFF4EC300.toInt(),
        0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(), 0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(),
        0xFFFF8D01.toInt(), 0xFFCAC300.toInt(), 0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(),
        0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(), 0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(),
        0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(), 0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(),
        0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(), 0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(),
        0xFF29D984.toInt(), 0xFF99FF99.toInt(), 0xFF9999FF.toInt(), 0xFF47BAAD.toInt(),
        0xFFFF9FFA.toInt(), 0xFF00C161.toInt(), 0xFFB093FF.toInt(), 0xFF57B3FF.toInt(),
        0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(), 0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(),
        0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(), 0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(),
        0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(), 0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(),
        0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(), 0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(),
        0xFFFFA3FF.toInt(), 0xFFC5FFB1.toInt(), 0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(),
        0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(), 0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(),
        0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(), 0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(),
        0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(), 0xFF80D886.toInt(), 0xFFA6E2FF.toInt(),
        0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(), 0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(),
        0xFFDC8080.toInt(), 0xFFFFB480.toInt(), 0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(),
        0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(), 0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(),
        0xFF56C878.toInt(), 0xFFE9D6B2.toInt(), 0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(),
        0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(), 0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(),
        0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(), 0xFFBFCFCF.toInt(), 0xFF59B726.toInt(),
        0xFFDE7083.toInt(), 0xFF68BBD9.toInt(), 0xFFFF8147.toInt(), 0xFF87D21E.toInt(),
        0xFF44D36F.toInt(), 0xFFCA9D00.toInt(), 0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(),
        0xFF00BD09.toInt(), 0xFF4EC300.toInt(), 0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(),
        0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(), 0xFFFF8D01.toInt(), 0xFFCAC300.toInt(),
        0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(), 0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(),
        0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(), 0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(),
        0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(), 0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(),
        0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(), 0xFF29D984.toInt(), 0xFF99FF99.toInt(),
        0xFF9999FF.toInt(), 0xFF47BAAD.toInt(), 0xFFFF9FFA.toInt(), 0xFF00C161.toInt(),
        0xFFB093FF.toInt(), 0xFF57B3FF.toInt(), 0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(),
        0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(), 0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(),
        0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(), 0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(),
        0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(), 0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(),
        0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(), 0xFFFFA3FF.toInt()
)
