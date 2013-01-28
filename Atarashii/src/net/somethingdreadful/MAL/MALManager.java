package net.somethingdreadful.MAL;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

public class MALManager {

    final static String APIProvider = "http://mal-api.com/";
    final static String VerifyAPI = "account/verify_credentials";
    final static String readAnimeListAPI = "animelist/";
    final static String readAnimeDetailsAPI = "anime/";
    final static String writeAnimeDetailsAPI = "animelist/anime/";
    final static String readMangaListAPI = "mangalist/";
    final static String readMangaDetailsAPI = "manga/";
    final static String writeMangaDetailsAPI = "mangalist/manga/";
    final static String readMineParam = "?mine=1";

    final static String TYPE_ANIME = "anime";
    final static String TYPE_MANGA = "manga";

    final static String USER_AGENT = "Atarashii! (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + " Build/" + Build.DISPLAY + ")";

    private String[] animeColumns = {"recordID", "recordName", "recordType", "recordStatus", "myStatus",
            "episodesWatched", "episodesTotal", "memberScore", "myScore", "synopsis", "imageUrl", "dirty", "lastUpdate"};

    private String[] mangaColumns = {"recordID", "recordName", "recordType", "recordStatus", "myStatus",
            "volumesRead", "chaptersRead", "volumesTotal", "chaptersTotal", "memberScore", "myScore", "synopsis",
            "imageUrl", "dirty", "lastUpdate"};

    Context c;
    PrefManager prefManager;
    String malUser;
    String malPass;
    MALSqlHelper helper;
    SQLiteDatabase db;

    public MALManager(Context c)
    {
        this.c = c;
        prefManager = new PrefManager(c);

        malUser = prefManager.getUser();
        malPass = prefManager.getPass();

        helper = new MALSqlHelper(this.c);
        db = helper.getWritableDatabase();
    }

    static public boolean verifyAccount(String user, String pass)
    {
        HttpGet request = new HttpGet(APIProvider + VerifyAPI);
        HttpResponse response = null;
        request.setHeader("Authorization", "basic " + Base64.encodeToString((user + ":" + pass).getBytes(), Base64.NO_WRAP));

        try
        {
            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
            response = client.execute(request);
        }
        catch (ClientProtocolException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        if (statusCode == 200)
        {
            return true;
        }

        else
        {
            return false;
        }
    }

    static String listSortFromInt(int i, String type)
    {
        String r = "";

        if(type == "anime") {
            switch (i)
            {
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
        }
        else if (type == "manga") {
            switch (i)
            {
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

    public JSONObject getList(String type) {
        String readListAPI = null;
        String result = null;
        JSONObject jReturn = null;

        if (type == "anime") {
            readListAPI = MALManager.readAnimeListAPI;
        }
        else if (type == "manga") {
            readListAPI = MALManager.readMangaListAPI;
        }
        else
        {
            throw new RuntimeException("getList called with unknown list type.");
        }

        HttpGet request;
        HttpResponse response;
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

        request = new HttpGet(APIProvider + readListAPI + malUser);
        request.setHeader("Authorization", "basic " + Base64.encodeToString((malUser + ":" + malPass).getBytes(), Base64.NO_WRAP));


        try
        {
            response = client.execute(request);

            HttpEntity getResponseEntity = response.getEntity();

            if (getResponseEntity != null)
            {
                result = EntityUtils.toString(getResponseEntity);
                jReturn = new JSONObject(result);

                Log.v("MALX", "Got JSON Response from the API");

            }

        }
        catch (ClientProtocolException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }


        return jReturn;
    }

    public JSONObject getDetails(int id, String type) {

        String result = null;
        JSONObject jReturn = null;
        String readDetailsAPI = null;

        if (type == "anime") {
            readDetailsAPI = MALManager.readAnimeDetailsAPI;
        }
        else if (type == "manga") {
            readDetailsAPI = MALManager.readMangaDetailsAPI;
        }
        else
        {
            throw new RuntimeException("getDetails called with unknown list type.");
        }

        HttpGet request;
        HttpResponse response;
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

        request = new HttpGet(APIProvider + readDetailsAPI + id + readMineParam);
        request.setHeader("Authorization", "basic " + Base64.encodeToString((malUser + ":" + malPass).getBytes(), Base64.NO_WRAP));

        try
        {
            response = client.execute(request);

            HttpEntity getResponseEntity = response.getEntity();

            if (getResponseEntity != null)
            {
                result = EntityUtils.toString(getResponseEntity);
                jReturn = new JSONObject(result);

            }

        }
        catch (ClientProtocolException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return jReturn;
    }

    public void downloadAndStoreList(String type)
    {
        JSONObject raw = getList(type);

        long currentTime = new Date().getTime() / 1000;

        JSONArray jArray;
        try
        {
            if(type == TYPE_ANIME) {
                jArray = raw.getJSONArray(TYPE_ANIME);

                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject a = jArray.getJSONObject(i);

                    int id = a.getInt("id");
                    String name = a.getString("title");
                    int watched = a.getInt("watched_episodes");
                    int totalEpisodes = a.getInt("episodes");
                    String imageUrl = a.getString("image_url");
                    String animeStatus = a.getString("status");
                    String myStatus = a.getString("watched_status");
                    String animeType = a.getString("type");
                    int myScore = a.getInt("score");

                    AnimeRecord ar = new AnimeRecord(id, name, imageUrl, watched, totalEpisodes,
                            myStatus, animeStatus, animeType, myScore, 0, currentTime);

                    saveItem(ar, true);
                }
            }
            else if(type == TYPE_MANGA) {
                jArray = raw.getJSONArray(TYPE_MANGA);

                for (int i = 0; i < jArray.length(); i++)
                {
                    try {
                        JSONObject a = jArray.getJSONObject(i);

                        int id = a.getInt("id");
                        String name = a.getString("title");
                        int readVolumes = a.getInt("volumes_read");
                        int readChapters = a.getInt("chapters_read");
                        int totalVolumes = a.getInt("volumes");
                        int totalChapters = a.getInt("chapters");
                        String imageUrl = a.getString("image_url");
                        String mangaStatus = a.getString("status");
                        String myStatus = a.getString("read_status");
                        String mangaType = a.getString("type");
                        int myScore = a.getInt("score");

                        MangaRecord mr = new MangaRecord(id, name, mangaType, mangaStatus, myStatus,
                                readVolumes, readChapters, totalVolumes, totalChapters, myScore, imageUrl, 0, currentTime);

                        saveItem(mr, true);
                    }
                    catch (JSONException e) {
                        throw e;
                    }


                }
            }

            clearDeletedItems(type, currentTime);

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public AnimeRecord updateWithDetails(int id, AnimeRecord ar)
    {
        JSONObject o = getDetails(id, "anime");

        ar.setSynopsis(getDataFromJSON(o, "synopsis"));
        ar.setMemberScore(Float.parseFloat(getDataFromJSON(o, "members_score")));

        saveItem(ar, false);

        return ar;
    }

    public MangaRecord updateWithDetails(int id, MangaRecord mr)
    {
        JSONObject o = getDetails(id, "manga");

        mr.setSynopsis(getDataFromJSON(o, "synopsis"));
        mr.setMemberScore(Float.parseFloat(getDataFromJSON(o, "members_score")));

        saveItem(mr, false);

        return mr;
    }

    public String getDataFromJSON(JSONObject json, String get)
    {
        String sReturn = "";

        try
        {
            sReturn = json.getString(get);

            if ("episodes".equals(get))
            {
                if ("null".equals(sReturn))
                {
                    sReturn = "unknown";
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
            sReturn = "unknown";
        }

        return sReturn;
    }

    public ArrayList<AnimeRecord> getAnimeRecordsFromDB(int list)
    {
        Log.v("MALX", "getAnimeRecordsFromDB() has been invoked for list " + listSortFromInt(list, "anime"));

        ArrayList<AnimeRecord> al = new ArrayList();
        Cursor cu;

        if (list == 0)
        {
            cu = db.query("anime", this.animeColumns, null, null, null, null, "recordName ASC");
        }
        else
        {
            cu = db.query("anime", this.animeColumns, "myStatus = ?", new String[]{listSortFromInt(list, "anime")}, null, null, "recordName ASC");
        }


        Log.v("MALX", "Got " + cu.getCount() + " records.");
        cu.moveToFirst();

        while (cu.isAfterLast() == false)
        {

            int recordId = cu.getInt(0);
            String recordName = cu.getString(1);
            String recordType = cu.getString(2);
            String recordStatus = cu.getString(3);
            String myStatus = cu.getString(4);
            int episodesWatched = cu.getInt(5);
            int episodesTotal = cu.getInt(6);
            float memberScore = cu.getFloat(7);
            int myScore = cu.getInt(8);
            String synopsis = cu.getString(9);
            String imageUrl = cu.getString(10);
            int dirty = cu.getInt(11);
            long lastUpdate = cu.getLong(12);

            AnimeRecord ar = new AnimeRecord(recordId, recordName, recordType, recordStatus, myStatus,
                    episodesWatched, episodesTotal, memberScore, myScore, synopsis, imageUrl, dirty, lastUpdate);

            al.add(ar);

            cu.moveToNext();
        }

        if (al.isEmpty())
        {
            return null;
        }

        cu.close();

        return al;
    }

    public ArrayList<MangaRecord> getMangaRecordsFromDB(int list)
    {
        Log.v("MALX", "getMangaRecordsFromDB() has been invoked for list " + listSortFromInt(list, "manga"));

        ArrayList<MangaRecord> ml = new ArrayList();
        Cursor cu;

        if (list == 0)
        {
            cu = db.query("manga", this.mangaColumns, null, null, null, null, "recordName ASC");
        }
        else
        {
            cu = db.query("manga", this.mangaColumns, "myStatus = ?", new String[]{listSortFromInt(list, "manga")}, null, null, "recordName ASC");
        }

        Log.v("MALX", "Got " + cu.getCount() + " records.");
        cu.moveToFirst();

        while (cu.isAfterLast() == false)
        {

            int recordId = cu.getInt(0);
            String recordName = cu.getString(1);
            String recordType = cu.getString(2);
            String recordStatus = cu.getString(3);
            String myStatus = cu.getString(4);
            int volumesRead = cu.getInt(5);
            int chaptersRead = cu.getInt(6);
            int volumesTotal = cu.getInt(7);
            int chaptersTotal = cu.getInt(8);
            float memberScore = cu.getFloat(9);
            int myScore = cu.getInt(10);
            String synopsis = cu.getString(11);
            String imageUrl = cu.getString(12);
            int dirty = cu.getInt(13);
            long lastUpdate = cu.getLong(14);

            MangaRecord mr = new MangaRecord(recordId, recordName, recordType, recordStatus, myStatus,
                    volumesRead, chaptersRead, volumesTotal, chaptersTotal, memberScore, myScore, synopsis,
                    imageUrl, dirty, lastUpdate);
            ml.add(mr);

            cu.moveToNext();
        }

        if (ml.isEmpty())
        {
            return null;
        }

        cu.close();

        return ml;
    }

    public void saveItem(MangaRecord mr, boolean ignoreSynopsis)
    {
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
        cv.put("volumesTotal", mr.getVolumeTotal());
        cv.put("chaptersTotal", mr.getTotal());
        cv.put("dirty", mr.getDirty());
        cv.put("lastUpdate", mr.getLastUpdate());

        if (ignoreSynopsis == false)
        {
            cv.put("synopsis", mr.getSynopsis());
        }

        if (itemExists(mr.getID(), "manga"))
        {
            db.update(MALSqlHelper.TABLE_MANGA, cv, "recordID=?", new String[] {mr.getID()});
        }
        else
        {
            db.insert(MALSqlHelper.TABLE_MANGA, null, cv);
        }
    }

    public void saveItem(AnimeRecord ar, boolean ignoreSynopsis)
    {

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

        if (ignoreSynopsis == false)
        {
            cv.put("synopsis", ar.getSynopsis());
        }

        if (itemExists(ar.getID(), "anime"))
        {
            db.update(MALSqlHelper.TABLE_ANIME, cv, "recordID=?", new String[] {ar.getID()});
        }
        else
        {
            db.insert(MALSqlHelper.TABLE_ANIME, null, cv);
        }
    }

    public AnimeRecord getAnimeRecordFromDB(int id)
    {
        Log.v("MALX", "getAnimeRecordFromDB() has been invoked for id " + id);

        Cursor cu = db.query("anime", this.animeColumns, "recordID = ?", new String[]{Integer.toString(id)}, null, null, null);

        cu.moveToFirst();

        int recordId = cu.getInt(0);
        String recordName = cu.getString(1);
        String recordType = cu.getString(2);
        String recordStatus = cu.getString(3);
        String myStatus = cu.getString(4);
        int episodesWatched = cu.getInt(5);
        int episodesTotal = cu.getInt(6);
        float memberScore = cu.getFloat(7);
        int myScore = cu.getInt(8);
        String synopsis = cu.getString(9);
        String imageUrl = cu.getString(10);
        int dirty = cu.getInt(11);
        long lastUpdate = cu.getLong(12);

        AnimeRecord ar = new AnimeRecord(recordId, recordName, recordType, recordStatus, myStatus,
                episodesWatched, episodesTotal, memberScore, myScore, synopsis, imageUrl, dirty, lastUpdate);

        cu.close();

        return ar;
    }

    public MangaRecord getMangaRecordFromDB(int id)
    {
        Log.v("MALX", "getMangaRecordFromDB() has been invoked for id " + id);

        Cursor cu = db.query("manga", this.mangaColumns, "recordID = ?", new String[]{Integer.toString(id)}, null, null, null);

        cu.moveToFirst();

        int recordId = cu.getInt(0);
        String recordName = cu.getString(1);
        String recordType = cu.getString(2);
        String recordStatus = cu.getString(3);
        String myStatus = cu.getString(4);
        int volumesRead = cu.getInt(5);
        int chaptersRead = cu.getInt(6);
        int volumesTotal = cu.getInt(7);
        int chaptersTotal = cu.getInt(8);
        float memberScore = cu.getFloat(9);
        int myScore = cu.getInt(10);
        String synopsis = cu.getString(11);
        String imageUrl = cu.getString(12);
        int dirty = cu.getInt(13);
        long lastUpdate = cu.getLong(14);

        MangaRecord mr = new MangaRecord(recordId, recordName, recordType, recordStatus, myStatus,
                volumesRead, chaptersRead, volumesTotal, chaptersTotal, memberScore, myScore, synopsis,
                imageUrl, dirty, lastUpdate);

        cu.close();

        return mr;
    }


    public boolean itemExists(String id, String type) {
        if (type == "anime" || type == "manga") {
            Cursor cursor = db.rawQuery("select 1 from " + type + " where recordID=?",
                    new String[] { id });
            boolean exists = (cursor.getCount() > 0);
            cursor.close();
            return exists;
        }
        else
        {
            throw new RuntimeException("itemExists called with unknown type.");
        }
    }

    public boolean writeDetailsToMAL(GenericMALRecord gr, String type)
    {
        boolean success = false;

        if (gr.FLAG_DELETE) {
            HttpDelete deleteRequest;
            HttpResponse response;
            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

            if ("anime".equals(type)) {
                deleteRequest = new HttpDelete(APIProvider + writeAnimeDetailsAPI + gr.getID());
            }
            else {
                deleteRequest = new HttpDelete(APIProvider + writeMangaDetailsAPI + gr.getID());
            }

            deleteRequest.setHeader("Authorization", "basic " + Base64.encodeToString((malUser + ":" + malPass).getBytes(), Base64.NO_WRAP));

            try
            {
                response = client.execute(deleteRequest);

                if (200 == response.getStatusLine().getStatusCode())
                {
                    success = true;
                }

            }
            catch (ClientProtocolException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else {

            if (type.equals(TYPE_ANIME))
            {
                HttpPut writeRequest;
                HttpResponse response;
                HttpClient client = new DefaultHttpClient();
                client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);


                writeRequest = new HttpPut(APIProvider + writeAnimeDetailsAPI + gr.getID());
                writeRequest.setHeader("Authorization", "basic " + Base64.encodeToString((malUser + ":" + malPass).getBytes(), Base64.NO_WRAP));

                List<NameValuePair> putParams = new ArrayList<NameValuePair>();
                putParams.add(new BasicNameValuePair("status", gr.getMyStatus()));
                putParams.add(new BasicNameValuePair("episodes", Integer.toString(gr.getPersonalProgress())));
                putParams.add(new BasicNameValuePair("score", gr.getMyScoreString()));

                try
                {
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(putParams);
                    writeRequest.setEntity(entity);

                    response = client.execute(writeRequest);

                    if (200 == response.getStatusLine().getStatusCode())
                    {
                        success = true;
                    }

                }
                catch (ClientProtocolException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            else if (type.equals(TYPE_MANGA))
            {
                HttpPut writeRequest;
                HttpResponse response;
                HttpClient client = new DefaultHttpClient();
                client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

                writeRequest = new HttpPut(APIProvider + writeMangaDetailsAPI + gr.getID());
                writeRequest.setHeader("Authorization", "basic " + Base64.encodeToString((malUser + ":" + malPass).getBytes(), Base64.NO_WRAP));

                List<NameValuePair> putParams = new ArrayList<NameValuePair>();
                putParams.add(new BasicNameValuePair("status", gr.getMyStatus()));
                putParams.add(new BasicNameValuePair("chapters", Integer.toString(gr.getPersonalProgress())));
                putParams.add(new BasicNameValuePair("volumes", Integer.toString(((MangaRecord) gr).getVolumeProgress())));
                putParams.add(new BasicNameValuePair("score", gr.getMyScoreString()));

                try
                {
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(putParams);
                    writeRequest.setEntity(entity);

                    response = client.execute(writeRequest);

                    if (200 == response.getStatusLine().getStatusCode())
                    {
                        success = true;
                    }

                }
                catch (ClientProtocolException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return success;
    }

    public String watchedCounterBuilder(int watched, int total)
    {
        String built = "";

        if (total != 0)
        {
            built = watched + " / " + total;
        }
        else
        {
            built = watched + " / ?";
        }


        return built;
    }

    public void clearDeletedItems(String type, long currentTime) {
        Log.v("MALX", "Removing deleted items of type " + type + " older than " + DateFormat.getDateTimeInstance().format(currentTime * 1000));

        int recordsRemoved = db.delete(type, "lastUpdate < ?", new String[] {String.valueOf(currentTime)});

        Log.v("MALX", "Removed " + recordsRemoved + " "+ type + " items");
    }

    public boolean deleteItemFromDatabase(String type, int recordID) {
        int deleted = db.delete(type, "recordID = ?", new String[] {String.valueOf(recordID)});

        if (deleted == 1 ) {
            return true;
        }
        else {
            return false;
        }
    }

}
