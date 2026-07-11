package com.matijakljajic.freeairradio.ui;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.matijakljajic.freeairradio.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Test
    public void tabsOpenExpectedFragmentRoots() {
        assertVisible(R.id.station_list_root);

        Espresso.onView(withId(R.id.nav_search_button)).perform(androidx.test.espresso.action.ViewActions.click());
        assertVisible(R.id.station_search_root);

        Espresso.onView(withId(R.id.nav_settings_button)).perform(androidx.test.espresso.action.ViewActions.click());
        assertVisible(R.id.settings_root);

        Espresso.onView(withId(R.id.nav_home_button)).perform(androidx.test.espresso.action.ViewActions.click());
        assertVisible(R.id.station_list_root);

    }

    private static void assertVisible(int viewId) {
        Espresso.onView(withId(viewId)).check(matches(isDisplayed()));
    }
}
