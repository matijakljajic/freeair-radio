package com.matijakljajic.freeairradio.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.List;

public final class FavoriteStationNavigator {

    public boolean canNavigate(@Nullable Station currentStation,
                               @NonNull List<Station> favoriteStations) {
        return findNavigableIndex(currentStation, favoriteStations) >= 0;
    }

    @Nullable
    public Station getPrevious(@Nullable Station currentStation,
                               @NonNull List<Station> favoriteStations) {
        return getOffsetStation(currentStation, favoriteStations, -1);
    }

    @Nullable
    public Station getNext(@Nullable Station currentStation,
                           @NonNull List<Station> favoriteStations) {
        return getOffsetStation(currentStation, favoriteStations, 1);
    }

    @Nullable
    private Station getOffsetStation(@Nullable Station currentStation,
                                     @NonNull List<Station> favoriteStations,
                                     int offset) {
        int currentIndex = findNavigableIndex(currentStation, favoriteStations);
        if (currentIndex < 0) {
            return null;
        }

        int targetIndex = (currentIndex + offset + favoriteStations.size()) % favoriteStations.size();
        return favoriteStations.get(targetIndex);
    }

    private int findNavigableIndex(@Nullable Station currentStation,
                                   @NonNull List<Station> favoriteStations) {
        if (currentStation == null || favoriteStations.size() <= 1) {
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
