package com.geridea.trentastico.gui.activities;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.geridea.trentastico.services.LessonsUpdaterService;
import com.geridea.trentastico.services.NextLessonNotificationService;
import com.geridea.trentastico.utils.AppPreferences;

/**
 * Chooses the right activity to start the application with.
 */
public class FirstActivityChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(LessonsUpdaterService.createServiceIntent(this, LessonsUpdaterService.STARTER_APP_START));
        startService(NextLessonNotificationService.createIntent(this, NextLessonNotificationService.STARTER_APP_BOOT));

        if (AppPreferences.isFirstRun()) {
            startActivity(new Intent(this, WelcomeActivity.class));
        } else {
            startActivity(new Intent(this, HomeActivity.class));
        }

        finish();
    }
}
