package com.geridea.trentastico.model;

import com.geridea.trentastico.model.cache.CachedLessonType;
import com.geridea.trentastico.utils.AppPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class LessonType {

    private int id;
    private String name;
    private int color;

    private boolean isVisible;

    public LessonType() { }

    public LessonType(CachedLessonType cachedLessonType, boolean isVisible) {
        this.id    = cachedLessonType.getLesson_type_id();
        this.name  = cachedLessonType.getName();
        this.color = cachedLessonType.getColor();
        this.isVisible = isVisible;
    }

    public static LessonType fromJson(JSONObject jsonObject) throws JSONException {
        LessonType lessonType = new LessonType();
        lessonType.setId(jsonObject.getInt("IdADfisica"));
        lessonType.setName(jsonObject.getString("DescrizioneAD"));

        String cssName = jsonObject.getString("Css");
        lessonType.setColor(getColorFromCSSStyle(cssName));
        lessonType.setVisible(!AppPreferences.hasLessonTypeWithIdHidden(lessonType.getId()));

        return lessonType;
    }

    private void setId(int id) {
        this.id = id;
    }

    public static int getColorFromCSSStyle(String text) {
        switch(text) {
            case "colore1":  return 0xFFFFEFAA;
            case "colore2":  return 0xFFFFF9AA;
            case "colore3":  return 0xFFFAFFAA;
            case "colore4":  return 0xFFF0FFAA;
            case "colore5":  return 0xFFE7FFAA;
            case "colore6":  return 0xFFDDFFAA;
            case "colore7":  return 0xFFD3FFAA;
            case "colore8":  return 0xFFC9FFAA;
            case "colore9":  return 0xFFBFFFAA;
            case "colore10": return 0xFFB6FFAA;
            case "colore11": return 0xFFACFFAA;
            case "colore12": return 0xFFAAFFBD;
            case "colore13": return 0xFFAAFFC6;
            case "colore14": return 0xFFAAFFD0;
            case "colore15": return 0xFFAAFFDA;
            case "colore16": return 0xFFAAFFE4;
            case "colore17": return 0xFFAAFFEE;
            case "colore18": return 0xFFAAFFF7;
            case "colore19": return 0xFFAAFCFF;
            case "colore20": return 0xFFAAF2FF;
            case "colore21": return 0xFFAAE8FF;
            case "colore22": return 0xFFAADEFF;
            case "colore23": return 0xFFAAD4FF;
            case "colore24": return 0xFFAACBFF;
            case "colore25": return 0xFFAAC1FF;
            case "colore26": return 0xFFAAB7FF;
            case "colore27": return 0xFFAAADFF;
            case "colore28": return 0xFFB1AAFF;
            case "colore29": return 0xFFBBAAFF;
            case "colore30": return 0xFFC5AAFF;
            case "colore31": return 0xFFCFAAFF;
            case "colore32": return 0xFFD9AAFF;
            case "colore33": return 0xFFE2AAFF;
            case "colore34": return 0xFFECAAFF;
            case "colore35": return 0xFFF6AAFF;
            case "colore36": return 0xFFFFAAFD;
            case "colore37": return 0xFFFFAAF3;
            case "colore38": return 0xFFFFAAE9;
            case "colore39": return 0xFFFFAAE0;
            case "colore40": return 0xFFFFAAD6;
            case "colore41": return 0xFFFFAACC;
            case "colore42": return 0xFFFFAAC2;
            case "colore43": return 0xFFFFAAB8;
            case "colore44": return 0xFFFFAAAA;
            case "colore45": return 0xFFFFB4AA;
            case "colore46": return 0xFFFFBEAA;
            case "colore47": return 0xFFFFC8AA;
            case "colore48": return 0xFFFFD2AA;
            case "colore49": return 0xFFFFDBAA;
            case "colore50": return 0xFFFFE5AA;
        }

        return 0xFFFFFFFF;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

}
