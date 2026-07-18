package com.matijakljajic.freeairradio.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.List;

public final class FavoriteStationNavigator {

    public boolean canNavigate(@Nullable Station currentStation,
                               @NonNull List<Station> favoriteStations) {
        return favoriteStations.size() > 1 && findCurrentIndex(currentStation, favoriteStations) >= 0;
    }

    @Nullable
    public Station getPrevious(@Nullable Station currentStation,
                               @NonNull List<Station> favoriteStations) {
        int currentIndex = findCurrentIndex(currentStation, favoriteStations);
        if (favoriteStations.size() <= 1 || currentIndex < 0) {
            return null;
        }

        int previousIndex = currentIndex == 0
                ? favoriteStations.size() - 1
                : currentIndex - 1;
        return favoriteStations.get(previousIndex);
    }

    @Nullable
    public Station getNext(@Nullable Station currentStation,
                           @NonNull List<Station> favoriteStations) {
        int currentIndex = findCurrentIndex(currentStation, favoriteStations);
        if (favoriteStations.size() <= 1 || currentIndex < 0) {
            return null;
        }

        int nextIndex = currentIndex == favoriteStations.size() - 1
                ? 0
                : currentIndex + 1;
        return favoriteStations.get(nextIndex);
    }

    private int findCurrentIndex(@Nullable Station currentStation,
                                 @NonNull List<Station> favoriteStations) {
        if (currentStation == null) {
            return -1;
        }

        for (int index = 0; index < favoriteStations.size(); index++) {
            if (currentStation.getId().equals(favoriteStations.get(index).getId())) {
                return index;
            }
        }
        return -1;
    }
}
