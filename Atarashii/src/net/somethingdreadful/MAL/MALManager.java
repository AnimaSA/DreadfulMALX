package net.somethingdreadful.MAL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import net.somethingdreadful.MAL.api.MALApi;
import net.somethingdreadful.MAL.record.AnimeRecord;
import net.somethingdreadful.MAL.record.GenericMALRecord;
import net.somethingdreadful.MAL.record.MangaRecord;
import net.somethingdreadful.MAL.sql.MALSqlHelper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MALManager {

    final static String TYPE_ANIME = "anime";
    final static String TYPE_MANGA = "manga";

    private String[] animeColumns = {"recordID", "recordName", "recordType", "recordStatus", "myStatus",
            "episodesWatched", "episodesTotal", "memberScore", "myScore", "synopsis", "imageUrl", "dirty", "lastUpdate"};

    private String[] mangaColumns = {"recordID", "recordName", "recordType", "recordStatus", "myStatus",
            "volumesRead", "chaptersRead", "volumesTotal", "chaptersTotal", "memberScore", "myScore", "synopsis",
            "imageUrl", "dirty", "lastUpdate"};

    static MALSqlHelper malSqlHelper;

    MALApi malApi;
    static SQLiteDatabase dbRead;

    public MALManager(Context context) {
        malApi = new MALApi(context);
        if (malSqlHelper == null) {
            malSqlHelper = MALSqlHelper.getHelper(context);
        }
    }

    public synchronized static SQLiteDatabase getDBWrite() {
        return malSqlHelper.getWritableDatabase();
    }

    public static SQLiteDatabase getDBRead() {
        if (dbRead == null) {
            dbRead = malSqlHelper.getReadableDatabase();
        }
        return dbRead;
    }

    static String listSortFromInt(int i, String type) {
        String r = "";

        if (type.equals("anime")) {
            switch (i) {
                case 0:
                    r = "";
                    break;
                case 1:
                    r = AnimeRecord.STATUS_WATCHING;
                    break;
                case 2:
                    r = AnimeRecord.STATUS_COMPLETED;
                    break;
                case 3:
                    r = AnimeRecord.STATUS_ONHOLD;
                    break;
                case 4:
                    r = AnimeRecord.STATUS_DROPPED;
                    break;
                case 5:
                    r = AnimeRecord.STATUS_PLANTOWATCH;
                    break;
                default:
                    r = AnimeRecord.STATUS_WATCHING;
                    break;
            }
        } else if (type.equals("manga")) {
            switch (i) {
                case 0:
                    r = "";
                    break;
                case 1:
                    r = MangaRecord.STATUS_WATCHING;
                    break;
                case 2:
                    r = MangaRecord.STATUS_COMPLETED;
                    break;
                case 3:
                    r = MangaRecord.STATUS_ONHOLD;
                    break;
                case 4:
                    r = MangaRecord.STATUS_DROPPED;
                    break;
                case 5:
                    r = MangaRecord.STATUS_PLANTOWATCH;
                    break;
                default:
                    r = MangaRecord.STATUS_WATCHING;
                    break;
            }
        }

        return r;
    }

    public HashMap<String, Object> getRecordDataFromJSONObject(JSONObject jsonObject, String type) {
        HashMap<String, Object> recordData = new HashMap<>();

        try {
            recordData.put("recordID", jsonObject.getInt("id"));
            recordData.put("recordName", StringEscapeUtils.unescapeHtml4(jsonObject.getString("title")));
            recordData.put("recordType", jsonObject.getString("type"));
            recordData.put("recordStatus", jsonObject.getString("status"));
            recordData.put("myScore", jsonObject.optInt("score"));
            recordData.put("memberScore", (float) jsonObject.optDouble("members_score", 0.0));
            recordData.put("imageUrl", jsonObject.getString("image_url").replaceFirst("t.jpg$", ".jpg"));
            if (type.equals(TYPE_ANIME)) {
                recordData.put("episodesTotal", jsonObject.optInt("episodes"));
                recordData.put("episodesWatched", jsonObject.optInt("watched_episodes"));
                recordData.put("myStatus", jsonObject.getString("watched_status"));
            } else if (type.equals(TYPE_MANGA)) {
                recordData.put("myStatus", jsonObject.getString("read_status"));
                recordData.put("volumesTotal", jsonObject.optInt("volumes"));
                recordData.put("chaptersTotal", jsonObject.optInt("chapters"));
                recordData.put("volumesRead", jsonObject.optInt("volumes_read"));
                recordData.put("chaptersRead", jsonObject.optInt("chapters_read"));
            }
        } catch (JSONException e) {
            Log.e(this.getClass().getName(), Log.getStackTraceString(e));
        }
        return recordData;
    }

    public void downloadAndStoreList(String type) {

        ContentValues contentValues = new ContentValues();
        contentValues.put("lastUpdate", 0);
        getDBWrite().update(type, contentValues, null, null);

        int currentTime = (int) new Date().getTime() / 1000;
        JSONArray jArray = malApi.getList(getListTypeFromString(type));
        try {
            getDBWrite().beginTransaction();
            switch (type) {
                case TYPE_ANIME: {
                    for (int i = 0; i < jArray.length(); i++) {
                        HashMap<String, Object> recordData = getRecordDataFromJSONObject(jArray.getJSONObject(i), type);
                        AnimeRecord ar = new AnimeRecord(recordData);
                        ar.setLastUpdate(currentTime);
                        saveItem(ar, true);
                    }
                    break;
                }
                case TYPE_MANGA:
                    for (int i = 0; i < jArray.length(); i++) {
                        HashMap<String, Object> recordData = getRecordDataFromJSONObject(jArray.getJSONObject(i), type);
                        MangaRecord mr = new MangaRecord(recordData);
                        mr.setLastUpdate(currentTime);
                        saveItem(mr, true);
                    }
                    break;
            }
            getDBWrite().setTransactionSuccessful();
            clearDeletedItems(type, currentTime);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            getDBWrite().endTransaction();
        }
    }

    public AnimeRecord getAnimeRecordFromMAL(int id) {
        String type = TYPE_ANIME;
        JSONObject jsonObject = malApi.getDetail(id, getListTypeFromString(type));
        HashMap<String, Object> recordData = getRecordDataFromJSONObject(jsonObject, type);
        AnimeRecord record = new AnimeRecord(recordData);
        if (record.getMyStatus().isEmpty()) {
            record.markForCreate(true);
        }
        return record;
    }

    public MangaRecord getMangaRecordFromMAL(int id) {
        String type = TYPE_MANGA;
        JSONObject jsonObject = malApi.getDetail(id, getListTypeFromString(type));
        HashMap<String, Object> recordData = getRecordDataFromJSONObject(jsonObject, type);
        MangaRecord record = new MangaRecord(recordData);
        if (record.getMyStatus().isEmpty()) {
            record.markForCreate(true);
        }
        return record;
    }

    public AnimeRecord updateWithDetails(int id, AnimeRecord animeRecord) {
        JSONObject jsonObject = malApi.getDetail(id, getListTypeFromString(TYPE_ANIME));

        animeRecord.setSynopsis(getDataFromJSON(jsonObject, "synopsis"));
        animeRecord.setMemberScore(Float.parseFloat(getDataFromJSON(jsonObject, "members_score")));

        if (!getDBWrite().inTransaction()) {
            saveItem(animeRecord, false);
        }
        return animeRecord;
    }

    public MangaRecord updateWithDetails(int id, MangaRecord mangaRecord) {
        JSONObject jsonObject = malApi.getDetail(id, getListTypeFromString(TYPE_MANGA));
        mangaRecord.setSynopsis(getDataFromJSON(jsonObject, "synopsis"));
        mangaRecord.setMemberScore(Float.parseFloat(getDataFromJSON(jsonObject, "members_score")));
        saveItem(mangaRecord, false);
        return mangaRecord;
    }

    public String getDataFromJSON(JSONObject json, String get) {
        // TODO: replace to jsonObject.optString(get, fallback);
        String sReturn = "";

        try {
            sReturn = json.getString(get);

            if ("episodes".equals(get)) {
                if ("null".equals(sReturn)) {
                    sReturn = "unknown";
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            sReturn = "unknown";
        }

        return sReturn;
    }

    public Object getObjectFromCursorColumn(Cursor cursor, int index) {
        int object_type = cursor.getType(index);
        if (object_type == Cursor.FIELD_TYPE_STRING) {
            return cursor.getString(index);
        }
        if (object_type == Cursor.FIELD_TYPE_FLOAT) {
            return cursor.getFloat(index);
        }
        if (object_type == Cursor.FIELD_TYPE_INTEGER) {
            return cursor.getInt(index);
        }
        return null;
    }

    public HashMap<String, Object> getRecordDataFromCursor(Cursor cursor) {
        HashMap<String, Object> record_data = new HashMap<>();
        String[] columns = cursor.getColumnNames();
        for (int i = 0; i < columns.length; i++) {
            record_data.put(columns[i], this.getObjectFromCursorColumn(cursor, i));
        }
        return record_data;
    }

    public ArrayList<AnimeRecord> getAnimeRecordsFromDB(int list) {
        Log.v("MALX", "getAnimeRecordsFromDB() has been invoked for list " + listSortFromInt(list, "anime"));

        ArrayList<AnimeRecord> animeRecordArrayList = new ArrayList<>();
        Cursor cursor;

        if (list == 0) {
            cursor = getDBRead().query("anime", this.animeColumns, "myStatus != 'null'", null, null, null, "recordName ASC");
        } else {
            cursor = getDBRead().query("anime", this.animeColumns, "myStatus = ?", new String[]{listSortFromInt(list, "anime")}, null, null, "recordName ASC");
        }


        Log.v("MALX", "Got " + cursor.getCount() + " records.");
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            animeRecordArrayList.add(new AnimeRecord(this.getRecordDataFromCursor(cursor)));
            cursor.moveToNext();
        }

        if (animeRecordArrayList.isEmpty()) {
            return null;
        }

        cursor.close();

        return animeRecordArrayList;
    }

    public ArrayList<MangaRecord> getMangaRecordsFromDB(int list) {
        Log.v("MALX", "getMangaRecordsFromDB() has been invoked for list " + listSortFromInt(list, "manga"));

        ArrayList<MangaRecord> mangaRecordArrayList = new ArrayList<>();
        Cursor cursor;

        if (list == 0) {
            cursor = getDBRead().query("manga", this.mangaColumns, "myStatus != 'null'", null, null, null, "recordName ASC");
        } else {
            cursor = getDBRead().query("manga", this.mangaColumns, "myStatus = ?", new String[]{listSortFromInt(list, "manga")}, null, null, "recordName ASC");
        }

        Log.v("MALX", "Got " + cursor.getCount() + " records.");
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            mangaRecordArrayList.add(new MangaRecord(this.getRecordDataFromCursor(cursor)));
            cursor.moveToNext();
        }

        if (mangaRecordArrayList.isEmpty()) {
            return null;
        }

        cursor.close();

        return mangaRecordArrayList;
    }

    public void saveItem(MangaRecord mr, boolean ignoreSynopsis) {
        ContentValues cv = new ContentValues();

        cv.put("recordID", mr.getID());
        cv.put("recordName", mr.getName());
        cv.put("recordType", mr.getRecordType());
        cv.put("imageUrl", mr.getImageUrl());
        cv.put("recordStatus", mr.getRecordStatus());
        cv.put("myStatus", mr.getMyStatus());
        cv.put("memberScore", mr.getMemberScore());
        cv.put("myScore", mr.getMyScore());
        cv.put("volumesRead", mr.getVolumeProgress());
        cv.put("chaptersRead", mr.getPersonalProgress());
        cv.put("volumesTotal", mr.getVolumesTotal());
        cv.put("chaptersTotal", mr.getTotal());
        cv.put("dirty", mr.getDirty());
        cv.put("lastUpdate", mr.getLastUpdate());

        if (!ignoreSynopsis) {
            cv.put("synopsis", mr.getSynopsis());
        }

        if (itemExists(mr.getID(), "manga")) {
            getDBWrite().update(MALSqlHelper.TABLE_MANGA, cv, "recordID=?", new String[]{mr.getID().toString()});
        } else {
            getDBWrite().insert(MALSqlHelper.TABLE_MANGA, null, cv);
        }
    }

    public void saveItem(AnimeRecord ar, boolean ignoreSynopsis) {

        ContentValues cv = new ContentValues();

        cv.put("recordID", ar.getID());
        cv.put("recordName", ar.getName());
        cv.put("recordType", ar.getRecordType());
        cv.put("imageUrl", ar.getImageUrl());
        cv.put("recordStatus", ar.getRecordStatus());
        cv.put("myStatus", ar.getMyStatus());
        cv.put("memberScore", ar.getMemberScore());
        cv.put("myScore", ar.getMyScore());
        cv.put("episodesWatched", ar.getPersonalProgress());
        cv.put("episodesTotal", ar.getTotal());
        cv.put("dirty", ar.getDirty());
        cv.put("lastUpdate", ar.getLastUpdate());


        if (!ignoreSynopsis) {
            cv.put("synopsis", ar.getSynopsis());
        }

        if (itemExists(ar.getID(), "anime")) {
            getDBWrite().update(MALSqlHelper.TABLE_ANIME, cv, "recordID=?", new String[]{ar.getID().toString()});
        } else {
            getDBWrite().insert(MALSqlHelper.TABLE_ANIME, null, cv);
        }
    }

    public AnimeRecord getAnimeRecordFromDB(int id) {
        Log.v("MALX", "getAnimeRecordFromDB() has been invoked for id " + id);
        Cursor cu = getDBRead().query("anime", this.animeColumns, "recordID = ?", new String[]{Integer.toString(id)}, null, null, null);
        cu.moveToFirst();
        AnimeRecord ar = new AnimeRecord(this.getRecordDataFromCursor(cu));
        cu.close();
        return ar;
    }

    public MangaRecord getMangaRecordFromDB(int id) {
        Log.v("MALX", "getMangaRecordFromDB() has been invoked for id " + id);

        Cursor cu = getDBRead().query("manga", this.mangaColumns, "recordID = ?", new String[]{Integer.toString(id)}, null, null, null);

        cu.moveToFirst();

        MangaRecord mr = new MangaRecord(this.getRecordDataFromCursor(cu));

        cu.close();

        return mr;
    }

    public AnimeRecord getAnimeRecord(int recordID) {
        if (this.itemExists(recordID, TYPE_ANIME)) {
            return getAnimeRecordFromDB(recordID);
        }
        return getAnimeRecordFromMAL(recordID);
    }

    public MangaRecord getMangaRecord(int recordID) {
        if (this.itemExists(recordID, TYPE_MANGA)) {
            return this.getMangaRecordFromDB(recordID);
        }
        return getMangaRecordFromMAL(recordID);
    }

    public boolean itemExists(int id, String type) {
        return this.itemExists(Integer.toString(id), type);
    }

    public boolean itemExists(String id, String type) {
        if (type.equals("anime") || type.equals("manga")) {
            Cursor cursor = getDBRead().rawQuery("select 1 from " + type + " WHERE recordID=? LIMIT 1",
                    new String[]{id});
            boolean exists = (cursor.getCount() > 0);
            cursor.close();
            return exists;
        } else {
            throw new RuntimeException("itemExists called with unknown type.");
        }
    }

    public boolean writeDetailsToMAL(GenericMALRecord gr, String type) {
        boolean success;
        MALApi.ListType listType = getListTypeFromString(type);

        if (gr.hasDelete()) {
            success = malApi.deleteGenreFromList(listType, gr.getID().toString());
        } else {
            HashMap<String, String> data = new HashMap<>();
            data.put("status", gr.getMyStatus());
            data.put("score", gr.getMyScoreString());
            switch (listType) {
                case ANIME: {
                    data.put("episodes", Integer.toString(gr.getPersonalProgress()));
                    break;
                }
                case MANGA: {
                    data.put("chapters", Integer.toString(gr.getPersonalProgress()));
                    data.put("volumes", Integer.toString(((MangaRecord) gr).getVolumeProgress()));
                    break;
                }
            }
            success = malApi.addOrUpdateGenreInList(gr.hasCreate(), listType, gr.getID().toString(), data);
        }
        return success;
    }

    public void clearDeletedItems(String type, long currentTime) {
        Log.v("MALX", "Removing deleted items of type " + type + " older than " + DateFormat.getDateTimeInstance().format(currentTime * 1000));

        int recordsRemoved = getDBWrite().delete(type, "lastUpdate < ?", new String[]{String.valueOf(currentTime)});

        Log.v("MALX", "Removed " + recordsRemoved + " " + type + " items");
    }

    public boolean deleteItemFromDatabase(String type, int recordID) {
        int deleted = getDBWrite().delete(type, "recordID = ?", new String[]{String.valueOf(recordID)});

        return deleted == 1;
    }

    private MALApi.ListType getListTypeFromString(String type) {
        switch (type) {
            case TYPE_ANIME:
                return MALApi.ListType.ANIME;
            case TYPE_MANGA:
                return MALApi.ListType.MANGA;
            default:
                return null;
        }
    }

}
