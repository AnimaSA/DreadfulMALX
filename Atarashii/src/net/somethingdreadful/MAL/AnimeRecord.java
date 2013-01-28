package net.somethingdreadful.MAL;

public class AnimeRecord extends GenericMALRecord {

    private int episodesWatched;
    private int episodesTotal;

    public static final String STATUS_WATCHING = "watching";
    public static final String STATUS_PLANTOWATCH = "plan to watch";

    public AnimeRecord(int id, String name, String type, String status, String myStatus, int watched, int total,
            float memberScore, int myScore, String synopsis, String imageUrl, int dirty, long lastUpdate) {
        this.recordID = id;
        this.recordName = name;
        this.recordType = type;
        this.imageUrl = imageUrl;
        this.recordStatus = status;
        this.myStatus = myStatus;
        this.memberScore = memberScore;
        this.myScore = myScore;
        this.synopsis = synopsis;

        this.episodesTotal = total;
        this.episodesWatched = watched;

        this.dirty = dirty;
        this.lastUpdate = lastUpdate;

    }

    public AnimeRecord(int id, String name, String imageUrl, int watched, int totalEpisodes,
            String myStatus, String animeStatus, String animeType, int myScore, int dirty, long lastUpdate) {
        this.recordID = id;
        this.recordName = name;
        this.episodesWatched = watched;
        this.imageUrl = imageUrl;
        this.myStatus = myStatus;
        this.episodesTotal = totalEpisodes;
        this.recordStatus = animeStatus;
        this.recordType = animeType;
        this.myScore = myScore;

        this.dirty = dirty;
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String getTotal() {

        return Integer.toString(episodesTotal);
    }


    public void setEpisodesWatched(int watched) {
        this.episodesWatched = watched;
    }

    @Override
    public int getPersonalProgress() {
        return episodesWatched;
    }

    @Override
    public void setPersonalProgress(int amount) {
        this.episodesWatched = amount;
    }

}