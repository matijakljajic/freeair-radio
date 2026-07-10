package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.dto.RadioBrowserStationDto;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerSelector;
import com.matijakljajic.freeairradio.data.repository.StationRepository;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RadioBrowserRepositoryTest {

    private final Gson gson = new Gson();

    @Test
    public void mapStationMapsFieldsAndNormalizesUnknownValues() {
        RadioBrowserStationDto dto = gson.fromJson("{"
                + "\"stationuuid\":\"uuid-1\","
                + "\"name\":\" Test Station \","
                + "\"url\":\" https://example.com/stream \","
                + "\"url_resolved\":\" https://example.com/resolved \","
                + "\"homepage\":\" https://example.com \","
                + "\"favicon\":\" https://example.com/favicon.png \","
                + "\"country\":\" unknown country \","
                + "\"countrycode\":\" \","
                + "\"language\":\" Serbian \","
                + "\"tags\":\" jazz,rock \","
                + "\"codec\":\" MP3 \","
                + "\"bitrate\":128,"
                + "\"hls\":true"
                + "}", RadioBrowserStationDto.class);

        Station station = RadioBrowserRepository.mapStation(dto);

        assertEquals("RADIO_BROWSER:uuid-1", station.getId());
        assertEquals("Test Station", station.getName());
        assertEquals("https://example.com/stream", station.getStreamUrl());
        assertEquals("https://example.com/resolved", station.getResolvedStreamUrl());
        assertEquals("https://example.com", station.getHomepage());
        assertEquals("https://example.com/favicon.png", station.getFavicon());
        assertEquals(Station.UNKNOWN, station.getCountry());
        assertEquals(Station.UNKNOWN, station.getCountryCode());
        assertEquals("Serbian", station.getLanguage());
        assertEquals("jazz,rock", station.getTags());
        assertEquals("MP3", station.getCodec());
        assertEquals(128, station.getBitrate());
        assertEquals(Boolean.TRUE, station.getHls());
        assertEquals("https://example.com/resolved", station.getPlayableStreamUrl());
        assertEquals(StationOrigin.RADIO_BROWSER, station.getOrigin());
    }

    @Test
    public void mapStationRejectsWhitespaceOnlyRequiredFields() {
        RadioBrowserStationDto dto = gson.fromJson("{"
                + "\"stationuuid\":\" uuid-2 \","
                + "\"name\":\"   \","
                + "\"url\":\" https://example.com/stream \""
                + "}", RadioBrowserStationDto.class);

        assertNull(RadioBrowserRepository.mapStation(dto));
    }

    @Test
    public void mapStationReturnsNullWhenRequiredDataIsMissing() {
        RadioBrowserStationDto dto = gson.fromJson("{"
                + "\"stationuuid\":\"uuid-2\","
                + "\"name\":\"Broken Station\""
                + "}", RadioBrowserStationDto.class);

        assertNull(RadioBrowserRepository.mapStation(dto));
    }

    @Test
    public void mapStationsFiltersNullEntries() {
        RadioBrowserStationDto first = gson.fromJson("{"
                + "\"stationuuid\":\"uuid-3\","
                + "\"name\":\"First\","
                + "\"url\":\"https://example.com/1\""
                + "}", RadioBrowserStationDto.class);

        List<Station> stations = RadioBrowserRepository.mapStations(Arrays.asList(first, null));

        assertEquals(1, stations.size());
        assertEquals("RADIO_BROWSER:uuid-3", stations.get(0).getId());
    }

    @Test
    public void loadTopStationsRetriesWithNextServerAfterFailure() {
        RadioBrowserStationDto dto = gson.fromJson("{"
                + "\"stationuuid\":\"uuid-4\","
                + "\"name\":\"Retry Station\","
                + "\"url\":\"https://example.com/stream\""
                + "}", RadioBrowserStationDto.class);

        RecordingApiFactory apiFactory = new RecordingApiFactory(
                new FailingCall<>(new IOException("first mirror failed")),
                new SuccessCall<>(Response.success(Collections.singletonList(dto)))
        );
        RadioBrowserServerSelector selector = new RadioBrowserServerSelector(Arrays.asList(
                "https://mirror-one/",
                "https://mirror-two/"
        ));
        RadioBrowserRepository repository = new RadioBrowserRepository(apiFactory, selector);

        RecordingLoadCallback callback = new RecordingLoadCallback();
        repository.loadTopStations(callback);
        callback.await();

        assertEquals(Arrays.asList("https://mirror-one/", "https://mirror-two/"),
                apiFactory.getRequestedBaseUrls());
        assertNull(callback.getError());
        assertEquals(1, callback.getStations().size());
        assertEquals("RADIO_BROWSER:uuid-4", callback.getStations().get(0).getId());
        assertEquals("https://mirror-two/", selector.getSelectedBaseUrl());
    }

    private static final class RecordingLoadCallback implements StationRepository.LoadCallback {
        @Nullable
        private List<Station> stations;
        @Nullable
        private Throwable error;
        @NonNull
        private final CountDownLatch doneLatch = new CountDownLatch(1);

        @Override
        public void onStationsLoaded(@NonNull List<Station> stations) {
            this.stations = new ArrayList<>(stations);
            doneLatch.countDown();
        }

        @Override
        public void onError(@NonNull Throwable throwable) {
            this.error = throwable;
            doneLatch.countDown();
        }

        @NonNull
        List<Station> getStations() {
            return stations == null ? Collections.emptyList() : stations;
        }

        @Nullable
        Throwable getError() {
            return error;
        }

        void await() {
            try {
                doneLatch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    private static final class RecordingApiFactory implements RadioBrowserApiFactory {
        private final List<Call<List<RadioBrowserStationDto>>> calls = new ArrayList<>();
        private final List<String> requestedBaseUrls = new ArrayList<>();
        private final AtomicInteger requestIndex = new AtomicInteger();

        RecordingApiFactory(Call<List<RadioBrowserStationDto>>... preparedCalls) {
            Collections.addAll(calls, preparedCalls);
        }

        @NonNull
        @Override
        public RadioBrowserApi create(@NonNull String baseUrl) {
            requestedBaseUrls.add(baseUrl);
            final int index = requestIndex.getAndIncrement();
            return new RadioBrowserApi() {
                @NonNull
                @Override
                public Call<List<RadioBrowserStationDto>> loadTopStations(int limit, boolean hideBroken) {
                    return calls.get(index);
                }

                @NonNull
                @Override
                public Call<List<RadioBrowserStationDto>> searchStationsByName(@NonNull String name,
                                                                               int limit,
                                                                               boolean hideBroken) {
                    return calls.get(index);
                }
            };
        }

        @NonNull
        List<String> getRequestedBaseUrls() {
            return requestedBaseUrls;
        }
    }

    private abstract static class BaseCall<T> implements Call<T> {
        private boolean executed;
        private boolean canceled;

        @Override
        public Response<T> execute() throws IOException {
            throw new UnsupportedOperationException("Not used in tests");
        }

        @Override
        public boolean isExecuted() {
            return executed;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        protected final void markExecuted() {
            executed = true;
        }

        @NonNull
        @Override
        public Request request() {
            return new Request.Builder().url("https://example.com/").build();
        }

        @NonNull
        @Override
        public Timeout timeout() {
            return new Timeout();
        }

        @NonNull
        protected abstract BaseCall<T> copy();

        @NonNull
        @Override
        public Call<T> clone() {
            return copy();
        }
    }

    private static final class SuccessCall<T> extends BaseCall<T> {
        @NonNull
        private final Response<T> response;

        private SuccessCall(@NonNull Response<T> response) {
            this.response = response;
        }

        @NonNull
        @Override
        public Response<T> execute() {
            markExecuted();
            return response;
        }

        @Override
        public void enqueue(@NonNull Callback<T> callback) {
            markExecuted();
            callback.onResponse(this, response);
        }

        @NonNull
        @Override
        protected BaseCall<T> copy() {
            return new SuccessCall<>(response);
        }
    }

    private static final class FailingCall<T> extends BaseCall<T> {
        @NonNull
        private final IOException failure;

        private FailingCall(@NonNull IOException failure) {
            this.failure = failure;
        }

        @Override
        public void enqueue(@NonNull Callback<T> callback) {
            markExecuted();
            callback.onFailure(this, failure);
        }

        @NonNull
        @Override
        protected BaseCall<T> copy() {
            return new FailingCall<>(failure);
        }
    }
}
