/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.provider;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.support.v4.text.BidiFormatter;
import android.support.v4.util.LongSparseArray;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.otto.Bus;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.mariotaku.sqliteqb.library.Columns.Column;
import org.mariotaku.sqliteqb.library.Expression;
import org.mariotaku.sqliteqb.library.OrderBy;
import org.mariotaku.sqliteqb.library.RawItemArray;
import org.mariotaku.sqliteqb.library.SQLConstants;
import org.mariotaku.sqliteqb.library.SQLFunctions;
import org.mariotaku.sqliteqb.library.query.SQLSelectQuery;
import org.mariotaku.twidere.BuildConfig;
import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.activity.support.HomeActivity;
import org.mariotaku.twidere.annotation.CustomTabType;
import org.mariotaku.twidere.annotation.NotificationType;
import org.mariotaku.twidere.annotation.ReadPositionTag;
import org.mariotaku.twidere.api.twitter.model.Activity;
import org.mariotaku.twidere.app.TwidereApplication;
import org.mariotaku.twidere.model.AccountPreferences;
import org.mariotaku.twidere.model.ActivityTitleSummaryMessage;
import org.mariotaku.twidere.model.Draft;
import org.mariotaku.twidere.model.DraftCursorIndices;
import org.mariotaku.twidere.model.ParcelableActivity;
import org.mariotaku.twidere.model.ParcelableActivityCursorIndices;
import org.mariotaku.twidere.model.ParcelableUser;
import org.mariotaku.twidere.model.StringLongPair;
import org.mariotaku.twidere.model.UnreadItem;
import org.mariotaku.twidere.model.message.UnreadCountUpdatedEvent;
import org.mariotaku.twidere.model.util.ParcelableActivityUtils;
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts;
import org.mariotaku.twidere.provider.TwidereDataStore.Activities;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedHashtags;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedImages;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedRelationships;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedStatuses;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedUsers;
import org.mariotaku.twidere.provider.TwidereDataStore.DNS;
import org.mariotaku.twidere.provider.TwidereDataStore.DirectMessages;
import org.mariotaku.twidere.provider.TwidereDataStore.Drafts;
import org.mariotaku.twidere.provider.TwidereDataStore.Notifications;
import org.mariotaku.twidere.provider.TwidereDataStore.Permissions;
import org.mariotaku.twidere.provider.TwidereDataStore.Preferences;
import org.mariotaku.twidere.provider.TwidereDataStore.SavedSearches;
import org.mariotaku.twidere.provider.TwidereDataStore.SearchHistory;
import org.mariotaku.twidere.provider.TwidereDataStore.Statuses;
import org.mariotaku.twidere.provider.TwidereDataStore.Suggestions;
import org.mariotaku.twidere.provider.TwidereDataStore.UnreadCounts;
import org.mariotaku.twidere.receiver.NotificationReceiver;
import org.mariotaku.twidere.service.BackgroundOperationService;
import org.mariotaku.twidere.util.ActivityTracker;
import org.mariotaku.twidere.util.AsyncTwitterWrapper;
import org.mariotaku.twidere.util.DataStoreUtils;
import org.mariotaku.twidere.util.ImagePreloader;
import org.mariotaku.twidere.util.NotificationManagerWrapper;
import org.mariotaku.twidere.util.ParseUtils;
import org.mariotaku.twidere.util.PermissionsManager;
import org.mariotaku.twidere.util.ReadStateManager;
import org.mariotaku.twidere.util.SQLiteDatabaseWrapper;
import org.mariotaku.twidere.util.SQLiteDatabaseWrapper.LazyLoadCallback;
import org.mariotaku.twidere.util.SharedPreferencesWrapper;
import org.mariotaku.twidere.util.TwidereArrayUtils;
import org.mariotaku.twidere.util.TwidereListUtils;
import org.mariotaku.twidere.util.TwidereQueryBuilder.CachedUsersQueryBuilder;
import org.mariotaku.twidere.util.TwidereQueryBuilder.ConversationQueryBuilder;
import org.mariotaku.twidere.util.UriExtraUtils;
import org.mariotaku.twidere.util.UserColorNameManager;
import org.mariotaku.twidere.util.Utils;
import org.mariotaku.twidere.util.collection.CompactHashSet;
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper;
import org.mariotaku.twidere.util.media.preview.PreviewMediaExtractor;
import org.mariotaku.twidere.util.net.TwidereDns;
import org.oshkimaadziig.george.androidutils.SpanFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public final class TwidereDataProvider extends ContentProvider implements Constants, OnSharedPreferenceChangeListener,
        LazyLoadCallback {

    public static final String TAG_OLDEST_MESSAGES = "oldest_messages";
    private static final Pattern PATTERN_SCREEN_NAME = Pattern.compile("(?i)[@\uFF20]?([a-z0-9_]{1,20})");
    @Inject
    ReadStateManager mReadStateManager;
    @Inject
    AsyncTwitterWrapper mTwitterWrapper;
    @Inject
    ImageLoader mMediaLoader;
    @Inject
    NotificationManagerWrapper mNotificationManager;
    @Inject
    SharedPreferencesWrapper mPreferences;
    @Inject
    TwidereDns mDns;
    @Inject
    Bus mBus;
    @Inject
    UserColorNameManager mUserColorNameManager;
    @Inject
    BidiFormatter mBidiFormatter;
    @Inject
    ActivityTracker mActivityTracker;
    @Inject
    PermissionsManager mPermissionsManager;

    private Handler mHandler;
    private ContentResolver mContentResolver;
    private SQLiteDatabaseWrapper mDatabaseWrapper;
    private ImagePreloader mImagePreloader;
    private Executor mBackgroundExecutor;
    private boolean mNameFirst;
    private boolean mUseStarForLikes;

    private static PendingIntent getMarkReadDeleteIntent(Context context, @NotificationType String type,
                                                         long accountId, long position, boolean extraUserFollowing) {
        return getMarkReadDeleteIntent(context, type, accountId, position, -1, -1, extraUserFollowing);
    }

    private static PendingIntent getMarkReadDeleteIntent(Context context, @NotificationType String type,
                                                         long accountId, long position, long extraId,
                                                         long extraUserId, boolean extraUserFollowing) {
        // Setup delete intent
        final Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(BROADCAST_NOTIFICATION_DELETED);
        final Uri.Builder linkBuilder = new Uri.Builder();
        linkBuilder.scheme(SCHEME_TWIDERE);
        linkBuilder.authority(AUTHORITY_NOTIFICATIONS);
        linkBuilder.appendPath(type);
        linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, String.valueOf(position));
        linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, type);

        UriExtraUtils.addExtra(linkBuilder, "item_id", extraId);
        UriExtraUtils.addExtra(linkBuilder, "item_user_id", extraUserId);
        UriExtraUtils.addExtra(linkBuilder, "item_user_following", extraUserFollowing);
        intent.setData(linkBuilder.build());
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getMarkReadDeleteIntent(Context context, @NotificationType String notificationType,
                                                         long accountId, StringLongPair[] positions) {
        // Setup delete intent
        final Intent intent = new Intent(context, NotificationReceiver.class);
        final Uri.Builder linkBuilder = new Uri.Builder();
        linkBuilder.scheme(SCHEME_TWIDERE);
        linkBuilder.authority(AUTHORITY_NOTIFICATIONS);
        linkBuilder.appendPath(notificationType);
        linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITIONS, StringLongPair.toString(positions));
        linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, notificationType);
        intent.setData(linkBuilder.build());
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static Cursor getPreferencesCursor(final SharedPreferencesWrapper preferences, final String key) {
        final MatrixCursor c = new MatrixCursor(Preferences.MATRIX_COLUMNS);
        final Map<String, Object> map = new HashMap<>();
        final Map<String, ?> all = preferences.getAll();
        if (key == null) {
            map.putAll(all);
        } else {
            map.put(key, all.get(key));
        }
        for (final Map.Entry<String, ?> item : map.entrySet()) {
            final Object value = item.getValue();
            final int type = getPreferenceType(value);
            c.addRow(new Object[]{item.getKey(), ParseUtils.parseString(value), type});
        }
        return c;
    }

    private static int getPreferenceType(final Object object) {
        if (object == null)
            return Preferences.TYPE_NULL;
        else if (object instanceof Boolean)
            return Preferences.TYPE_BOOLEAN;
        else if (object instanceof Integer)
            return Preferences.TYPE_INTEGER;
        else if (object instanceof Long)
            return Preferences.TYPE_LONG;
        else if (object instanceof Float)
            return Preferences.TYPE_FLOAT;
        else if (object instanceof String) return Preferences.TYPE_STRING;
        return Preferences.TYPE_INVALID;
    }

    private static int getUnreadCount(final List<UnreadItem> set, final long... accountIds) {
        if (set == null || set.isEmpty()) return 0;
        int count = 0;
        for (final UnreadItem item : set.toArray(new UnreadItem[set.size()])) {
            if (item != null && ArrayUtils.contains(accountIds, item.account_id)) {
                count++;
            }
        }
        return count;
    }

    private static boolean shouldReplaceOnConflict(final int table_id) {
        switch (table_id) {
            case TABLE_ID_CACHED_HASHTAGS:
            case TABLE_ID_CACHED_STATUSES:
            case TABLE_ID_CACHED_USERS:
            case TABLE_ID_CACHED_RELATIONSHIPS:
            case TABLE_ID_SEARCH_HISTORY:
            case TABLE_ID_FILTERED_USERS:
            case TABLE_ID_FILTERED_KEYWORDS:
            case TABLE_ID_FILTERED_SOURCES:
            case TABLE_ID_FILTERED_LINKS:
                return true;
        }
        return false;
    }

    @Override
    public int bulkInsert(@NonNull final Uri uri, @NonNull final ContentValues[] valuesArray) {
        try {
            return bulkInsertInternal(uri, valuesArray);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return bulkInsertInternal(uri, valuesArray);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private boolean handleSQLException(SQLException e) {
        try {
            if (e instanceof SQLiteFullException) {
                // Drop cached databases
                mDatabaseWrapper.delete(CachedUsers.TABLE_NAME, null, null);
                mDatabaseWrapper.delete(CachedStatuses.TABLE_NAME, null, null);
                mDatabaseWrapper.delete(CachedHashtags.TABLE_NAME, null, null);
                mDatabaseWrapper.execSQL("VACUUM");
                return true;
            }
        } catch (SQLException ee) {
            throw new IllegalStateException(ee);
        }
        throw new IllegalStateException(e);
    }

    private int bulkInsertInternal(@NonNull Uri uri, @NonNull ContentValues[] valuesArray) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        checkWritePermission(tableId, table);
        switch (tableId) {
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
            case TABLE_ID_DIRECT_MESSAGES:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
                return 0;
        }
        int result = 0;
        final long[] newIds = new long[valuesArray.length];
        if (table != null && valuesArray.length > 0) {
            mDatabaseWrapper.beginTransaction();
            if (tableId == TABLE_ID_CACHED_USERS) {
                for (final ContentValues values : valuesArray) {
                    final Expression where = Expression.equals(CachedUsers.USER_ID,
                            values.getAsLong(CachedUsers.USER_ID));
                    mDatabaseWrapper.update(table, values, where.getSQL(), null);
                    newIds[result++] = mDatabaseWrapper.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            } else if (tableId == TABLE_ID_SEARCH_HISTORY) {
                for (final ContentValues values : valuesArray) {
                    values.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis());
                    final Expression where = Expression.equalsArgs(SearchHistory.QUERY);
                    final String[] args = {values.getAsString(SearchHistory.QUERY)};
                    mDatabaseWrapper.update(table, values, where.getSQL(), args);
                    newIds[result++] = mDatabaseWrapper.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_IGNORE);
                }
            } else if (shouldReplaceOnConflict(tableId)) {
                for (final ContentValues values : valuesArray) {
                    newIds[result++] = mDatabaseWrapper.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            } else {
                for (final ContentValues values : valuesArray) {
                    newIds[result++] = mDatabaseWrapper.insert(table, null, values);
                }
            }
            mDatabaseWrapper.setTransactionSuccessful();
            mDatabaseWrapper.endTransaction();
        }
        if (result > 0) {
            onDatabaseUpdated(tableId, uri);
        }
        onNewItemsInserted(uri, tableId, valuesArray);
        return result;
    }

    @Override
    public int delete(@NonNull final Uri uri, final String selection, final String[] selectionArgs) {
        try {
            return deleteInternal(uri, selection, selectionArgs);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return deleteInternal(uri, selection, selectionArgs);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private int deleteInternal(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        checkWritePermission(tableId, table);
        switch (tableId) {
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
            case TABLE_ID_DIRECT_MESSAGES:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
                return 0;
            case VIRTUAL_TABLE_ID_NOTIFICATIONS: {
                final List<String> segments = uri.getPathSegments();
                if (segments.size() == 1) {
                    clearNotification();
                } else if (segments.size() == 2) {
                    final int notificationType = NumberUtils.toInt(segments.get(1), -1);
                    clearNotification(notificationType, 0);
                } else if (segments.size() == 3) {
                    final int notificationType = NumberUtils.toInt(segments.get(1), -1);
                    final long accountId = NumberUtils.toLong(segments.get(2), -1);
                    clearNotification(notificationType, accountId);
                }
                return 1;
            }
            case VIRTUAL_TABLE_ID_UNREAD_COUNTS: {
                return 0;
            }
        }
        if (table == null) return 0;
        final int result = mDatabaseWrapper.delete(table, selection, selectionArgs);
        if (result > 0) {
            onDatabaseUpdated(tableId, uri);
        }
        return result;
    }

    @Override
    public String getType(@NonNull final Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues values) {
        try {
            return insertInternal(uri, values);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return insertInternal(uri, values);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private Uri insertInternal(@NonNull Uri uri, ContentValues values) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        checkWritePermission(tableId, table);
        switch (tableId) {
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
            case TABLE_ID_DIRECT_MESSAGES:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
                return null;
        }
        final long rowId;
        if (tableId == TABLE_ID_CACHED_USERS) {
            final Expression where = Expression.equals(CachedUsers.USER_ID,
                    values.getAsLong(CachedUsers.USER_ID));
            mDatabaseWrapper.update(table, values, where.getSQL(), null);
            rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
        } else if (tableId == TABLE_ID_SEARCH_HISTORY) {
            values.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis());
            final Expression where = Expression.equalsArgs(SearchHistory.QUERY);
            final String[] args = {values.getAsString(SearchHistory.QUERY)};
            mDatabaseWrapper.update(table, values, where.getSQL(), args);
            rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
        } else if (tableId == TABLE_ID_CACHED_RELATIONSHIPS) {
            final long accountId = values.getAsLong(CachedRelationships.ACCOUNT_ID);
            final long userId = values.getAsLong(CachedRelationships.USER_ID);
            final Expression where = Expression.and(
                    Expression.equals(CachedRelationships.ACCOUNT_ID, accountId),
                    Expression.equals(CachedRelationships.USER_ID, userId)
            );
            if (mDatabaseWrapper.update(table, values, where.getSQL(), null) > 0) {
                final String[] projection = {CachedRelationships._ID};
                final Cursor c = mDatabaseWrapper.query(table, projection, where.getSQL(), null,
                        null, null, null);
                if (c.moveToFirst()) {
                    rowId = c.getLong(0);
                } else {
                    rowId = 0;
                }
                c.close();
            } else {
                rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        } else if (tableId == VIRTUAL_TABLE_ID_DRAFTS_NOTIFICATIONS) {
            rowId = showDraftNotification(values);
        } else if (shouldReplaceOnConflict(tableId)) {
            rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } else if (table != null) {
            rowId = mDatabaseWrapper.insert(table, null, values);
        } else {
            return null;
        }
        onDatabaseUpdated(tableId, uri);
        onNewItemsInserted(uri, tableId, values);
        return Uri.withAppendedPath(uri, String.valueOf(rowId));
    }

    private long showDraftNotification(ContentValues values) {
        final Context context = getContext();
        if (values == null || context == null) return -1;
        final Long draftId = values.getAsLong(BaseColumns._ID);
        if (draftId == null) return -1;
        final Expression where = Expression.equals(Drafts._ID, draftId);
        final Cursor c = getContentResolver().query(Drafts.CONTENT_URI, Drafts.COLUMNS, where.getSQL(), null, null);
        if (c == null) return -1;
        final DraftCursorIndices i = new DraftCursorIndices(c);
        final Draft item;
        try {
            if (!c.moveToFirst()) return -1;
            item = i.newObject(c);
        } finally {
            c.close();
        }
        final String title = context.getString(R.string.status_not_updated);
        final String message = context.getString(R.string.status_not_updated_summary);
        final Intent intent = new Intent();
        intent.setPackage(BuildConfig.APPLICATION_ID);
        final Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(SCHEME_TWIDERE);
        uriBuilder.authority(AUTHORITY_DRAFTS);
        intent.setData(uriBuilder.build());
        final NotificationCompat.Builder nb = new NotificationCompat.Builder(context);
        nb.setTicker(message);
        nb.setContentTitle(title);
        nb.setContentText(item.text);
        nb.setAutoCancel(true);
        nb.setWhen(System.currentTimeMillis());
        nb.setSmallIcon(R.drawable.ic_stat_draft);
        final Intent discardIntent = new Intent(context, BackgroundOperationService.class);
        discardIntent.setAction(INTENT_ACTION_DISCARD_DRAFT);
        final Uri draftUri = Uri.withAppendedPath(Drafts.CONTENT_URI, String.valueOf(draftId));
        discardIntent.setData(draftUri);
        nb.addAction(R.drawable.ic_action_delete, context.getString(R.string.discard), PendingIntent.getService(context, 0,
                discardIntent, PendingIntent.FLAG_ONE_SHOT));

        final Intent sendIntent = new Intent(context, BackgroundOperationService.class);
        sendIntent.setAction(INTENT_ACTION_SEND_DRAFT);
        sendIntent.setData(draftUri);
        nb.addAction(R.drawable.ic_action_send, context.getString(R.string.send),
                PendingIntent.getService(context, 0, sendIntent, PendingIntent.FLAG_ONE_SHOT));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        nb.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT));
        mNotificationManager.notify(draftUri.toString(), NOTIFICATION_ID_DRAFTS,
                nb.build());
        return draftId;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        assert context != null;
        GeneralComponentHelper.build(context).inject(this);
        mHandler = new Handler(Looper.getMainLooper());
        mDatabaseWrapper = new SQLiteDatabaseWrapper(this);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        mBackgroundExecutor = Executors.newSingleThreadExecutor();
        updatePreferences();
        mImagePreloader = new ImagePreloader(context, mMediaLoader);
        // final GetWritableDatabaseTask task = new
        // GetWritableDatabaseTask(context, helper, mDatabaseWrapper);
        // task.executeTask();
        return true;
    }

    @Override
    public SQLiteDatabase onCreateSQLiteDatabase() {
        final TwidereApplication app = TwidereApplication.getInstance(getContext());
        final SQLiteOpenHelper helper = app.getSQLiteOpenHelper();
        return helper.getWritableDatabase();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        updatePreferences();
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull final Uri uri, @NonNull final String mode) throws FileNotFoundException {
        final int table_id = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(table_id);
        final int modeCode;
        switch (mode) {
            case "r":
                modeCode = ParcelFileDescriptor.MODE_READ_ONLY;
                break;
            case "rw":
                modeCode = ParcelFileDescriptor.MODE_READ_WRITE;
                break;
            case "rwt":
                modeCode = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_TRUNCATE;
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (modeCode == ParcelFileDescriptor.MODE_READ_ONLY) {
            checkReadPermission(table_id, table, null);
        } else if ((modeCode & ParcelFileDescriptor.MODE_READ_WRITE) != 0) {
            checkReadPermission(table_id, table, null);
            checkWritePermission(table_id, table);
        }
        switch (table_id) {
            case VIRTUAL_TABLE_ID_CACHED_IMAGES: {
                return getCachedImageFd(uri.getQueryParameter(QUERY_PARAM_URL));
            }
            case VIRTUAL_TABLE_ID_CACHE_FILES: {
                return getCacheFileFd(uri.getLastPathSegment());
            }
        }
        return null;
    }

    @Override
    public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
                        final String sortOrder) {
        try {
            final int tableId = DataStoreUtils.getTableId(uri);
            final String table = DataStoreUtils.getTableNameById(tableId);
            checkReadPermission(tableId, table, projection);
            switch (tableId) {
                case VIRTUAL_TABLE_ID_DATABASE_READY: {
                    if (mDatabaseWrapper.isReady())
                        return new MatrixCursor(projection != null ? projection : new String[0]);
                    return null;
                }
                case VIRTUAL_TABLE_ID_PERMISSIONS: {
                    final Context context = getContext();
                    if (context == null) return null;
                    final MatrixCursor c = new MatrixCursor(Permissions.MATRIX_COLUMNS);
                    final PackageManager pm = context.getPackageManager();
                    if (Binder.getCallingUid() == Process.myUid()) {
                        final Map<String, String> map = mPermissionsManager.getAll();
                        for (final Map.Entry<String, String> item : map.entrySet()) {
                            c.addRow(new Object[]{item.getKey(), item.getValue()});
                        }
                    } else {
                        final Map<String, String> map = mPermissionsManager.getAll();
                        final String[] callingPackages = pm.getPackagesForUid(Binder.getCallingUid());
                        for (final Map.Entry<String, String> item : map.entrySet()) {
                            final String key = item.getKey();
                            if (ArrayUtils.contains(callingPackages, key)) {
                                c.addRow(new Object[]{key, item.getValue()});
                            }
                        }
                    }
                    return c;
                }
                case VIRTUAL_TABLE_ID_ALL_PREFERENCES: {
                    return getPreferencesCursor(mPreferences, null);
                }
                case VIRTUAL_TABLE_ID_PREFERENCES: {
                    return getPreferencesCursor(mPreferences, uri.getLastPathSegment());
                }
                case VIRTUAL_TABLE_ID_DNS: {
                    return getDNSCursor(uri.getLastPathSegment());
                }
                case VIRTUAL_TABLE_ID_CACHED_IMAGES: {
                    return getCachedImageCursor(uri.getQueryParameter(QUERY_PARAM_URL));
                }
                case VIRTUAL_TABLE_ID_NOTIFICATIONS: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() == 2) {
                        final int def = -1;
                        return getNotificationsCursor(NumberUtils.toInt(segments.get(1), def));
                    } else
                        return getNotificationsCursor();
                }
                case VIRTUAL_TABLE_ID_UNREAD_COUNTS: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() == 2) {
                        final int def = -1;
                        return getUnreadCountsCursor(NumberUtils.toInt(segments.get(1), def));
                    } else
                        return getUnreadCountsCursor();
                }
                case VIRTUAL_TABLE_ID_UNREAD_COUNTS_BY_TYPE: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() != 3) return null;
                    return getUnreadCountsCursorByType(segments.get(2));
                }
                case TABLE_ID_DIRECT_MESSAGES_CONVERSATION: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() != 4) return null;
                    final long accountId = NumberUtils.toLong(segments.get(2), -1);
                    final long conversationId = NumberUtils.toLong(segments.get(3), -1);
                    final SQLSelectQuery query = ConversationQueryBuilder.buildByConversationId(projection,
                            accountId, conversationId, selection, sortOrder);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
                    setNotificationUri(c, DirectMessages.CONTENT_URI);
                    return c;
                }
                case TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() != 4) return null;
                    final long accountId = NumberUtils.toLong(segments.get(2), -1);
                    final String screenName = segments.get(3);
                    final SQLSelectQuery query = ConversationQueryBuilder.buildByScreenName(projection,
                            accountId, screenName, selection, sortOrder);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
                    setNotificationUri(c, DirectMessages.CONTENT_URI);
                    return c;
                }
                case VIRTUAL_TABLE_ID_CACHED_USERS_WITH_RELATIONSHIP: {
                    final long def = -1;
                    final long accountId = NumberUtils.toLong(uri.getLastPathSegment(), def);
                    final SQLSelectQuery query = CachedUsersQueryBuilder.withRelationship(projection,
                            selection, sortOrder, accountId);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
                    setNotificationUri(c, CachedUsers.CONTENT_URI);
                    return c;
                }
                case VIRTUAL_TABLE_ID_CACHED_USERS_WITH_SCORE: {
                    final long def = -1;
                    final long accountId = NumberUtils.toLong(uri.getLastPathSegment(), def);
                    final SQLSelectQuery query = CachedUsersQueryBuilder.withScore(projection,
                            selection, sortOrder, accountId, 0);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
                    setNotificationUri(c, CachedUsers.CONTENT_URI);
                    return c;
                }
                case VIRTUAL_TABLE_ID_DRAFTS_UNSENT: {
                    final AsyncTwitterWrapper twitter = mTwitterWrapper;
                    final RawItemArray sendingIds = new RawItemArray(twitter.getSendingDraftIds());
                    final Expression where;
                    if (selection != null) {
                        where = Expression.and(new Expression(selection),
                                Expression.notIn(new Column(Drafts._ID), sendingIds));
                    } else {
                        where = Expression.and(Expression.notIn(new Column(Drafts._ID), sendingIds));
                    }
                    final Cursor c = mDatabaseWrapper.query(Drafts.TABLE_NAME, projection,
                            where.getSQL(), selectionArgs, null, null, sortOrder);
                    setNotificationUri(c, Utils.getNotificationUri(tableId, uri));
                    return c;
                }
                case VIRTUAL_TABLE_ID_SUGGESTIONS_AUTO_COMPLETE: {
                    return getAutoCompleteSuggestionsCursor(uri);
                }
                case VIRTUAL_TABLE_ID_SUGGESTIONS_SEARCH: {
                    return getSearchSuggestionCursor(uri);
                }
                case VIRTUAL_TABLE_ID_EMPTY: {
                    return new MatrixCursor(projection);
                }
                case VIRTUAL_TABLE_ID_RAW_QUERY: {
                    if (projection != null || selection != null || sortOrder != null) {
                        throw new IllegalArgumentException();
                    }
                    return mDatabaseWrapper.rawQuery(uri.getLastPathSegment(), selectionArgs);
                }
            }
            if (table == null) return null;
            final Cursor c = mDatabaseWrapper.query(table, projection, selection, selectionArgs, null, null, sortOrder);
            setNotificationUri(c, Utils.getNotificationUri(tableId, uri));
            return c;
        } catch (final SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Cursor getSearchSuggestionCursor(Uri uri) {
        final String query = uri.getQueryParameter(QUERY_PARAM_QUERY);
        final long def = -1;
        final long accountId = NumberUtils.toLong(uri.getQueryParameter(QUERY_PARAM_ACCOUNT_ID), def);
        if (query == null || accountId <= 0) return null;
        final boolean emptyQuery = TextUtils.isEmpty(query);
        final String queryEscaped = query.replace("_", "^_");
        final Cursor[] cursors;
        final String[] historyProjection = {
                new Column(SearchHistory._ID, Suggestions.Search._ID).getSQL(),
                new Column("'" + Suggestions.Search.TYPE_SEARCH_HISTORY + "'", Suggestions.Search.TYPE).getSQL(),
                new Column(SearchHistory.QUERY, Suggestions.Search.TITLE).getSQL(),
                new Column(SQLConstants.NULL, Suggestions.Search.SUMMARY).getSQL(),
                new Column(SQLConstants.NULL, Suggestions.Search.ICON).getSQL(),
                new Column("0", Suggestions.Search.EXTRA_ID).getSQL(),
                new Column(SQLConstants.NULL, Suggestions.Search.EXTRA).getSQL(),
                new Column(SearchHistory.QUERY, Suggestions.Search.VALUE).getSQL(),
        };
        final Expression historySelection = Expression.likeRaw(new Column(SearchHistory.QUERY), "?||'%'", "^");
        @SuppressLint("Recycle") final Cursor historyCursor = mDatabaseWrapper.query(true,
                SearchHistory.TABLE_NAME, historyProjection, historySelection.getSQL(),
                new String[]{queryEscaped}, null, null, SearchHistory.DEFAULT_SORT_ORDER,
                TextUtils.isEmpty(query) ? "3" : "2");
        if (emptyQuery) {
            final String[] savedSearchesProjection = {
                    new Column(SavedSearches._ID, Suggestions.Search._ID).getSQL(),
                    new Column("'" + Suggestions.Search.TYPE_SAVED_SEARCH + "'", Suggestions.Search.TYPE).getSQL(),
                    new Column(SavedSearches.QUERY, Suggestions.Search.TITLE).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.SUMMARY).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.ICON).getSQL(),
                    new Column("0", Suggestions.Search.EXTRA_ID).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.EXTRA).getSQL(),
                    new Column(SavedSearches.QUERY, Suggestions.Search.VALUE).getSQL()
            };
            final Expression savedSearchesWhere = Expression.equals(SavedSearches.ACCOUNT_ID, accountId);
            @SuppressLint("Recycle") final Cursor savedSearchesCursor = mDatabaseWrapper.query(true,
                    SavedSearches.TABLE_NAME, savedSearchesProjection, savedSearchesWhere.getSQL(),
                    null, null, null, SavedSearches.DEFAULT_SORT_ORDER, null);
            cursors = new Cursor[2];
            cursors[1] = savedSearchesCursor;
        } else {
            final String[] usersProjection = {
                    new Column(CachedUsers._ID, Suggestions.Search._ID).getSQL(),
                    new Column("'" + Suggestions.Search.TYPE_USER + "'", Suggestions.Search.TYPE).getSQL(),
                    new Column(CachedUsers.NAME, Suggestions.Search.TITLE).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.Search.SUMMARY).getSQL(),
                    new Column(CachedUsers.PROFILE_IMAGE_URL, Suggestions.Search.ICON).getSQL(),
                    new Column(CachedUsers.USER_ID, Suggestions.Search.EXTRA_ID).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.EXTRA).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.Search.VALUE).getSQL(),
            };
            String queryTrimmed = queryEscaped.startsWith("@") ? queryEscaped.substring(1) : queryEscaped;
            final long[] nicknameIds = Utils.getMatchedNicknameIds(query, mUserColorNameManager);
            final Expression usersSelection = Expression.or(
                    Expression.likeRaw(new Column(CachedUsers.SCREEN_NAME), "?||'%'", "^"),
                    Expression.likeRaw(new Column(CachedUsers.NAME), "?||'%'", "^"),
                    Expression.in(new Column(CachedUsers.USER_ID), new RawItemArray(nicknameIds)));
            final String[] selectionArgs = new String[]{queryTrimmed, queryTrimmed};
            final String[] order = {CachedUsers.LAST_SEEN, CachedUsers.SCORE, CachedUsers.SCREEN_NAME,
                    CachedUsers.NAME};
            final boolean[] ascending = {false, false, true, true};
            final OrderBy orderBy = new OrderBy(order, ascending);

            final SQLSelectQuery usersQuery = CachedUsersQueryBuilder.withScore(usersProjection,
                    usersSelection.getSQL(), orderBy.getSQL(), accountId, 0);
            @SuppressLint("Recycle") final Cursor usersCursor = mDatabaseWrapper.rawQuery(usersQuery.getSQL(), selectionArgs);
            final Expression exactUserSelection = Expression.or(Expression.likeRaw(new Column(CachedUsers.SCREEN_NAME), "?", "^"));
            final Cursor exactUserCursor = mDatabaseWrapper.query(CachedUsers.TABLE_NAME,
                    new String[]{SQLFunctions.COUNT()}, exactUserSelection.getSQL(),
                    new String[]{queryTrimmed}, null, null, null, "1");
            final boolean hasName = exactUserCursor.moveToPosition(0) && exactUserCursor.getInt(0) > 0;
            exactUserCursor.close();
            final MatrixCursor screenNameCursor = new MatrixCursor(Suggestions.Search.COLUMNS);
            if (!hasName) {
                final Matcher m = PATTERN_SCREEN_NAME.matcher(query);
                if (m.matches()) {
                    final String screenName = m.group(1);
                    screenNameCursor.addRow(new Object[]{0, Suggestions.Search.TYPE_SCREEN_NAME,
                            screenName, null, null, 0, null, screenName});
                }
            }
            cursors = new Cursor[3];
            cursors[1] = screenNameCursor;
            cursors[2] = usersCursor;
        }
        cursors[0] = historyCursor;
        return new MergeCursor(cursors);
    }

    private Cursor getAutoCompleteSuggestionsCursor(@NonNull Uri uri) {
        final String query = uri.getQueryParameter(QUERY_PARAM_QUERY);
        final String type = uri.getQueryParameter(QUERY_PARAM_TYPE);
        final String accountId = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_ID);
        if (query == null || type == null) return null;
        final String queryEscaped = query.replace("_", "^_");
        if (Suggestions.AutoComplete.TYPE_USERS.equals(type)) {
            final long[] nicknameIds = Utils.getMatchedNicknameIds(query, mUserColorNameManager);
            final Expression where = Expression.or(Expression.likeRaw(new Column(CachedUsers.SCREEN_NAME), "?||'%'", "^"),
                    Expression.likeRaw(new Column(CachedUsers.NAME), "?||'%'", "^"),
                    Expression.in(new Column(CachedUsers.USER_ID), new RawItemArray(nicknameIds)));
            final String[] whereArgs = {queryEscaped, queryEscaped};
            final String[] mappedProjection = {
                    new Column(CachedUsers._ID, Suggestions._ID).getSQL(),
                    new Column("'" + Suggestions.AutoComplete.TYPE_USERS + "'", Suggestions.TYPE).getSQL(),
                    new Column(CachedUsers.NAME, Suggestions.TITLE).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.SUMMARY).getSQL(),
                    new Column(CachedUsers.USER_ID, Suggestions.EXTRA_ID).getSQL(),
                    new Column(CachedUsers.PROFILE_IMAGE_URL, Suggestions.ICON).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.VALUE).getSQL(),
            };
            final String[] orderBy = {CachedUsers.SCORE, CachedUsers.LAST_SEEN, CachedUsers.SCREEN_NAME,
                    CachedUsers.NAME};
            final boolean[] ascending = {false, false, true, true};
            return query(Uri.withAppendedPath(CachedUsers.CONTENT_URI_WITH_SCORE, accountId),
                    mappedProjection, where.getSQL(), whereArgs, new OrderBy(orderBy, ascending).getSQL());
        } else if (Suggestions.AutoComplete.TYPE_HASHTAGS.equals(type)) {
            final Expression where = Expression.likeRaw(new Column(CachedHashtags.NAME), "?||'%'", "^");
            final String[] whereArgs = new String[]{queryEscaped};
            final String[] mappedProjection = {
                    new Column(CachedHashtags._ID, Suggestions._ID).getSQL(),
                    new Column("'" + Suggestions.AutoComplete.TYPE_HASHTAGS + "'", Suggestions.TYPE).getSQL(),
                    new Column(CachedHashtags.NAME, Suggestions.TITLE).getSQL(),
                    new Column("NULL", Suggestions.SUMMARY).getSQL(),
                    new Column("0", Suggestions.EXTRA_ID).getSQL(),
                    new Column("NULL", Suggestions.ICON).getSQL(),
                    new Column(CachedHashtags.NAME, Suggestions.VALUE).getSQL(),
            };
            return query(CachedHashtags.CONTENT_URI, mappedProjection, where.getSQL(),
                    whereArgs, null);
        }
        return null;
    }

    @Override
    public int update(@NonNull final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        try {
            return updateInternal(uri, values, selection, selectionArgs);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return updateInternal(uri, values, selection, selectionArgs);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private int updateInternal(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        checkWritePermission(tableId, table);
        int result = 0;
        if (table != null) {
            switch (tableId) {
                case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
                case TABLE_ID_DIRECT_MESSAGES:
                case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
                    return 0;
            }
            result = mDatabaseWrapper.update(table, values, selection, selectionArgs);
        }
        if (result > 0) {
            onDatabaseUpdated(tableId, uri);
        }
        return result;
    }

    private boolean checkPermission(final String... permissions) {
        return mPermissionsManager.checkCallingPermission(permissions);
    }

    private void checkReadPermission(final int id, final String table, final String[] projection) {
        if (Binder.getCallingPid() == Process.myPid()) return;
        switch (id) {
            case VIRTUAL_TABLE_ID_PERMISSIONS: {
                return;
            }
            case VIRTUAL_TABLE_ID_PREFERENCES:
            case VIRTUAL_TABLE_ID_DNS: {
                if (!checkPermission(PERMISSION_PREFERENCES))
                    throw new SecurityException("Access preferences requires level PERMISSION_LEVEL_PREFERENCES");
                break;
            }
            case TABLE_ID_ACCOUNTS: {
                // Reading some information like user_id, screen_name etc is
                // okay, but reading columns like password requires higher
                // permission level.
                if (checkPermission(PERMISSION_ACCOUNTS)) {
                    break;
                }
                // Only querying basic information
                if (TwidereArrayUtils.contains(Accounts.COLUMNS_NO_CREDENTIALS, projection) && !checkPermission(PERMISSION_READ)) {
                    final String pkgName = mPermissionsManager.getPackageNameByUid(Binder.getCallingUid());
                    throw new SecurityException("Access database " + table + " requires level PERMISSION_LEVEL_READ, package: " + pkgName);
                }
                final String pkgName = mPermissionsManager.getPackageNameByUid(Binder.getCallingUid());
                final List<String> callingSensitiveCols = new ArrayList<>();
                if (projection != null) {
                    Collections.addAll(callingSensitiveCols, projection);
                    callingSensitiveCols.removeAll(Arrays.asList(Accounts.COLUMNS_NO_CREDENTIALS));
                } else {
                    callingSensitiveCols.add("*");
                }
                throw new SecurityException("Access column " + TwidereListUtils.toString(callingSensitiveCols, ',', true)
                        + " in database accounts requires level PERMISSION_LEVEL_ACCOUNTS, package: " + pkgName);
            }
            case TABLE_ID_DIRECT_MESSAGES:
            case TABLE_ID_DIRECT_MESSAGES_INBOX:
            case TABLE_ID_DIRECT_MESSAGES_OUTBOX:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES: {
                if (!checkPermission(PERMISSION_DIRECT_MESSAGES))
                    throw new SecurityException("Access database " + table
                            + " requires level PERMISSION_LEVEL_DIRECT_MESSAGES");
                break;
            }
            case TABLE_ID_STATUSES:
            case TABLE_ID_MENTIONS:
            case TABLE_ID_TABS:
            case TABLE_ID_DRAFTS:
            case TABLE_ID_CACHED_USERS:
            case TABLE_ID_FILTERED_USERS:
            case TABLE_ID_FILTERED_KEYWORDS:
            case TABLE_ID_FILTERED_SOURCES:
            case TABLE_ID_FILTERED_LINKS:
            case TABLE_ID_TRENDS_LOCAL:
            case TABLE_ID_CACHED_STATUSES:
            case TABLE_ID_CACHED_HASHTAGS: {
                if (!checkPermission(PERMISSION_READ))
                    throw new SecurityException("Access database " + table + " requires level PERMISSION_LEVEL_READ");
                break;
            }
            default: {
                if (!mPermissionsManager.checkSignature(Binder.getCallingUid())) {
                    throw new SecurityException("Internal database " + id + " is not allowed for third-party applications");
                }
            }
        }
    }

    private void checkWritePermission(final int id, final String table) {
        if (Binder.getCallingPid() == Process.myPid()) return;
        switch (id) {
            case TABLE_ID_ACCOUNTS: {
                // Writing to accounts database is not allowed for third-party
                // applications.
                if (!mPermissionsManager.checkSignature(Binder.getCallingUid()))
                    throw new SecurityException(
                            "Writing to accounts database is not allowed for third-party applications");
                break;
            }
            case TABLE_ID_DIRECT_MESSAGES:
            case TABLE_ID_DIRECT_MESSAGES_INBOX:
            case TABLE_ID_DIRECT_MESSAGES_OUTBOX:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES: {
                if (!checkPermission(PERMISSION_DIRECT_MESSAGES))
                    throw new SecurityException("Access database " + table
                            + " requires level PERMISSION_LEVEL_DIRECT_MESSAGES");
                break;
            }
            case TABLE_ID_STATUSES:
            case TABLE_ID_MENTIONS:
            case TABLE_ID_TABS:
            case TABLE_ID_DRAFTS:
            case TABLE_ID_CACHED_USERS:
            case TABLE_ID_FILTERED_USERS:
            case TABLE_ID_FILTERED_KEYWORDS:
            case TABLE_ID_FILTERED_SOURCES:
            case TABLE_ID_FILTERED_LINKS:
            case TABLE_ID_TRENDS_LOCAL:
            case TABLE_ID_CACHED_STATUSES:
            case TABLE_ID_CACHED_HASHTAGS: {
                if (!checkPermission(PERMISSION_WRITE))
                    throw new SecurityException("Access database " + table + " requires level PERMISSION_LEVEL_WRITE");
                break;
            }
            default: {
                if (!mPermissionsManager.checkSignature(Binder.getCallingUid())) {
                    throw new SecurityException("Internal database is not allowed for third-party applications");
                }
            }
        }
    }

    private void clearNotification() {
        mNotificationManager.cancelAll();
    }

    private void clearNotification(final int notificationType, final long accountId) {
        mNotificationManager.cancelById(Utils.getNotificationId(notificationType, accountId));
    }

    private Cursor getCachedImageCursor(final String url) {
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, String.format("getCachedImageCursor(%s)", url));
        }
        final MatrixCursor c = new MatrixCursor(CachedImages.MATRIX_COLUMNS);
        final File file = mImagePreloader.getCachedImageFile(url);
        if (url != null && file != null) {
            c.addRow(new String[]{url, file.getPath()});
        }
        return c;
    }

    private ParcelFileDescriptor getCachedImageFd(final String url) throws FileNotFoundException {
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, String.format("getCachedImageFd(%s)", url));
        }
        final File file = mImagePreloader.getCachedImageFile(url);
        if (file == null) return null;
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private ParcelFileDescriptor getCacheFileFd(final String name) throws FileNotFoundException {
        if (name == null) return null;
        final Context context = getContext();
        assert context != null;
        final File cacheDir = context.getCacheDir();
        final File file = new File(cacheDir, name);
        if (!file.exists()) return null;
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private ContentResolver getContentResolver() {
        if (mContentResolver != null) return mContentResolver;
        final Context context = getContext();
        assert context != null;
        return mContentResolver = context.getContentResolver();
    }

    private Cursor getDNSCursor(final String host) {
        final MatrixCursor c = new MatrixCursor(DNS.MATRIX_COLUMNS);
        try {
            final List<InetAddress> addresses = mDns.lookup(host);
            for (InetAddress address : addresses) {
                c.addRow(new String[]{host, address.getHostAddress()});
            }
        } catch (final IOException ignore) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, ignore);
            }
        }
        return c;
    }

    private Cursor getNotificationsCursor() {
        final MatrixCursor c = new MatrixCursor(Notifications.MATRIX_COLUMNS);
        return c;
    }

    private Cursor getNotificationsCursor(final int id) {
        final MatrixCursor c = new MatrixCursor(Notifications.MATRIX_COLUMNS);
        return c;
    }

    private Bitmap getProfileImageForNotification(final String profileImageUrl) {
        final Context context = getContext();
        assert context != null;
        final Resources res = context.getResources();
        final int w = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        final int h = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        final File profile_image_file = mImagePreloader.getCachedImageFile(profileImageUrl);
        final Bitmap profile_image = profile_image_file != null && profile_image_file.isFile() ? BitmapFactory
                .decodeFile(profile_image_file.getPath()) : null;
        if (profile_image != null) return Bitmap.createScaledBitmap(profile_image, w, h, true);
        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher), w, h, true);
    }

    private Cursor getUnreadCountsCursor() {
        final MatrixCursor c = new MatrixCursor(UnreadCounts.MATRIX_COLUMNS);
        return c;
    }

    private Cursor getUnreadCountsCursor(final int position) {
        final MatrixCursor c = new MatrixCursor(UnreadCounts.MATRIX_COLUMNS);

        return c;
    }

    private Cursor getUnreadCountsCursorByType(final String type) {
        final MatrixCursor c = new MatrixCursor(UnreadCounts.MATRIX_COLUMNS);
        return c;
    }

    private boolean isNotificationAudible() {
        return !mActivityTracker.isHomeActivityStarted();
    }

    private void notifyContentObserver(final Uri uri) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final ContentResolver cr = getContentResolver();
                if (uri == null || cr == null) return;
                cr.notifyChange(uri, null);
            }
        });
    }

    private void notifyUnreadCountChanged(final int position) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBus.post(new UnreadCountUpdatedEvent(position));
            }
        });
        notifyContentObserver(UnreadCounts.CONTENT_URI);
    }

    private void onDatabaseUpdated(final int tableId, final Uri uri) {
        if (uri == null) return;
        switch (tableId) {
            case TABLE_ID_ACCOUNTS: {
                DataStoreUtils.clearAccountColor();
                DataStoreUtils.clearAccountName();
                break;
            }
        }
        notifyContentObserver(Utils.getNotificationUri(tableId, uri));
    }

    private void onNewItemsInserted(final Uri uri, final int tableId, final ContentValues values) {
        onNewItemsInserted(uri, tableId, new ContentValues[]{values});
    }

    private void onNewItemsInserted(final Uri uri, final int tableId, final ContentValues[] valuesArray) {
        final Context context = getContext();
        if (uri == null || valuesArray == null || valuesArray.length == 0 || context == null)
            return;
        preloadImages(valuesArray);
        if (!uri.getBooleanQueryParameter(QUERY_PARAM_NOTIFY, true)) return;
        switch (tableId) {
            case TABLE_ID_STATUSES: {
                mBackgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                                DataStoreUtils.getAccountIds(context));
                        for (final AccountPreferences pref : prefs) {
                            if (!pref.isHomeTimelineNotificationEnabled()) continue;
                            final long positionTag = getPositionTag(CustomTabType.HOME_TIMELINE, pref.getAccountId());
                            showTimelineNotification(pref, positionTag);
                        }
                        notifyUnreadCountChanged(NOTIFICATION_ID_HOME_TIMELINE);
                    }
                });
                break;
            }
            case TABLE_ID_ACTIVITIES_ABOUT_ME: {
                mBackgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                                DataStoreUtils.getAccountIds(context));
                        final boolean combined = mPreferences.getBoolean(KEY_COMBINED_NOTIFICATIONS);
                        for (final AccountPreferences pref : prefs) {
                            if (!pref.isInteractionsNotificationEnabled()) continue;
                            showInteractionsNotification(pref, getPositionTag(ReadPositionTag.ACTIVITIES_ABOUT_ME,
                                    pref.getAccountId()), combined);
                        }
                        notifyUnreadCountChanged(NOTIFICATION_ID_INTERACTIONS_TIMELINE);
                    }
                });
                break;
            }
            case TABLE_ID_DIRECT_MESSAGES_INBOX: {
                final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                        DataStoreUtils.getAccountIds(context));
                for (final AccountPreferences pref : prefs) {
                    if (!pref.isDirectMessagesNotificationEnabled()) continue;
                    final StringLongPair[] pairs = mReadStateManager.getPositionPairs(CustomTabType.DIRECT_MESSAGES);
                    showMessagesNotification(pref, pairs, valuesArray);
                }
                notifyUnreadCountChanged(NOTIFICATION_ID_DIRECT_MESSAGES);
                break;
            }
            case TABLE_ID_DRAFTS: {
                break;
            }
        }
    }

    private long getPositionTag(String tag, long accountId) {
        final long position = mReadStateManager.getPosition(Utils.getReadPositionTagWithAccounts(tag,
                accountId));
        if (position != -1) return position;
        return mReadStateManager.getPosition(tag);
    }

    private void showTimelineNotification(AccountPreferences pref, long position) {
        final long accountId = pref.getAccountId();
        final Context context = getContext();
        if (context == null) return;
        final Resources resources = context.getResources();
        final NotificationManagerWrapper nm = mNotificationManager;
        final Expression selection = Expression.and(Expression.equals(Statuses.ACCOUNT_ID, accountId),
                Expression.greaterThan(Statuses.STATUS_ID, position));
        final String filteredSelection = DataStoreUtils.buildStatusFilterWhereClause(Statuses.TABLE_NAME,
                selection).getSQL();
        final String[] userProjection = {Statuses.USER_ID, Statuses.USER_NAME, Statuses.USER_SCREEN_NAME};
        final String[] statusProjection = {Statuses.STATUS_ID};
        final Cursor statusCursor = mDatabaseWrapper.query(Statuses.TABLE_NAME, statusProjection,
                filteredSelection, null, null, null, Statuses.DEFAULT_SORT_ORDER);
        final Cursor userCursor = mDatabaseWrapper.query(Statuses.TABLE_NAME, userProjection,
                filteredSelection, null, Statuses.USER_ID, null, Statuses.DEFAULT_SORT_ORDER);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final int usersCount = userCursor.getCount();
            final int statusesCount = statusCursor.getCount();
            if (statusesCount == 0 || usersCount == 0) return;
            final int idxStatusId = statusCursor.getColumnIndex(Statuses.STATUS_ID),
                    idxUserName = userCursor.getColumnIndex(Statuses.USER_NAME),
                    idxUserScreenName = userCursor.getColumnIndex(Statuses.USER_NAME),
                    idxUserId = userCursor.getColumnIndex(Statuses.USER_NAME);
            final long statusId = statusCursor.moveToFirst() ? statusCursor.getLong(idxStatusId) : -1;
            final String notificationTitle = resources.getQuantityString(R.plurals.N_new_statuses,
                    statusesCount, statusesCount);
            final String notificationContent;
            userCursor.moveToFirst();
            final String displayName = mUserColorNameManager.getUserNickname(userCursor.getLong(idxUserId),
                    mNameFirst ? userCursor.getString(idxUserName) : userCursor.getString(idxUserScreenName));
            if (usersCount == 1) {
                notificationContent = context.getString(R.string.from_name, displayName);
            } else if (usersCount == 2) {
                userCursor.moveToPosition(1);
                final String othersName = mUserColorNameManager.getUserNickname(userCursor.getLong(idxUserId),
                        mNameFirst ? userCursor.getString(idxUserName) : userCursor.getString(idxUserScreenName));
                notificationContent = resources.getString(R.string.from_name_and_name, displayName, othersName);
            } else {
                notificationContent = resources.getString(R.string.from_name_and_N_others, displayName, usersCount - 1);
            }

            // Setup notification
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.ic_stat_twitter);
            builder.setTicker(notificationTitle);
            builder.setContentTitle(notificationTitle);
            builder.setContentText(notificationContent);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            builder.setContentIntent(getContentIntent(context, CustomTabType.HOME_TIMELINE,
                    NotificationType.HOME_TIMELINE, accountId));
            builder.setDeleteIntent(getMarkReadDeleteIntent(context, NotificationType.HOME_TIMELINE,
                    accountId, statusId, false));
            builder.setNumber(statusesCount);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            applyNotificationPreferences(builder, pref, pref.getHomeTimelineNotificationType());
            try {
                nm.notify("home_" + accountId, Utils.getNotificationId(NOTIFICATION_ID_HOME_TIMELINE, accountId), builder.build());
                Utils.sendPebbleNotification(context, notificationContent);
            } catch (SecurityException e) {
                // Silently ignore
            }
        } finally {
            statusCursor.close();
            userCursor.close();
        }
    }

    private void showInteractionsNotification(AccountPreferences pref, long position, boolean combined) {
        final Context context = getContext();
        if (context == null) return;
        final long accountId = pref.getAccountId();
        final String where = Expression.and(
                Expression.equals(Activities.ACCOUNT_ID, pref.getAccountId()),
                Expression.greaterThan(Activities.TIMESTAMP, position)
        ).getSQL();
        Cursor c = query(Activities.AboutMe.CONTENT_URI, Activities.COLUMNS, where, null,
                new OrderBy(Activities.TIMESTAMP, false).getSQL());
        if (c == null) return;
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        try {
            final int count = c.getCount();
            if (count == 0) return;
            builder.setSmallIcon(R.drawable.ic_stat_notification);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            applyNotificationPreferences(builder, pref, pref.getMentionsNotificationType());

            final Resources resources = context.getResources();
            final String accountName = DataStoreUtils.getAccountDisplayName(context, accountId, mNameFirst);
            builder.setContentText(accountName);
            final InboxStyle style = new InboxStyle();
            builder.setStyle(style);
            builder.setAutoCancel(true);
            style.setSummaryText(accountName);
            final ParcelableActivityCursorIndices ci = new ParcelableActivityCursorIndices(c);
            int messageLines = 0;

            long timestamp = -1;
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                if (messageLines == 5) {
                    style.addLine(resources.getString(R.string.and_N_more, count - c.getPosition()));
                    break;
                }
                final ParcelableActivity activity = ci.newObject(c);
                if (pref.isNotificationMentionsOnly() && ArrayUtils.contains(Activity.Action.MENTION_ACTIONS,
                        activity.action)) {
                    continue;
                }
                final long[] filteredUserIds = DataStoreUtils.getFilteredUserIds(context);
                if (timestamp == -1) {
                    timestamp = activity.timestamp;
                }
                ParcelableActivityUtils.initAfterFilteredSourceIds(activity, filteredUserIds,
                        pref.isNotificationFollowingOnly());
                final ParcelableUser[] sources = ParcelableActivityUtils.getAfterFilteredSources(activity);
                if (ArrayUtils.isEmpty(sources)) continue;
                final ActivityTitleSummaryMessage message = ActivityTitleSummaryMessage.get(context,
                        mUserColorNameManager, activity, sources,
                        0, false, mUseStarForLikes, mNameFirst);
                if (message != null) {
                    final CharSequence summary = message.getSummary();
                    if (TextUtils.isEmpty(summary)) {
                        style.addLine(message.getTitle());
                    } else {
                        style.addLine(SpanFormatter.format(resources.getString(R.string.title_summary_line_format),
                                message.getTitle(), summary));
                    }
                    messageLines++;
                }
            }
            if (messageLines == 0) return;
            final int displayCount = messageLines + count - c.getPosition();
            final String title = resources.getQuantityString(R.plurals.N_new_interactions,
                    displayCount, displayCount);
            builder.setContentTitle(title);
            style.setBigContentTitle(title);
            builder.setNumber(displayCount);
            builder.setContentIntent(getContentIntent(context, CustomTabType.NOTIFICATIONS_TIMELINE,
                    NotificationType.INTERACTIONS, accountId));
            if (timestamp != -1) {
                builder.setDeleteIntent(getMarkReadDeleteIntent(context,
                        NotificationType.INTERACTIONS, accountId, timestamp, false));
            }
        } finally {
            c.close();
        }
        final int notificationId = Utils.getNotificationId(NOTIFICATION_ID_INTERACTIONS_TIMELINE, accountId);
        mNotificationManager.notify("interactions", notificationId, builder.build());
    }

    private PendingIntent getContentIntent(final Context context, @CustomTabType final String type,
                                           @NotificationType final String notificationType,
                                           final long accountId) {
        // Setup click intent
        final Intent homeIntent = new Intent(context, HomeActivity.class);
        final Uri.Builder homeLinkBuilder = new Uri.Builder();
        homeLinkBuilder.scheme(SCHEME_TWIDERE);
        homeLinkBuilder.authority(type);
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_FROM_NOTIFICATION, String.valueOf(true));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, type);
        homeIntent.setData(homeLinkBuilder.build());
        return PendingIntent.getActivity(context, 0, homeIntent, 0);
    }

    private PendingIntent getStatusContentIntent(final Context context, @CustomTabType final String type,
                                                 @NotificationType final String notificationType,
                                                 long accountId, long statusId,
                                                 long userId, boolean userFollowing) {
        // Setup click intent
        final Intent homeIntent = new Intent(Intent.ACTION_VIEW);
        homeIntent.setPackage(BuildConfig.APPLICATION_ID);
        final Uri.Builder homeLinkBuilder = new Uri.Builder();
        homeLinkBuilder.scheme(SCHEME_TWIDERE);
        homeLinkBuilder.authority(AUTHORITY_STATUS);
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_STATUS_ID, String.valueOf(statusId));
        UriExtraUtils.addExtra(homeLinkBuilder, "item_id", statusId);
        UriExtraUtils.addExtra(homeLinkBuilder, "item_user_id", userId);
        UriExtraUtils.addExtra(homeLinkBuilder, "item_user_following", userFollowing);
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_FROM_NOTIFICATION, String.valueOf(true));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, type);
        homeIntent.setData(homeLinkBuilder.build());
        return PendingIntent.getActivity(context, 0, homeIntent, 0);
    }

    private void applyNotificationPreferences(NotificationCompat.Builder builder, AccountPreferences pref, int defaultFlags) {
        int notificationDefaults = 0;
        if (AccountPreferences.isNotificationHasLight(defaultFlags)) {
            notificationDefaults |= NotificationCompat.DEFAULT_LIGHTS;
        }
        if (isNotificationAudible()) {
            if (AccountPreferences.isNotificationHasVibration(defaultFlags)) {
                notificationDefaults |= NotificationCompat.DEFAULT_VIBRATE;
            } else {
                notificationDefaults &= ~NotificationCompat.DEFAULT_VIBRATE;
            }
            if (AccountPreferences.isNotificationHasRingtone(defaultFlags)) {
                builder.setSound(pref.getNotificationRingtone(), AudioManager.STREAM_NOTIFICATION);
            }
        } else {
            notificationDefaults &= ~(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND);
        }
        builder.setColor(pref.getNotificationLightColor());
        builder.setDefaults(notificationDefaults);
    }

    private void showMessagesNotification(AccountPreferences pref, StringLongPair[] pairs, ContentValues[] valuesArray) {
        final Context context = getContext();
        assert context != null;
        final long accountId = pref.getAccountId();
        final long prevOldestId = mReadStateManager.getPosition(TAG_OLDEST_MESSAGES, String.valueOf(accountId));
        long oldestId = -1;
        for (final ContentValues contentValues : valuesArray) {
            final long messageId = contentValues.getAsLong(DirectMessages.MESSAGE_ID);
            oldestId = oldestId < 0 ? messageId : Math.min(oldestId, messageId);
            if (messageId <= prevOldestId) return;
        }
        mReadStateManager.setPosition(TAG_OLDEST_MESSAGES, String.valueOf(accountId), oldestId, false);
        final Resources resources = context.getResources();
        final NotificationManagerWrapper nm = mNotificationManager;
        final ArrayList<Expression> orExpressions = new ArrayList<>();
        final String prefix = accountId + "-";
        final int prefixLength = prefix.length();
        final Set<Long> senderIds = new CompactHashSet<>();
        for (StringLongPair pair : pairs) {
            final String key = pair.getKey();
            if (key.startsWith(prefix)) {
                final long senderId = Long.parseLong(key.substring(prefixLength));
                senderIds.add(senderId);
                final Expression expression = Expression.and(
                        Expression.equals(DirectMessages.SENDER_ID, senderId),
                        Expression.greaterThan(DirectMessages.MESSAGE_ID, pair.getValue())
                );
                orExpressions.add(expression);
            }
        }
        orExpressions.add(Expression.notIn(new Column(DirectMessages.SENDER_ID), new RawItemArray(senderIds.toArray())));
        final Expression selection = Expression.and(
                Expression.equals(DirectMessages.ACCOUNT_ID, accountId),
                Expression.greaterThan(DirectMessages.MESSAGE_ID, prevOldestId),
                Expression.or(orExpressions.toArray(new Expression[orExpressions.size()]))
        );
        final String filteredSelection = selection.getSQL();
        final String[] userProjection = {DirectMessages.SENDER_ID, DirectMessages.SENDER_NAME,
                DirectMessages.SENDER_SCREEN_NAME};
        final String[] messageProjection = {DirectMessages.MESSAGE_ID, DirectMessages.SENDER_ID,
                DirectMessages.SENDER_NAME, DirectMessages.SENDER_SCREEN_NAME, DirectMessages.TEXT_UNESCAPED,
                DirectMessages.MESSAGE_TIMESTAMP};
        final Cursor messageCursor = mDatabaseWrapper.query(DirectMessages.Inbox.TABLE_NAME, messageProjection,
                filteredSelection, null, null, null, DirectMessages.DEFAULT_SORT_ORDER);
        final Cursor userCursor = mDatabaseWrapper.query(DirectMessages.Inbox.TABLE_NAME, userProjection,
                filteredSelection, null, DirectMessages.SENDER_ID, null, DirectMessages.DEFAULT_SORT_ORDER);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final int usersCount = userCursor.getCount();
            final int messagesCount = messageCursor.getCount();
            if (messagesCount == 0 || usersCount == 0) return;
            final String accountName = DataStoreUtils.getAccountName(context, accountId);
            final String accountScreenName = DataStoreUtils.getAccountScreenName(context, accountId);
            final int idxMessageText = messageCursor.getColumnIndex(DirectMessages.TEXT_UNESCAPED),
                    idxMessageTimestamp = messageCursor.getColumnIndex(DirectMessages.MESSAGE_TIMESTAMP),
                    idxMessageId = messageCursor.getColumnIndex(DirectMessages.MESSAGE_ID),
                    idxMessageUserId = messageCursor.getColumnIndex(DirectMessages.SENDER_ID),
                    idxMessageUserName = messageCursor.getColumnIndex(DirectMessages.SENDER_NAME),
                    idxMessageUserScreenName = messageCursor.getColumnIndex(DirectMessages.SENDER_SCREEN_NAME),
                    idxUserName = userCursor.getColumnIndex(DirectMessages.SENDER_NAME),
                    idxUserScreenName = userCursor.getColumnIndex(DirectMessages.SENDER_NAME),
                    idxUserId = userCursor.getColumnIndex(DirectMessages.SENDER_NAME);

            final CharSequence notificationTitle = resources.getQuantityString(R.plurals.N_new_messages,
                    messagesCount, messagesCount);
            final String notificationContent;
            userCursor.moveToFirst();
            final String displayName = mUserColorNameManager.getUserNickname(userCursor.getLong(idxUserId),
                    mNameFirst ? userCursor.getString(idxUserName) : userCursor.getString(idxUserScreenName));
            if (usersCount == 1) {
                if (messagesCount == 1) {
                    notificationContent = context.getString(R.string.notification_direct_message, displayName);
                } else {
                    notificationContent = context.getString(R.string.notification_direct_message_multiple_messages,
                            displayName, messagesCount);
                }
            } else {
                notificationContent = context.getString(R.string.notification_direct_message_multiple_users,
                        displayName, usersCount - 1, messagesCount);
            }

            final LongSparseArray<Long> idsMap = new LongSparseArray<>();
            // Add rich notification and get latest tweet timestamp
            long when = -1;
            final InboxStyle style = new InboxStyle();
            for (int i = 0; messageCursor.moveToPosition(i) && i < messagesCount; i++) {
                if (when < 0) {
                    when = messageCursor.getLong(idxMessageTimestamp);
                }
                if (i < 5) {
                    final SpannableStringBuilder sb = new SpannableStringBuilder();
                    sb.append(mUserColorNameManager.getUserNickname(messageCursor.getLong(idxUserId),
                            mNameFirst ? messageCursor.getString(idxMessageUserName) : messageCursor.getString(idxMessageUserScreenName)));
                    sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(' ');
                    sb.append(messageCursor.getString(idxMessageText));
                    style.addLine(sb);
                }
                final long userId = messageCursor.getLong(idxMessageUserId);
                final long messageId = messageCursor.getLong(idxMessageId);
                idsMap.put(userId, Math.max(idsMap.get(userId, -1L), messageId));
            }
            if (mNameFirst) {
                style.setSummaryText(accountName);
            } else {
                style.setSummaryText("@" + accountScreenName);
            }
            final StringLongPair[] positions = new StringLongPair[idsMap.size()];
            for (int i = 0, j = idsMap.size(); i < j; i++) {
                positions[i] = new StringLongPair(String.valueOf(idsMap.keyAt(i)), idsMap.valueAt(i));
            }

            // Setup notification
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.ic_stat_message);
            builder.setTicker(notificationTitle);
            builder.setContentTitle(notificationTitle);
            builder.setContentText(notificationContent);
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
            builder.setContentIntent(getContentIntent(context, CustomTabType.DIRECT_MESSAGES,
                    NotificationType.DIRECT_MESSAGES, accountId));
            builder.setDeleteIntent(getMarkReadDeleteIntent(context,
                    NotificationType.DIRECT_MESSAGES, accountId, positions));
            builder.setNumber(messagesCount);
            builder.setWhen(when);
            builder.setStyle(style);
            builder.setColor(pref.getNotificationLightColor());
            applyNotificationPreferences(builder, pref, pref.getDirectMessagesNotificationType());
            try {
                nm.notify("messages_" + accountId, NOTIFICATION_ID_DIRECT_MESSAGES, builder.build());
                Utils.sendPebbleNotification(context, notificationContent);
            } catch (SecurityException e) {
                // Silently ignore
            }
        } finally {
            messageCursor.close();
            userCursor.close();
        }
    }

    private void preloadImages(final ContentValues... values) {
        if (values == null) return;
        for (final ContentValues v : values) {
            if (mPreferences.getBoolean(KEY_PRELOAD_PROFILE_IMAGES, false)) {
                mImagePreloader.preloadImage(v.getAsString(Statuses.USER_PROFILE_IMAGE_URL));
                mImagePreloader.preloadImage(v.getAsString(DirectMessages.SENDER_PROFILE_IMAGE_URL));
                mImagePreloader.preloadImage(v.getAsString(DirectMessages.RECIPIENT_PROFILE_IMAGE_URL));
            }
            if (mPreferences.getBoolean(KEY_PRELOAD_PREVIEW_IMAGES, false)) {
                final String textHtml = v.getAsString(Statuses.TEXT_HTML);
                for (final String link : PreviewMediaExtractor.getSupportedLinksInStatus(textHtml)) {
                    mImagePreloader.preloadImage(link);
                }
            }
        }
    }

    private void setNotificationUri(final Cursor c, final Uri uri) {
        final ContentResolver cr = getContentResolver();
        if (cr == null || c == null || uri == null) return;
        c.setNotificationUri(cr, uri);
    }

    private void updatePreferences() {
        mNameFirst = mPreferences.getBoolean(KEY_NAME_FIRST);
        mUseStarForLikes = mPreferences.getBoolean(KEY_I_WANT_MY_STARS_BACK);
    }

}
