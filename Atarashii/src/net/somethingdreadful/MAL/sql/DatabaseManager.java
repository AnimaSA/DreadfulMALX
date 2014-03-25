package net.somethingdreadful.MAL.sql;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import net.somethingdreadful.MAL.api.response.Anime;
import net.somethingdreadful.MAL.api.response.Manga;
import net.somethingdreadful.MAL.api.response.User;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DatabaseManager {

	public final String[] ANIMECOLUMNS = {"recordID", "recordName", "recordType", "recordStatus", "myStatus",
			"episodesWatched", "episodesTotal", "memberScore", "myScore", "synopsis", "imageUrl", "dirty", "lastUpdate"};

	private final String[] MANGACOLUMNS = {"recordID", "recordName", "recordType", "recordStatus", "myStatus",
			"volumesRead", "chaptersRead", "volumesTotal", "chaptersTotal", "memberScore", "myScore", "synopsis",
			"imageUrl", "dirty", "lastUpdate"};
	
	private final String[] FRIENDSCOLUMNS = {"username", "avatar_url", "last_online", "friend_since"};

    private final String[] PROFILECOLUMNS = {"username", "avatar_url", "birthday", "location", "website", "comments", "forum_posts",
            "last_online", "gender", "join_date", "access_rank", "anime_list_views", "manga_list_views",
            "anime_time_days", "anime_watching", "anime_completed", "anime_on_hold", "anime_dropped",  //anime
            "anime_plan_to_watch", "anime_total_entries",
            "manga_time_days", "manga_reading", "manga_completed", "manga_on_hold", "manga_dropped",   //manga
            "manga_plan_to_read",   "manga_total_entries"};

	static MALSqlHelper malSqlHelper;
	static SQLiteDatabase dbRead;

	public DatabaseManager(Context context) {
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

	public static Date parseSQLDateString(String date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		try {
			Date result = sdf.parse(date);
			return result;
		} catch (ParseException e) {
			Log.e("MALX", "Parsing datetime failed", e);
			return null;
		}
	}

	public void saveAnimeList(ArrayList<Anime> list) {
		if ( list != null && list.size() > 0 ) {
			try {
				getDBWrite().beginTransaction();
				for(Anime anime: list)
					saveAnime(anime, true);
				getDBWrite().setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("MALX", "error saving animelist to db");
			} finally {
				getDBWrite().endTransaction();
			}
		}
	}

	public void saveAnime(Anime anime, boolean ignoreSynopsis) {
		ContentValues cv = new ContentValues();

		cv.put("recordID", anime.getId());
		cv.put("recordName", anime.getTitle());
		cv.put("recordType", anime.getType());
		cv.put("imageUrl", anime.getImageUrl());
		cv.put("recordStatus", anime.getStatus());
		cv.put("myStatus", anime.getWatchedStatus());
		cv.put("memberScore", anime.getMembersScore());
		cv.put("myScore", anime.getScore());
		cv.put("episodesWatched", anime.getWatchedEpisodes());
		cv.put("episodesTotal", anime.getEpisodes());
		cv.put("dirty", anime.getDirty());
		if ( anime.getLastUpdate() != null )
			cv.put("lastUpdate", anime.getLastUpdate().getTime());

		if (!ignoreSynopsis) {
			cv.put("synopsis", anime.getSynopsis());
		}
		getDBWrite().replace(MALSqlHelper.TABLE_ANIME, null, cv);
	}

	public Anime getAnime(int id) {
		Anime result = null;
		Cursor cursor = getDBRead().query(MALSqlHelper.TABLE_ANIME, ANIMECOLUMNS, "recordID = ?", new String[]{Integer.toString(id)}, null, null, null);
		if (cursor.moveToFirst())
			result = Anime.fromCursor(cursor);
		cursor.close();
		return result;
	}

	public boolean deleteAnime(int id) {
		return getDBWrite().delete(MALSqlHelper.TABLE_ANIME, "recordID = ?", new String[]{String.valueOf(id)}) == 1;
	}

	public ArrayList<Anime> getAnimeList() {
		return getAnimeList("watching");
	}

	public ArrayList<Anime> getAnimeList(String listType) {
		if ( listType == "" )
			return getAnimeList(null, null);
		else
			return getAnimeList("myStatus = ?", new String[]{listType});
	}

	public ArrayList<Anime> getDirtyAnimeList() {
		return getAnimeList("dirty = ?", new String[]{"1"});
	}

	private ArrayList<Anime> getAnimeList(String selection, String[] selectionArgs) {
		ArrayList<Anime> result = null;
		Cursor cursor;
		try {
			cursor = getDBRead().query(MALSqlHelper.TABLE_ANIME, ANIMECOLUMNS, selection, selectionArgs, null, null, "recordName COLLATE NOCASE");
			if (cursor.moveToFirst())
			{
				result = new ArrayList<Anime>();
				do {
					result.add(Anime.fromCursor(cursor));
				} while (cursor.moveToNext());
			}
			cursor.close();
		} catch (SQLException e) {
			Log.e("MALX", "DatabaseManager.getAnimeList exception: " + e.getMessage());
		}

		return result;
	}

	// replacement for clearDeletedItems, with imo better describing name
	public int clearOldAnimeRecords(Date time) {
		return getDBWrite().delete(MALSqlHelper.TABLE_ANIME, "lastUpdate < ?", new String[]{time.toString()});
	}

	public void saveMangaList(ArrayList<Manga> list) {
		if ( list != null && list.size() > 0 ) {
			try {
				getDBWrite().beginTransaction();
				for(Manga manga: list)
					saveManga(manga, true);
				getDBWrite().setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("MALX", "error saving mangalist to db: " + e.getMessage());
			} finally {
				getDBWrite().endTransaction();
			}
		}
	}

	public void saveManga(Manga manga, boolean ignoreSynopsis) {
		ContentValues cv = new ContentValues();

		cv.put("recordID", manga.getId());
		cv.put("recordName", manga.getTitle());
		cv.put("recordType", manga.getType());
		cv.put("imageUrl", manga.getImageUrl());
		cv.put("recordStatus", manga.getStatus());
		cv.put("myStatus", manga.getReadStatus());
		cv.put("memberScore", manga.getMembersScore());
		cv.put("myScore", manga.getScore());
		cv.put("volumesRead", manga.getVolumesRead());
		cv.put("chaptersRead", manga.getChaptersRead());
		cv.put("volumesTotal", manga.getVolumes());
		cv.put("chaptersTotal", manga.getChapters());
		cv.put("dirty", manga.getDirty());
		if (manga.getLastUpdate() != null)
			cv.put("lastUpdate", manga.getLastUpdate().getTime());

		if (!ignoreSynopsis) {
			cv.put("synopsis", manga.getSynopsis());
		}

		getDBWrite().replace(MALSqlHelper.TABLE_MANGA, null, cv);
	}

	public Manga getManga(int id) {
		Manga result = null;
		Cursor cursor = getDBRead().query(MALSqlHelper.TABLE_MANGA, MANGACOLUMNS, "recordID = ?", new String[]{Integer.toString(id)}, null, null, null);
		if (cursor.moveToFirst())
			result = Manga.fromCursor(cursor);
		cursor.close();
		return result;
	}

	public boolean deleteManga(int id) {
		return getDBWrite().delete(MALSqlHelper.TABLE_MANGA, "recordID = ?", new String[]{String.valueOf(id)}) == 1;
	}

	public ArrayList<Manga> getMangaList() {
		return getMangaList("reading");
	}

	public ArrayList<Manga> getMangaList(String listType) {
		if ( listType == "" )
			return getMangaList(null, null);
		else
			return getMangaList("myStatus = ?", new String[]{listType});
	}

	public ArrayList<Manga> getDirtyMangaList() {
		return getMangaList("dirty = ?", new String[]{"1"});
	}

	private ArrayList<Manga> getMangaList(String selection, String[] selectionArgs) {
		ArrayList<Manga> result = null;
		Cursor cursor;
		try {
			cursor = getDBRead().query(MALSqlHelper.TABLE_MANGA, MANGACOLUMNS, selection, selectionArgs, null, null, "recordName COLLATE NOCASE");
			if (cursor.moveToFirst())
			{
				result = new ArrayList<Manga>();
				do {
					result.add(Manga.fromCursor(cursor));
				} while (cursor.moveToNext());
			}
			cursor.close();
		} catch (SQLException e) {
			Log.e("MALX", "DatabaseManager.getMangaList exception: " + e.getMessage());
		}

		return result;
	}

	// replacement for clearDeletedItems, with imo better describing name
	public int clearOldMangaRecords(Date time) {
		return getDBWrite().delete(MALSqlHelper.TABLE_MANGA, "lastUpdate < ?", new String[]{time.toString()});
	}

    public void saveFriendList(ArrayList<User> list) {
        if ( list != null && list.size() > 0 ) {
            try {
            	getDBWrite().delete(MALSqlHelper.TABLE_FRIENDS, null, new String[]{});
                getDBWrite().beginTransaction();
                for(User friend: list)
                    saveFriend(friend);
                getDBWrite().setTransactionSuccessful();
            } catch (Exception e) {
                Log.e("MALX", "error saving friendlist to db: " + e.getMessage());
            } finally {
                getDBWrite().endTransaction();
            }
        }
    }
 
    public void saveFriend(User friend) {
        ContentValues cv = new ContentValues();
        
        cv.put("username", friend.getName());
        cv.put("avatar_url", friend.getProfile().getAvatarUrl());
        if ( friend.getProfile().getDetails().getLastOnline() != null )
            cv.put("last_online", friend.getProfile().getDetails().getLastOnline());
        else
            cv.putNull("last_online");
        if ( friend.getFriendSince() != null )
            cv.put("friend_since", friend.getFriendSince());
        else
            cv.putNull("friend_since");
        getDBWrite().replace(MALSqlHelper.TABLE_FRIENDS, null, cv);
    }
    
    public ArrayList<User> getFriendList() {
        ArrayList<User> result = null;
        Cursor cursor;
        try {
            cursor = getDBRead().query(MALSqlHelper.TABLE_FRIENDS, FRIENDSCOLUMNS, null, null, null, null, "username COLLATE NOCASE");
            if (cursor.moveToFirst())
            {
                result = new ArrayList<User>();
                do {
                    result.add(User.fromCursor(cursor, true));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (SQLException e) {
            Log.e("MALX", "DatabaseManager.getFriendList exception: " + e.getMessage());
        }
        
        return result;
    }
    
    public void saveUser(User user) {
        ContentValues cv = new ContentValues();
        
        cv.put("username", user.getName());
        cv.put("avatar_url", user.getProfile().getAvatarUrl());
        if ( user.getProfile().getDetails().getBirthday() != null )
            cv.put("birthday", user.getProfile().getDetails().getBirthday());
        else
            cv.putNull("birthday");
        cv.put("location", user.getProfile().getDetails().getLocation());
        cv.put("website", user.getProfile().getDetails().getWebsite());
        cv.put("comments", user.getProfile().getDetails().getComments());
        cv.put("forum_posts", user.getProfile().getDetails().getForumPosts());
        if ( user.getProfile().getDetails().getLastOnline() != null )
            cv.put("last_online", user.getProfile().getDetails().getLastOnline());
        else
            cv.putNull("last_online");
        cv.put("gender", user.getProfile().getDetails().getGender());
        if ( user.getProfile().getDetails().getJoinDate() != null )
            cv.put("join_date", user.getProfile().getDetails().getJoinDate());
        else
            cv.putNull("join_date");
        cv.put("access_rank", user.getProfile().getDetails().getAccessRank());
        cv.put("anime_list_views", user.getProfile().getDetails().getAnimeListViews());
        cv.put("manga_list_views", user.getProfile().getDetails().getMangaListViews());

        cv.put("anime_time_days", user.getProfile().getAnimeStats().getTimeDays());
        cv.put("anime_watching", user.getProfile().getAnimeStats().getWatching());
        cv.put("anime_completed", user.getProfile().getAnimeStats().getCompleted());
        cv.put("anime_on_hold", user.getProfile().getAnimeStats().getOnHold());
        cv.put("anime_dropped", user.getProfile().getAnimeStats().getDropped());
        cv.put("anime_plan_to_watch", user.getProfile().getAnimeStats().getPlanToWatch());
        cv.put("anime_total_entries", user.getProfile().getAnimeStats().getTotalEntries());

        cv.put("manga_time_days", user.getProfile().getMangaStats().getTimeDays());
        cv.put("manga_reading", user.getProfile().getMangaStats().getReading());
        cv.put("manga_completed", user.getProfile().getMangaStats().getCompleted());
        cv.put("manga_on_hold", user.getProfile().getMangaStats().getOnHold());
        cv.put("manga_dropped", user.getProfile().getMangaStats().getDropped());
        cv.put("manga_plan_to_read", user.getProfile().getMangaStats().getPlanToRead());
        cv.put("manga_total_entries", user.getProfile().getMangaStats().getTotalEntries());
        
        getDBWrite().replace(MALSqlHelper.TABLE_PROFILE, null, cv);
    }

    public User getProfile(String name) {
        User result = null;
        Cursor cursor;
        try {
            cursor = getDBRead().query(MALSqlHelper.TABLE_PROFILE, PROFILECOLUMNS, "username = ?", new String[]{name}, null, null, null);
            if (cursor.moveToFirst())
                result = User.fromCursor(cursor);
            cursor.close();
        } catch (SQLException e) {
            Log.e("MALX", "DatabaseManager.getProfile exception: " + e.getMessage());
        }
        return result;
    }
}
