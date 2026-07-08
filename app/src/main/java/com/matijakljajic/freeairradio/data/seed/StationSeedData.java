package com.matijakljajic.freeairradio.data.seed;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StationSeedData {

    private StationSeedData() {
    }

    public static List<Station> createDemoStations() {
        List<Station> demoStations = new ArrayList<>();
        demoStations.add(new Station(
                "be930f76-8b01-4d51-9863-4b5a8c64897a",
                "1-NRK Jazz",
                "http://cdn0-47115-liveicecast0.dna.contentdelivery.net/jazz_mp3_h",
                "Norway",
                "norwegian",
                "jazz,smooth jazz",
                "MP3",
                192,
                StationOrigin.RADIO_BROWSER));
        demoStations.add(new Station(
                "c65436db-e6b8-425f-b925-0396b056ea89",
                "Hrvatski radio - Klasik (HRT)",
                "http://playerservices.streamtheworld.com/m3u/HR_CLASSICSAAC.m3u",
                "Croatia",
                "",
                "baroque,classic,classical,classical baroque,classical music",
                "AAC+",
                128,
                StationOrigin.RADIO_BROWSER));
        demoStations.add(new Station(
                "00f796ae-9ccb-4235-8c99-9c80b556ff16",
                "Radio 021",
                "https://radio.dukahosting.com/8006/stream",
                "Serbia",
                "serbian",
                "",
                "AAC+",
                80,
                StationOrigin.RADIO_BROWSER));
        demoStations.add(new Station(
                "72a0c58a-54e6-43da-b493-b0e38baa9867",
                "Radio Beograd 202",
                "https://rtsradio-live.morescreens.com/RTS_2_004/playlist.m3u8",
                "Serbia",
                "serbian",
                "",
                Station.UNKNOWN,
                128,
                StationOrigin.RADIO_BROWSER));
        demoStations.add(new Station(
                "9606ceae-0601-11e8-ae97-52543be04c81",
                "Radio Caroline",
                "http://78.129.202.200:8040/",
                "The United Kingdom Of Great Britain And Northern Ireland",
                "english",
                "country,pop,rock",
                "MP3",
                128,
                StationOrigin.RADIO_BROWSER));
        demoStations.add(new Station(
                "caa4d543-693a-11e9-af37-52543be04c81",
                "Radio Nova Vintage",
                "http://nova-vnt.ice.infomaniak.ch/nova-vnt-128.mp3",
                "France",
                "french",
                "1980s,1990s,80s,90s,groove,oldies,rare groove,vintage,vintage music",
                "MP3",
                128,
                StationOrigin.RADIO_BROWSER));
        return Collections.unmodifiableList(demoStations);
    }
}
