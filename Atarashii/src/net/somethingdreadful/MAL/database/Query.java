package net.somethingdreadful.MAL.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import net.somethingdreadful.MAL.api.MALModels.RecordStub;

import java.util.ArrayList;
import java.util.List;

public class Query {
    String queryString = "";
    private static SQLiteDatabase db;

    public static Query newQuery(SQLiteDatabase db) {
        Query.db = db;
        return new Query();
    }

    public Query selectFrom(String column, String table) {
        queryString += " SELECT " + column + " FROM " + table;
        return this;
    }

    public Query innerJoinOn(String table1, String column1, String column2) {
        queryString += " INNER JOIN " + table1 + " ON " + column1 + " = " + column2;
        return this;
    }

    public Query where(String column1, String value) {
        queryString += " WHERE " + column1 + " = '" + value + "'";
        return this;
    }

    public Query whereEqGr(String column1, String value) {
        queryString += " WHERE " + column1 + " >= '" + value + "'";
        return this;
    }

    public Query isNotNull(String column1) {
        queryString += " WHERE " + column1 + " IS NOT NULL ";
        return this;
    }

    public Query andEquals(String column1, String value) {
        queryString += " AND " + column1 + " = '" + value + "'";
        return this;
    }

    public Query OrderBy(int type, String column) {
        switch (type) {
            case 1: // Name
                queryString += " ORDER BY " + column + " COLLATE NOCASE";
                break;
        }
        return this;
    }

    public Cursor run() {
        try {
            Cursor cursor = db.rawQuery(queryString, new String[]{});
            queryString = "";
            return cursor;
        } catch (Exception e) {
            log("run", e.getMessage(), true);
        }
        return null;
    }

    /**
     * Update or insert records.
     *
     * @param table The table where the record should be updated
     * @param cv    The ContentValues which should be updated
     * @param id    The ID of the record
     */
    public int updateRecord(String table, ContentValues cv, int id) {
        int updateResult = db.update(table, cv, DatabaseTest.COLUMN_ID + " = " + id, new String[]{});
        if (updateResult == 0)
            return (int) db.insert(table, null, cv);
        return updateResult;
    }

    /**
     * Update or insert records.
     *
     * @param table    The table where the record should be updated
     * @param cv       The ContentValues which should be updated
     * @param username The username of the record
     */
    public int updateRecord(String table, ContentValues cv, String username) {
        int updateResult = db.update(table, cv, "username" + " = '" + username + "'", new String[]{});
        if (updateResult == 0)
            return (int) db.insert(table, null, cv);
        return updateResult;
    }

    /**
     * The query to string.
     *
     * @return String Query
     */
    @Override
    public String toString() {
        return queryString;
    }

    /**
     * Update relations.
     *
     * @param table        The relation table name
     * @param relationType The relation type
     * @param id           The record id which should be related with
     * @param recordStubs  The records
     */
    public void updateRelation(String table, String relationType, int id, List<RecordStub> recordStubs) {
        if (id <= 0)
            log("updateRelation", "error saving relation: id <= 0", true);
        if (recordStubs == null || recordStubs.size() == 0)
            return;

        boolean relatedRecordExists;
        String recordTable;

        try {
            for (RecordStub relation : recordStubs) {
                recordTable = relation.isAnime() ? DatabaseTest.TABLE_ANIME : DatabaseTest.TABLE_MANGA;
                if (!recordExists(DatabaseTest.COLUMN_ID, recordTable, String.valueOf(relation.getId()))) {
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseTest.COLUMN_ID, relation.getId());
                    cv.put("title", relation.getTitle());
                    relatedRecordExists = db.insert(recordTable, null, cv) > 0;
                } else {
                    relatedRecordExists = true;
                }

                if (relatedRecordExists) {
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseTest.COLUMN_ID, id);
                    cv.put("relationId", relation.getId());
                    cv.put("relationType", relationType);
                    db.replace(table, null, cv);
                } else {
                    log("updateRelation", "error saving relation: record does not exist", true);
                }
            }
        } catch (Exception e) {
            log("updateRelation", e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * update Links for records.
     *
     * @param id       The anime/manga ID
     * @param list     Arraylist of strings
     * @param refTable The table where the references will be placed
     * @param table    The table where the records will be placed
     * @param column   The references column name
     *
    Query.newQuery(db).updateLink(DatabaseTest.TABLE_GENRES, DatabaseTest.TABLE_ANIME_GENRES, anime.getId(), anime.getGenres(), "genre_id");
     */
    public void updateLink(String table, String refTable, int id, ArrayList<String> list, String column) {
        if (id <= 0)
            log("updateLink", "error saving relation: id <= 0", true);
        if (list == null || list.size() == 0)
            return;

        try {
            for (String item : list) {
                String columnID = refTable.contains("anime") ? "anime_id" : "manga_id";

                // delete old links
                db.delete(refTable, columnID + " = ?", new String[]{String.valueOf(id)});
                int linkID = getRecordId(table, item);

                if (linkID != -1) {
                    // get the refID
                    ContentValues gcv = new ContentValues();
                    gcv.put(columnID, id);
                    gcv.put(column, linkID);
                    db.insert(refTable, null, gcv);
                }
            }
        } catch (Exception e) {
            log("updateRelation", e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private int getRecordId(String table, String item) {
        Integer result = null;
        Cursor cursor = Query.newQuery(db).selectFrom("*", table).where("title", item).run();
        if (cursor.moveToFirst())
            result = cursor.getInt(0);
        cursor.close();

        if (result == null) {
            ContentValues cv = new ContentValues();
            cv.put("title", item);
            Long addResult = db.insert(table, null, cv);
            if (addResult > -1)
                result = addResult.intValue();
        }

        return result == null ? -1 : result;
    }

    /**
     * Log events.
     *
     * @param method  The method name to find easy crashes
     * @param message The thrown message
     * @param error   True if it is an error else false
     */
    @SuppressWarnings("deprecation")
    private void log(String method, String message, boolean error) {
        Crashlytics.log(error ? Log.ERROR : Log.INFO, "MALX", "Query." + method + "(" + toString() + "): " + message);
    }

    /**
     * Check if a records already exists.
     *
     * @param column      The column where we can find the record
     * @param columnValue The ID
     * @param table       The table where we can find the record
     * @return boolean True if it exists
     */
    private boolean recordExists(String column, String table, String columnValue) {
        Cursor cursor = selectFrom(column, table).where(column, columnValue).run();
        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    public ArrayList<RecordStub> getRelation(Integer Id, String relationTable, String relationType, boolean anime) {
        ArrayList<RecordStub> result = null;

        try {
            String name = "mr.title";
            String id = "mr." + DatabaseTest.COLUMN_ID;

            Cursor cursor = selectFrom(id + ", " + name, (anime ? DatabaseTest.TABLE_ANIME : DatabaseTest.TABLE_MANGA) + " mr")
                    .innerJoinOn(relationTable + " rr", id, "rr.relationId")
                    .where("rr." + DatabaseTest.COLUMN_ID, String.valueOf(Id)).andEquals("rr.relationType", relationType).run();

            if (cursor != null && cursor.moveToFirst()) {
                result = new ArrayList<>();
                do {
                    RecordStub recordStub = new RecordStub();
                    recordStub.setId(cursor.getInt(0), anime);
                    recordStub.setTitle(cursor.getString(1));
                    result.add(recordStub);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            log("getRelation", e.getMessage(), true);
        }
        return result;
    }

    public ArrayList<String> getArrayList(int id, String relTable, String table, String column, boolean anime) {
        ArrayList<String> result = null;

        try {
            String recID = (anime ? DatabaseTest.TABLE_ANIME : DatabaseTest.TABLE_MANGA) + DatabaseTest.COLUMN_ID;
            Cursor cursor = selectFrom(recID, relTable + " g")
                    .innerJoinOn(table + " ag", "ag." + column, "g." + DatabaseTest.COLUMN_ID)
                    .where(anime ? "ag.anime_id" : "ag.manga_id", String.valueOf(id)).run();

            if (cursor != null && cursor.moveToFirst()) {
                result = new ArrayList<>();
                do
                    result.add(cursor.getString(0));
                while
                        (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            log("getArrayList", e.getMessage(), true);
        }
        return result;
    }
}