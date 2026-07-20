package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import com.matijakljajic.freeairradio.data.remote.radiobrowser.dto.RadioBrowserCountryCodeDto;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.dto.RadioBrowserStationDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

@SuppressWarnings("GrazieInspectionRunner")
public interface RadioBrowserApi {

    @GET("json/stations/topclick")
    Call<List<RadioBrowserStationDto>> loadTopStations(
            @Query("limit") int limit,
            @Query("hidebroken") boolean hideBroken
    );

    @GET("json/stations/bycountrycodeexact/{countryCode}")
    Call<List<RadioBrowserStationDto>> loadTopStationsByCountryCode(
            @Path("countryCode") String countryCode,
            @Query("limit") int limit,
            @Query("hidebroken") boolean hideBroken,
            @Query("order") String order,
            @Query("reverse") boolean reverse
    );

    @GET("json/countrycodes")
    Call<List<RadioBrowserCountryCodeDto>> loadCountryCodes(
            @Query("hidebroken") boolean hideBroken,
            @Query("order") String order
    );

    @GET("json/stations/search")
    Call<List<RadioBrowserStationDto>> searchStationsByName(
            @Query("name") String name,
            @Query("limit") int limit,
            @Query("hidebroken") boolean hideBroken
    );

    @GET("json/url/{stationuuid}")
    Call<Void> reportStationUsage(@Path("stationuuid") String stationUuid);
}
