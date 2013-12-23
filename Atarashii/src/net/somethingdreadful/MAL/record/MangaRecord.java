package net.somethingdreadful.MAL.record;

import java.util.HashMap;

public class MangaRecord extends GenericMALRecord {

    public static final String STATUS_WATCHING = "reading";
    public static final String STATUS_PLANTOWATCH = "plan to read";

    public MangaRecord(HashMap<String, Object> record_data) {
        super(record_data);
    }

    public Integer getVolumesTotal() {
        return (Integer)this.recordData.get("volumesTotal");
    }

    @Override
    public String getTotal(boolean useSecondaryAmount) {
        if (useSecondaryAmount) {
            return Integer.toString(getVolumesTotal());
        }
        else {
            return Integer.toString(getChaptersTotal());
        }

    }

    public Integer getVolumeProgress() {
        return getVolumesRead();
    }

    public Integer getVolumesRead() {
        return (Integer)this.recordData.get("volumesRead");
    }

    public void setVolumesRead(Integer read) {
        this.recordData.put("volumesRead", read);
    }

    public Integer getChaptersTotal() {
        return (Integer)this.recordData.get("chaptersTotal");
    }

    public Integer getChaptersRead() {
        return (Integer)this.recordData.get("chaptersRead");
    }

    public void setChaptersRead(Integer chaptersRead) {
        this.recordData.put("chaptersRead", chaptersRead);
    }

    @Override
    public Integer getPersonalProgress(boolean useSecondaryAmount) {
        if (useSecondaryAmount) {
            return getVolumesRead();
        }
        else {
            return getChaptersRead();
        }
    }

    @Override
    public void setPersonalProgress(boolean useSecondaryAmount, Integer amount) {
        if (useSecondaryAmount) {
            setVolumesRead(amount);
        }
        else {
            setChaptersRead(amount);
        }
    }

    public static HashMap<String, Class<?>> getTypeMap() {
        typeMap = GenericMALRecord.getTypeMap();
        typeMap.put("volumesTotal", Integer.class);
        typeMap.put("chaptersTotal", Integer.class);
        typeMap.put("volumesRead", Integer.class);
        typeMap.put("chaptersRead", Integer.class);
        return typeMap;
    }

}