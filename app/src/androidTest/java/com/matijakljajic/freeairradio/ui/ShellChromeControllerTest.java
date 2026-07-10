package com.matijakljajic.freeairradio.ui;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.matijakljajic.freeairradio.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ShellChromeControllerTest {

    @Test
    public void searchChromeAppearsOnlyForSearchTab() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            final int[] homeTopFilterHeight = new int[1];
            final int[] searchTopFilterHeight = new int[1];

            scenario.onActivity(activity -> {
                ShellChromeController controller = activity.getShellChromeController();
                assertNotNull(controller);
                homeTopFilterHeight[0] = assertChromeState(activity, View.GONE);
            });

            Espresso.onView(withId(R.id.nav_search_button)).perform(androidx.test.espresso.action.ViewActions.click());
            Espresso.onView(withId(R.id.station_search_shell)).check(matches(isDisplayed()));

            scenario.onActivity(activity -> {
                ShellChromeController controller = activity.getShellChromeController();
                assertNotNull(controller);
                searchTopFilterHeight[0] = assertChromeState(activity, View.VISIBLE);
                assertTrue(searchTopFilterHeight[0] > homeTopFilterHeight[0]);
            });

            Espresso.onView(withId(R.id.nav_home_button)).perform(androidx.test.espresso.action.ViewActions.click());
            Espresso.onView(withId(R.id.station_search_shell)).check(matches(androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(GONE)));

            scenario.onActivity(activity -> assertChromeState(activity, View.GONE));
        }
    }

    @Test
    public void settingsContentUsesShellPadding() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.nav_settings_button)).perform(androidx.test.espresso.action.ViewActions.click());

            Espresso.onView(withId(R.id.settings_root)).check(matches(isDisplayed()));

            scenario.onActivity(activity -> {
                View settingsRoot = activity.findViewById(R.id.settings_root);
                View statusBarFilter = activity.findViewById(R.id.status_bar_filter);
                View bottomFilter = activity.findViewById(R.id.bottom_content_filter);

                assertNotNull(settingsRoot);
                assertNotNull(statusBarFilter);
                assertNotNull(bottomFilter);
                assertTrue(settingsRoot.getPaddingTop() > 0);
                assertTrue(settingsRoot.getPaddingBottom() > 0);
            });
        }
    }

    private static int assertChromeState(MainActivity activity, int expectedSearchVisibility) {
        View searchShell = activity.findViewById(R.id.station_search_shell);
        View statusBarFilter = activity.findViewById(R.id.status_bar_filter);
        View bottomFilter = activity.findViewById(R.id.bottom_content_filter);

        assertNotNull(searchShell);
        assertNotNull(statusBarFilter);
        assertNotNull(bottomFilter);

        assertEquals(searchShell.getVisibility(), expectedSearchVisibility);
        assertEquals(View.VISIBLE, bottomFilter.getVisibility());
        assertTrue(bottomFilter.getHeight() > 0);
        assertTrue(statusBarFilter.getHeight() >= 0);
        return statusBarFilter.getHeight();
    }
}
