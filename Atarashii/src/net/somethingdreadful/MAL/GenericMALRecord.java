package net.somethingdreadful.MAL;

import android.text.Html;
import android.text.Spanned;

public abstract class GenericMALRecord {
    public static final int CLEAN = 0;
    public static final int DIRTY = 1;

    //these are the same for both, so put them in here
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_ONHOLD = "on-hold";
    public static final String STATUS_DROPPED = "dropped";

    protected int recordID;
    protected String recordName;
    protected String recordType;
    protected String imageUrl;
    protected String recordStatus;
    protected String myStatus;
    protected float memberScore;
    protected int myScore;
    protected String synopsis;
    protected int dirty;
    protected long lastUpdate;
    protected boolean FLAG_DELETE = false;

    public abstract int getPersonalProgress();

    public abstract void setPersonalProgress(int amount);

    public abstract String getTotal();

    public GenericMALRecord() {

    }

    public String getName() {
        return recordName;
    }



    public String getImageUrl() {
        return imageUrl;
    }

    public String getID() {

        return Integer.toString(recordID);
    }

    public String getRecordStatus() {

        return recordStatus;
    }

    public float getMemberScore() {

        return memberScore;
    }

    // Use this to get the raw HTML-formatted synopsis
    public String getSynopsis() {

        return synopsis;
    }

    // Use this to get a formatted version of the text suited for display in the application
    public Spanned getSpannedSynopsis() {
        return Html.fromHtml(synopsis);
    }

    public void setSynopsis(String newSynopsis) {
        this.synopsis = newSynopsis;
    }

    public String getRecordType() {

        return recordType;
    }

    public String getMyStatus() {

        return myStatus;
    }

    public int getMyScore() {
        return myScore;
    }

    public String getMyScoreString() {

        return Integer.toString(myScore);
    }

    public int getDirty() {
        // TODO Auto-generated method stub
        return dirty;
    }

    public void setDirty(int dirty) {
        this.dirty = dirty;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setMyStatus(String status) {
        this.myStatus = status;
    }

    public void setMemberScore(float memberScore) {
        this.memberScore = memberScore;
    }

    public void setMyScore(int myScore) {
        this.myScore = myScore;
    }

    public void markForDeletion(boolean mark) {
        FLAG_DELETE = mark;
    }
}