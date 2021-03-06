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

package org.mariotaku.twidere.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.net.ConnectivityManagerCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.system.ErrnoException;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONException;
import org.mariotaku.sqliteqb.library.AllColumns;
import org.mariotaku.sqliteqb.library.Columns;
import org.mariotaku.sqliteqb.library.Columns.Column;
import org.mariotaku.sqliteqb.library.Expression;
import org.mariotaku.sqliteqb.library.Selectable;
import org.mariotaku.twidere.BuildConfig;
import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.activity.CopyLinkActivity;
import org.mariotaku.twidere.activity.support.AccountSelectorActivity;
import org.mariotaku.twidere.activity.support.ColorPickerDialogActivity;
import org.mariotaku.twidere.adapter.iface.IBaseAdapter;
import org.mariotaku.twidere.adapter.iface.IBaseCardAdapter;
import org.mariotaku.twidere.annotation.CustomTabType;
import org.mariotaku.twidere.annotation.ReadPositionTag;
import org.mariotaku.twidere.api.twitter.Twitter;
import org.mariotaku.twidere.api.twitter.TwitterException;
import org.mariotaku.twidere.api.twitter.model.DirectMessage;
import org.mariotaku.twidere.api.twitter.model.GeoLocation;
import org.mariotaku.twidere.api.twitter.model.RateLimitStatus;
import org.mariotaku.twidere.api.twitter.model.Relationship;
import org.mariotaku.twidere.api.twitter.model.Status;
import org.mariotaku.twidere.api.twitter.model.UserMentionEntity;
import org.mariotaku.twidere.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback;
import org.mariotaku.twidere.fragment.support.AbsStatusesFragment.DefaultOnLikedListener;
import org.mariotaku.twidere.fragment.support.AccountsManagerFragment;
import org.mariotaku.twidere.fragment.support.AddStatusFilterDialogFragment;
import org.mariotaku.twidere.fragment.support.DestroyStatusDialogFragment;
import org.mariotaku.twidere.fragment.support.DraftsFragment;
import org.mariotaku.twidere.fragment.support.FiltersFragment;
import org.mariotaku.twidere.fragment.support.IncomingFriendshipsFragment;
import org.mariotaku.twidere.fragment.support.ListsFragment;
import org.mariotaku.twidere.fragment.support.MessagesConversationFragment;
import org.mariotaku.twidere.fragment.support.MutesUsersListFragment;
import org.mariotaku.twidere.fragment.support.SavedSearchesListFragment;
import org.mariotaku.twidere.fragment.support.ScheduledStatusesFragment;
import org.mariotaku.twidere.fragment.support.SearchFragment;
import org.mariotaku.twidere.fragment.support.SetUserNicknameDialogFragment;
import org.mariotaku.twidere.fragment.support.StatusFavoritersListFragment;
import org.mariotaku.twidere.fragment.support.StatusFragment;
import org.mariotaku.twidere.fragment.support.StatusRepliesListFragment;
import org.mariotaku.twidere.fragment.support.StatusRetweetersListFragment;
import org.mariotaku.twidere.fragment.support.StatusesListFragment;
import org.mariotaku.twidere.fragment.support.UserBlocksListFragment;
import org.mariotaku.twidere.fragment.support.UserFavoritesFragment;
import org.mariotaku.twidere.fragment.support.UserFollowersFragment;
import org.mariotaku.twidere.fragment.support.UserFragment;
import org.mariotaku.twidere.fragment.support.UserFriendsFragment;
import org.mariotaku.twidere.fragment.support.UserListFragment;
import org.mariotaku.twidere.fragment.support.UserListMembersFragment;
import org.mariotaku.twidere.fragment.support.UserListMembershipsFragment;
import org.mariotaku.twidere.fragment.support.UserListSubscribersFragment;
import org.mariotaku.twidere.fragment.support.UserListTimelineFragment;
import org.mariotaku.twidere.fragment.support.UserMediaTimelineFragment;
import org.mariotaku.twidere.fragment.support.UserMentionsFragment;
import org.mariotaku.twidere.fragment.support.UserProfileEditorFragment;
import org.mariotaku.twidere.fragment.support.UserTimelineFragment;
import org.mariotaku.twidere.fragment.support.UsersListFragment;
import org.mariotaku.twidere.graphic.ActionIconDrawable;
import org.mariotaku.twidere.graphic.PaddingDrawable;
import org.mariotaku.twidere.menu.SupportStatusShareProvider;
import org.mariotaku.twidere.menu.support.FavoriteItemProvider;
import org.mariotaku.twidere.model.AccountPreferences;
import org.mariotaku.twidere.model.ParcelableAccount;
import org.mariotaku.twidere.model.ParcelableCredentials;
import org.mariotaku.twidere.model.ParcelableCredentialsCursorIndices;
import org.mariotaku.twidere.model.ParcelableDirectMessage;
import org.mariotaku.twidere.model.ParcelableDirectMessageCursorIndices;
import org.mariotaku.twidere.model.ParcelableLocation;
import org.mariotaku.twidere.model.ParcelableStatus;
import org.mariotaku.twidere.model.ParcelableStatusCursorIndices;
import org.mariotaku.twidere.model.ParcelableUser;
import org.mariotaku.twidere.model.ParcelableUserList;
import org.mariotaku.twidere.model.ParcelableUserMention;
import org.mariotaku.twidere.model.PebbleMessage;
import org.mariotaku.twidere.model.util.ParcelableStatusUtils;
import org.mariotaku.twidere.provider.TwidereDataStore;
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedRelationships;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedStatuses;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedUsers;
import org.mariotaku.twidere.provider.TwidereDataStore.DirectMessages;
import org.mariotaku.twidere.provider.TwidereDataStore.DirectMessages.ConversationEntries;
import org.mariotaku.twidere.provider.TwidereDataStore.Statuses;
import org.mariotaku.twidere.service.RefreshService;
import org.mariotaku.twidere.util.TwidereLinkify.HighlightStyle;
import org.mariotaku.twidere.util.content.ContentResolverUtils;
import org.mariotaku.twidere.util.menu.TwidereMenuInfo;
import org.mariotaku.twidere.view.CardMediaContainer.PreviewStyle;
import org.mariotaku.twidere.view.ShapedImageView;
import org.mariotaku.twidere.view.ShapedImageView.ShapeStyle;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import javax.net.ssl.SSLException;

import edu.tsinghua.hotmobi.HotMobiLogger;
import edu.tsinghua.hotmobi.model.NotificationEvent;

import static android.text.TextUtils.isEmpty;
import static android.text.format.DateUtils.getRelativeTimeSpanString;
import static org.mariotaku.twidere.provider.TwidereDataStore.DIRECT_MESSAGES_URIS;
import static org.mariotaku.twidere.provider.TwidereDataStore.STATUSES_URIS;
import static org.mariotaku.twidere.util.TwidereLinkify.PATTERN_TWITTER_PROFILE_IMAGES;
import static org.mariotaku.twidere.util.TwidereLinkify.TWITTER_PROFILE_IMAGES_AVAILABLE_SIZES;

@SuppressWarnings("unused")
public final class Utils implements Constants {

    public static final Pattern PATTERN_XML_RESOURCE_IDENTIFIER = Pattern.compile("res/xml/([\\w_]+)\\.xml");
    public static final Pattern PATTERN_RESOURCE_IDENTIFIER = Pattern.compile("@([\\w_]+)/([\\w_]+)");

    private static final UriMatcher LINK_HANDLER_URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final UriMatcher HOME_TABS_URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {

        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS, null, LINK_ID_STATUS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER, null, LINK_ID_USER);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_TIMELINE, null, LINK_ID_USER_TIMELINE);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_MEDIA_TIMELINE, null, LINK_ID_USER_MEDIA_TIMELINE);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FOLLOWERS, null, LINK_ID_USER_FOLLOWERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FRIENDS, null, LINK_ID_USER_FRIENDS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FAVORITES, null, LINK_ID_USER_FAVORITES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_BLOCKS, null, LINK_ID_USER_BLOCKS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DIRECT_MESSAGES_CONVERSATION, null,
                LINK_ID_DIRECT_MESSAGES_CONVERSATION);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST, null, LINK_ID_USER_LIST);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_TIMELINE, null, LINK_ID_USER_LIST_TIMELINE);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_MEMBERS, null, LINK_ID_USER_LIST_MEMBERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_SUBSCRIBERS, null, LINK_ID_USER_LIST_SUBSCRIBERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_MEMBERSHIPS, null, LINK_ID_USER_LIST_MEMBERSHIPS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LISTS, null, LINK_ID_USER_LISTS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SAVED_SEARCHES, null, LINK_ID_SAVED_SEARCHES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_MENTIONS, null, LINK_ID_USER_MENTIONS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_INCOMING_FRIENDSHIPS, null, LINK_ID_INCOMING_FRIENDSHIPS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USERS, null, LINK_ID_USERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUSES, null, LINK_ID_STATUSES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS_RETWEETERS, null, LINK_ID_STATUS_RETWEETERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS_FAVORITERS, null, LINK_ID_STATUS_FAVORITERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS_REPLIES, null, LINK_ID_STATUS_REPLIES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SEARCH, null, LINK_ID_SEARCH);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_MUTES_USERS, null, LINK_ID_MUTES_USERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_MAP, null, LINK_ID_MAP);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SCHEDULED_STATUSES, null, LINK_ID_SCHEDULED_STATUSES);

        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_ACCOUNTS, null, LINK_ID_ACCOUNTS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DRAFTS, null, LINK_ID_DRAFTS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_FILTERS, null, LINK_ID_FILTERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_PROFILE_EDITOR, null, LINK_ID_PROFILE_EDITOR);

        HOME_TABS_URI_MATCHER.addURI(CustomTabType.HOME_TIMELINE, null, TAB_CODE_HOME_TIMELINE);
        HOME_TABS_URI_MATCHER.addURI(CustomTabType.NOTIFICATIONS_TIMELINE, null, TAB_CODE_NOTIFICATIONS_TIMELINE);
        HOME_TABS_URI_MATCHER.addURI(CustomTabType.DIRECT_MESSAGES, null, TAB_CODE_DIRECT_MESSAGES);
    }


    private Utils() {
        throw new AssertionError("You are trying to create an instance for this utility class!");
    }

    public static void addIntentToMenuForExtension(final Context context, final Menu menu,
                                                   final int groupId, final String action,
                                                   final String parcelableKey, final String parcelableJSONKey,
                                                   final Parcelable parcelable) {
        if (context == null || menu == null || action == null || parcelableKey == null || parcelable == null)
            return;
        final PackageManager pm = context.getPackageManager();
        final Resources res = context.getResources();
        final float density = res.getDisplayMetrics().density;
        final int padding = Math.round(density * 4);
        final Intent queryIntent = new Intent(action);
        queryIntent.setExtrasClassLoader(context.getClassLoader());
        final List<ResolveInfo> activities = pm.queryIntentActivities(queryIntent, PackageManager.GET_META_DATA);
        final String parcelableJson = JsonSerializer.serialize(parcelable);
        for (final ResolveInfo info : activities) {
            final Intent intent = new Intent(queryIntent);
            if (isExtensionUseJSON(info) && parcelableJson != null) {
                intent.putExtra(parcelableJSONKey, parcelableJson);
            } else {
                intent.putExtra(parcelableKey, parcelable);
            }
            intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            final MenuItem item = menu.add(groupId, Menu.NONE, Menu.NONE, info.loadLabel(pm));
            item.setIntent(intent);
            final Drawable metaDataDrawable = getMetadataDrawable(pm, info.activityInfo, METADATA_KEY_EXTENSION_ICON);
            final int actionIconColor = ThemeUtils.getThemeForegroundColor(context);
            if (metaDataDrawable != null) {
                metaDataDrawable.mutate();
                metaDataDrawable.setColorFilter(actionIconColor, Mode.SRC_ATOP);
                item.setIcon(metaDataDrawable);
            } else {
                final Drawable icon = info.loadIcon(pm);
                final int iw = icon.getIntrinsicWidth(), ih = icon.getIntrinsicHeight();
                if (iw > 0 && ih > 0) {
                    final Drawable iconWithPadding = new PaddingDrawable(icon, padding);
                    iconWithPadding.setBounds(0, 0, iw, ih);
                    item.setIcon(iconWithPadding);
                } else {
                    item.setIcon(icon);
                }
            }

        }
    }

    public static void announceForAccessibilityCompat(final Context context, final View view, final CharSequence text,
                                                      final Class<?> cls) {
        final AccessibilityManager accessibilityManager = (AccessibilityManager) context
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (!accessibilityManager.isEnabled()) return;
        // Prior to SDK 16, announcements could only be made through FOCUSED
        // events. Jelly Bean (SDK 16) added support for speaking text verbatim
        // using the ANNOUNCEMENT event type.
        final int eventType;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            eventType = AccessibilityEvent.TYPE_VIEW_FOCUSED;
        } else {
            eventType = AccessibilityEventCompat.TYPE_ANNOUNCEMENT;
        }

        // Construct an accessibility event with the minimum recommended
        // attributes. An event without a class name or package may be dropped.
        final AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        event.getText().add(text);
        event.setClassName(cls.getName());
        event.setPackageName(context.getPackageName());
        event.setSource(view);

        // Sends the event directly through the accessibility manager. If your
        // application only targets SDK 14+, you should just call
        // getParent().requestSendAccessibilityEvent(this, event);
        accessibilityManager.sendAccessibilityEvent(event);
    }

    public static Uri buildDirectMessageConversationUri(final long account_id, final long conversation_id,
                                                        final String screen_name) {
        if (conversation_id <= 0 && screen_name == null) return TwidereDataStore.CONTENT_URI_NULL;
        final Uri.Builder builder = conversation_id > 0 ? DirectMessages.Conversation.CONTENT_URI.buildUpon()
                : DirectMessages.Conversation.CONTENT_URI_SCREEN_NAME.buildUpon();
        builder.appendPath(String.valueOf(account_id));
        builder.appendPath(conversation_id > 0 ? String.valueOf(conversation_id) : screen_name);
        return builder.build();
    }

    public static int calculateInSampleSize(final int width, final int height, final int preferredWidth,
                                            final int preferredHeight) {
        if (preferredHeight > height && preferredWidth > width) return 1;
        final int result = Math.round(Math.max(width, height) / (float) Math.max(preferredWidth, preferredHeight));
        return Math.max(1, result);
    }

    public static boolean checkActivityValidity(final Context context, final Intent intent) {
        final PackageManager pm = context.getPackageManager();
        return !pm.queryIntentActivities(intent, 0).isEmpty();
    }

    public static void clearListViewChoices(final AbsListView view) {
        if (view == null) return;
        final ListAdapter adapter = view.getAdapter();
        if (adapter == null) return;
        view.clearChoices();
        for (int i = 0, j = view.getChildCount(); i < j; i++) {
            view.setItemChecked(i, false);
        }
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            }
        });
        // Workaround for Android bug
        // http://stackoverflow.com/questions/9754170/listview-selection-remains-persistent-after-exiting-choice-mode
//        final int position = view.getFirstVisiblePosition(), offset = Utils.getFirstChildOffset(view);
//        view.setAdapter(adapter);
//        Utils.scrollListToPosition(view, position, offset);
    }

    public static boolean closeSilently(final Closeable c) {
        if (c == null) return false;
        try {
            c.close();
        } catch (final IOException e) {
            return false;
        }
        return true;
    }

    public static boolean closeSilently(final Cursor c) {
        if (c == null) return false;
        c.close();
        return true;
    }

    public static void configBaseAdapter(final Context context, final IBaseAdapter adapter) {
        if (context == null) return;
        final SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        adapter.setDisplayProfileImage(pref.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true));
        adapter.setDisplayNameFirst(pref.getBoolean(KEY_NAME_FIRST, true));
        adapter.setLinkHighlightOption(pref.getString(KEY_LINK_HIGHLIGHT_OPTION, VALUE_LINK_HIGHLIGHT_OPTION_NONE));
        adapter.setTextSize(pref.getInt(KEY_TEXT_SIZE, getDefaultTextSize(context)));
        adapter.notifyDataSetChanged();
    }

    public static void configBaseCardAdapter(final Context context, final IBaseCardAdapter adapter) {
        if (context == null) return;
        configBaseAdapter(context, adapter);
        adapter.notifyDataSetChanged();
    }

    public static int[] getAccountColors(@Nullable final ParcelableAccount[] accounts) {
        if (accounts == null) return null;
        final int[] colors = new int[accounts.length];
        for (int i = 0, j = accounts.length; i < j; i++) {
            colors[i] = accounts[i].color;
        }
        return colors;
    }

    public static Fragment createFragmentForIntent(final Context context, final Intent intent) {
        final Uri uri = intent.getData();
        return createFragmentForIntent(context, matchLinkId(uri), intent);
    }

    public static Fragment createFragmentForIntent(final Context context, final int linkId, final Intent intent) {
        intent.setExtrasClassLoader(context.getClassLoader());
        final Bundle extras = intent.getExtras();
        final Uri uri = intent.getData();
        final Fragment fragment;
        if (uri == null) return null;
        final Bundle args = new Bundle();
        if (extras != null) {
            args.putAll(extras);
        }
        switch (linkId) {
            case LINK_ID_ACCOUNTS: {
                fragment = new AccountsManagerFragment();
                break;
            }
            case LINK_ID_DRAFTS: {
                fragment = new DraftsFragment();
                break;
            }
            case LINK_ID_FILTERS: {
                fragment = new FiltersFragment();
                break;
            }
            case LINK_ID_PROFILE_EDITOR: {
                if (!args.containsKey(EXTRA_ACCOUNT_ID) && uri.getQueryParameter(QUERY_PARAM_ACCOUNT_ID) == null) {
                    return null;
                }
                fragment = new UserProfileEditorFragment();
                break;
            }
            case LINK_ID_MAP: {
                if (!args.containsKey(EXTRA_LATITUDE) && !args.containsKey(EXTRA_LONGITUDE)) {
                    final double lat = NumberUtils.toDouble(uri.getQueryParameter(QUERY_PARAM_LAT), Double.NaN);
                    final double lng = NumberUtils.toDouble(uri.getQueryParameter(QUERY_PARAM_LNG), Double.NaN);
                    if (Double.isNaN(lat) || Double.isNaN(lng)) return null;
                    args.putDouble(EXTRA_LATITUDE, lat);
                    args.putDouble(EXTRA_LONGITUDE, lng);
                }
                fragment = MapFragmentFactory.SINGLETON.createMapFragment(context);
                break;
            }
            case LINK_ID_STATUS: {
                fragment = new StatusFragment();
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    final String param_status_id = uri.getQueryParameter(QUERY_PARAM_STATUS_ID);
                    args.putLong(EXTRA_STATUS_ID, NumberUtils.toLong(param_status_id, -1));
                }
                break;
            }
            case LINK_ID_USER: {
                fragment = new UserFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String param_user_id = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(param_user_id, -1));
                }
                break;
            }
            case LINK_ID_USER_LIST_MEMBERSHIPS: {
                fragment = new UserListMembershipsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                break;
            }
            case LINK_ID_USER_TIMELINE: {
                fragment = new UserTimelineFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(paramUserId)) return null;
                break;
            }
            case LINK_ID_USER_MEDIA_TIMELINE: {
                fragment = new UserMediaTimelineFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(paramUserId)) return null;
                break;
            }
            case LINK_ID_USER_FAVORITES: {
                fragment = new UserFavoritesFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !args.containsKey(EXTRA_USER_ID))
                    return null;
                break;
            }
            case LINK_ID_USER_FOLLOWERS: {
                fragment = new UserFollowersFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String param_user_id = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(param_user_id, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(param_user_id)) return null;
                break;
            }
            case LINK_ID_USER_FRIENDS: {
                fragment = new UserFriendsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String param_user_id = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(param_user_id, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(param_user_id)) return null;
                break;
            }
            case LINK_ID_USER_BLOCKS: {
                fragment = new UserBlocksListFragment();
                break;
            }
            case LINK_ID_MUTES_USERS: {
                fragment = new MutesUsersListFragment();
                break;
            }
            case LINK_ID_SCHEDULED_STATUSES: {
                fragment = new ScheduledStatusesFragment();
                break;
            }
            case LINK_ID_DIRECT_MESSAGES_CONVERSATION: {
                fragment = new MessagesConversationFragment();
                final String paramRecipientId = uri.getQueryParameter(QUERY_PARAM_RECIPIENT_ID);
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final long conversationId = NumberUtils.toLong(paramRecipientId, -1);
                if (conversationId > 0) {
                    args.putLong(EXTRA_RECIPIENT_ID, conversationId);
                } else if (paramScreenName != null) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                break;
            }
            case LINK_ID_USER_LIST: {
                fragment = new UserListFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_USER_LISTS: {
                fragment = new ListsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(paramUserId)) return null;
                break;
            }
            case LINK_ID_USER_LIST_TIMELINE: {
                fragment = new UserListTimelineFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_USER_LIST_MEMBERS: {
                fragment = new UserListMembersFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_USER_LIST_SUBSCRIBERS: {
                fragment = new UserListSubscribersFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_SAVED_SEARCHES: {
                fragment = new SavedSearchesListFragment();
                break;
            }
            case LINK_ID_USER_MENTIONS: {
                fragment = new UserMentionsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !isEmpty(paramScreenName)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (isEmpty(args.getString(EXTRA_SCREEN_NAME))) return null;
                break;
            }
            case LINK_ID_INCOMING_FRIENDSHIPS: {
                fragment = new IncomingFriendshipsFragment();
                break;
            }
            case LINK_ID_USERS: {
                fragment = new UsersListFragment();
                break;
            }
            case LINK_ID_STATUSES: {
                fragment = new StatusesListFragment();
                break;
            }
            case LINK_ID_STATUS_RETWEETERS: {
                fragment = new StatusRetweetersListFragment();
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    final String paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID);
                    args.putLong(EXTRA_STATUS_ID, NumberUtils.toLong(paramStatusId, -1));
                }
                break;
            }
            case LINK_ID_STATUS_FAVORITERS: {
                fragment = new StatusFavoritersListFragment();
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    final String paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID);
                    args.putLong(EXTRA_STATUS_ID, NumberUtils.toLong(paramStatusId, -1));
                }
                break;
            }
            case LINK_ID_STATUS_REPLIES: {
                fragment = new StatusRepliesListFragment();
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    final String paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID);
                    args.putLong(EXTRA_STATUS_ID, NumberUtils.toLong(paramStatusId, -1));
                }
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                break;
            }
            case LINK_ID_SEARCH: {
                final String paramQuery = uri.getQueryParameter(QUERY_PARAM_QUERY);
                if (!args.containsKey(EXTRA_QUERY) && !isEmpty(paramQuery)) {
                    args.putString(EXTRA_QUERY, paramQuery);
                }
                if (!args.containsKey(EXTRA_QUERY)) {
                    return null;
                }
                fragment = new SearchFragment();
                break;
            }
            default: {
                return null;
            }
        }
        final String paramAccountId = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_ID);
        if (paramAccountId != null) {
            args.putLong(EXTRA_ACCOUNT_ID, NumberUtils.toLong(paramAccountId, -1));
        } else {
            final String paramAccountName = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_NAME);
            if (paramAccountName != null) {
                args.putLong(EXTRA_ACCOUNT_ID, DataStoreUtils.getAccountId(context, paramAccountName));
            } else {
                final long accountId = getDefaultAccountId(context);
                if (isMyAccount(context, accountId)) {
                    args.putLong(EXTRA_ACCOUNT_ID, accountId);
                }
            }
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static Intent createStatusShareIntent(@NonNull final Context context, @NonNull final ParcelableStatus status) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, IntentUtils.getStatusShareSubject(context, status));
        intent.putExtra(Intent.EXTRA_TEXT, IntentUtils.getStatusShareText(context, status));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }


    public static String getReadPositionTagWithAccounts(@ReadPositionTag String tag, Bundle args) {
        final long[] accountIds = getAccountIds(args);
        return getReadPositionTagWithAccounts(tag, accountIds);
    }

    public static long[] getAccountIds(Bundle args) {
        final long[] accountIds;
        if (args.containsKey(EXTRA_ACCOUNT_IDS)) {
            accountIds = args.getLongArray(EXTRA_ACCOUNT_IDS);
        } else if (args.containsKey(EXTRA_ACCOUNT_ID)) {
            accountIds = new long[]{args.getLong(EXTRA_ACCOUNT_ID, -1)};
        } else {
            accountIds = null;
        }
        return accountIds;
    }

    @Nullable
    public static String getReadPositionTagWithAccounts(@Nullable @ReadPositionTag final String tag,
                                                        final long... accountIds) {
        if (tag == null) return null;
        if (accountIds == null || accountIds.length == 0 || (accountIds.length == 1 && accountIds[0] < 0))
            return tag;
        final long[] accountIdsClone = accountIds.clone();
        Arrays.sort(accountIdsClone);
        return tag + "_" + TwidereArrayUtils.toString(accountIdsClone, '_', false);
    }

    @Nullable
    public static String getReadPositionTagWithAccounts(Context context, boolean activatedIfMissing,
                                                        @Nullable @ReadPositionTag String tag,
                                                        long... accountIds) {
        if (tag == null) return null;
        if (accountIds == null || accountIds.length == 0 || (accountIds.length == 1 && accountIds[0] < 0)) {
            final long[] activatedIds = DataStoreUtils.getActivatedAccountIds(context);
            Arrays.sort(activatedIds);
            return tag + "_" + TwidereArrayUtils.toString(activatedIds, '_', false);
        }
        final long[] accountIdsClone = accountIds.clone();
        Arrays.sort(accountIdsClone);
        return tag + "_" + TwidereArrayUtils.toString(accountIdsClone, '_', false);
    }

    public static String encodeQueryParams(final String value) throws IOException {
        final String encoded = URLEncoder.encode(value, "UTF-8");
        final StringBuilder buf = new StringBuilder();
        final int length = encoded.length();
        char focus;
        for (int i = 0; i < length; i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                buf.append("%2A");
            } else if (focus == '+') {
                buf.append("%20");
            } else if (focus == '%' && i + 1 < encoded.length() && encoded.charAt(i + 1) == '7'
                    && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(focus);
            }
        }
        return buf.toString();
    }

    public static ParcelableDirectMessage findDirectMessageInDatabases(final Context context, final long account_id,
                                                                       final long message_id) {
        if (context == null) return null;
        final ContentResolver resolver = context.getContentResolver();
        ParcelableDirectMessage message = null;
        final String where = DirectMessages.ACCOUNT_ID + " = " + account_id + " AND " + DirectMessages.MESSAGE_ID
                + " = " + message_id;
        for (final Uri uri : DIRECT_MESSAGES_URIS) {
            final Cursor cur = ContentResolverUtils.query(resolver, uri, DirectMessages.COLUMNS, where, null, null);
            if (cur == null) {
                continue;
            }
            if (cur.getCount() > 0 && cur.moveToFirst()) {
                message = ParcelableDirectMessageCursorIndices.fromCursor(cur);
            }
            cur.close();
        }
        return message;
    }

    @NonNull
    public static ParcelableStatus findStatus(final Context context, final long accountId, final long statusId)
            throws TwitterException {
        if (context == null) throw new NullPointerException();
        final ParcelableStatus cached = findStatusInDatabases(context, accountId, statusId);
        if (cached != null) return cached;
        final Twitter twitter = TwitterAPIFactory.getTwitterInstance(context, accountId, true);
        if (twitter == null) throw new TwitterException("Account does not exist");
        final Status status = twitter.showStatus(statusId);
        final String where = Expression.and(Expression.equals(Statuses.ACCOUNT_ID, accountId),
                Expression.equals(Statuses.STATUS_ID, statusId)).getSQL();
        final ContentResolver resolver = context.getContentResolver();
        resolver.delete(CachedStatuses.CONTENT_URI, where, null);
        resolver.insert(CachedStatuses.CONTENT_URI, ContentValuesCreator.createStatus(status, accountId));
        return ParcelableStatusUtils.fromStatus(status, accountId, false);
    }

    @Nullable
    public static ParcelableStatus findStatusInDatabases(final Context context, final long accountId,
                                                         final long statusId) {
        if (context == null) return null;
        final ContentResolver resolver = context.getContentResolver();
        ParcelableStatus status = null;
        final String where = Expression.and(Expression.equals(Statuses.ACCOUNT_ID, accountId),
                Expression.equals(Statuses.STATUS_ID, statusId)).getSQL();
        for (final Uri uri : STATUSES_URIS) {
            final Cursor cur = ContentResolverUtils.query(resolver, uri, Statuses.COLUMNS, where, null, null);
            if (cur == null) {
                continue;
            }
            if (cur.getCount() > 0) {
                cur.moveToFirst();
                status = ParcelableStatusCursorIndices.fromCursor(cur);
            }
            cur.close();
        }
        return status;
    }

    @SuppressWarnings("deprecation")
    public static String formatSameDayTime(final Context context, final long timestamp) {
        if (context == null) return null;
        if (DateUtils.isToday(timestamp))
            return DateUtils.formatDateTime(context, timestamp,
                    DateFormat.is24HourFormat(context) ? DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR
                            : DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_12HOUR);
        return DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE);
    }

    @SuppressWarnings("deprecation")
    public static String formatTimeStampString(final Context context, final long timestamp) {
        if (context == null) return null;
        final Time then = new Time();
        then.set(timestamp);
        final Time now = new Time();
        now.setToNow();

        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_CAP_AMPM;

        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        return DateUtils.formatDateTime(context, timestamp, format_flags);
    }

    @SuppressWarnings("deprecation")
    public static String formatTimeStampString(final Context context, final String date_time) {
        if (context == null) return null;
        return formatTimeStampString(context, Date.parse(date_time));
    }

    @SuppressWarnings("deprecation")
    public static String formatToLongTimeString(final Context context, final long timestamp) {
        if (context == null) return null;
        final Time then = new Time();
        then.set(timestamp);
        final Time now = new Time();
        now.setToNow();

        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_CAP_AMPM;

        format_flags |= DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;

        return DateUtils.formatDateTime(context, timestamp, format_flags);
    }

    public static int getAccountNotificationId(final int notificationType, final long accountId) {
        return Arrays.hashCode(new long[]{notificationType, accountId});
    }

    public static boolean isComposeNowSupported(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || context == null) return false;
        return hasNavBar(context);
    }

    public static boolean isOfficialCredentials(final Context context, final ParcelableCredentials account) {
        if (account == null) return false;
        final boolean isOAuth = account.auth_type == ParcelableCredentials.AUTH_TYPE_OAUTH
                || account.auth_type == ParcelableCredentials.AUTH_TYPE_XAUTH;
        final String consumerKey = account.consumer_key, consumerSecret = account.consumer_secret;
        return isOAuth && TwitterContentUtils.isOfficialKey(context, consumerKey, consumerSecret);
    }

    public static TextView newSectionView(final Context context, final int titleRes) {
        return newSectionView(context, titleRes != 0 ? context.getString(titleRes) : null);
    }

    public static TextView newSectionView(final Context context, final CharSequence title) {
        final TextView textView = new TextView(context, null, android.R.attr.listSeparatorTextViewStyle);
        textView.setText(title);
        return textView;
    }

    public static boolean setLastSeen(Context context, ParcelableUserMention[] entities, long time) {
        if (entities == null) return false;
        boolean result = false;
        for (ParcelableUserMention entity : entities) {
            result |= setLastSeen(context, entity.id, time);
        }
        return result;
    }

    public static boolean setLastSeen(Context context, UserMentionEntity[] entities, long time) {
        if (entities == null) return false;
        boolean result = false;
        for (UserMentionEntity entity : entities) {
            result |= setLastSeen(context, entity.getId(), time);
        }
        return result;
    }

    public static boolean setLastSeen(Context context, long userId, long time) {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues values = new ContentValues();
        if (time > 0) {
            values.put(CachedUsers.LAST_SEEN, time);
        } else {
            // Zero or negative value means remove last seen
            values.putNull(CachedUsers.LAST_SEEN);
        }
        final Expression where = Expression.equals(CachedUsers.USER_ID, userId);
        return cr.update(CachedUsers.CONTENT_URI, values, where.getSQL(), null) != 0;
    }

    public static File getBestCacheDir(final Context context, final String cacheDirName) {
        if (context == null) throw new NullPointerException();
        final File extCacheDir;
        try {
            // Workaround for https://github.com/mariotaku/twidere/issues/138
            extCacheDir = context.getExternalCacheDir();
        } catch (final Exception e) {
            return new File(context.getCacheDir(), cacheDirName);
        }
        if (extCacheDir != null && extCacheDir.isDirectory()) {
            final File cacheDir = new File(extCacheDir, cacheDirName);
            if (cacheDir.isDirectory() || cacheDir.mkdirs()) return cacheDir;
        }
        return new File(context.getCacheDir(), cacheDirName);
    }

    public static String getBiggerTwitterProfileImage(final String url) {
        return getTwitterProfileImageOfSize(url, "bigger");
    }

    public static Bitmap getBitmap(final Drawable drawable) {
        if (drawable instanceof NinePatchDrawable) return null;
        if (drawable instanceof BitmapDrawable)
            return ((BitmapDrawable) drawable).getBitmap();
        else if (drawable instanceof TransitionDrawable) {
            final int layer_count = ((TransitionDrawable) drawable).getNumberOfLayers();
            for (int i = 0; i < layer_count; i++) {
                final Drawable layer = ((TransitionDrawable) drawable).getDrawable(i);
                if (layer instanceof BitmapDrawable) return ((BitmapDrawable) layer).getBitmap();
            }
        }
        return null;
    }

    public static Bitmap.CompressFormat getBitmapCompressFormatByMimeType(final String mimeType,
                                                                          final Bitmap.CompressFormat def) {
        final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if ("jpeg".equalsIgnoreCase(extension) || "jpg".equalsIgnoreCase(extension))
            return Bitmap.CompressFormat.JPEG;
        else if ("png".equalsIgnoreCase(extension))
            return Bitmap.CompressFormat.PNG;
        else if ("webp".equalsIgnoreCase(extension)) return Bitmap.CompressFormat.WEBP;
        return def;
    }

    public static int getCardHighlightColor(final Context context, final boolean isMention,
                                            final boolean isFavorite, final boolean isRetweet) {
        if (isMention)
            return ContextCompat.getColor(context, R.color.highlight_reply);
        else if (isFavorite)
            return ContextCompat.getColor(context, R.color.highlight_like);
        else if (isRetweet) ContextCompat.getColor(context, R.color.highlight_retweet);
        return Color.TRANSPARENT;
    }

    public static String getCardHighlightOption(final Context context) {
        if (context == null) return null;
        final String defaultOption = context.getString(R.string.default_tab_display_option);
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TAB_DISPLAY_OPTION, defaultOption);
    }

    public static int getCardHighlightOptionInt(final Context context) {
        return getCardHighlightOptionInt(getCardHighlightOption(context));
    }

    public static int getCardHighlightOptionInt(final String option) {
        if (VALUE_CARD_HIGHLIGHT_OPTION_NONE.equals(option))
            return VALUE_CARD_HIGHLIGHT_OPTION_CODE_NONE;
        else if (VALUE_CARD_HIGHLIGHT_OPTION_LINE.equals(option))
            return VALUE_CARD_HIGHLIGHT_OPTION_CODE_LINE;
        return VALUE_CARD_HIGHLIGHT_OPTION_CODE_BACKGROUND;
    }

    public static Selectable getColumnsFromProjection(final String... projection) {
        if (projection == null) return new AllColumns();
        final int length = projection.length;
        final Column[] columns = new Column[length];
        for (int i = 0; i < length; i++) {
            columns[i] = new Column(projection[i]);
        }
        return new Columns(columns);
    }

    public static long getDefaultAccountId(final Context context) {
        if (context == null) return -1;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final long accountId = prefs.getLong(KEY_DEFAULT_ACCOUNT_ID, -1);
        final long[] accountIds = DataStoreUtils.getAccountIds(context);
        if (accountIds.length > 0 && !ArrayUtils.contains(accountIds, accountId)) {
             /* TODO: this is just a quick fix */
            return accountIds[0];
        }
        return accountId;
    }

    public static String getDefaultAccountScreenName(final Context context) {
        if (context == null) return null;
        return DataStoreUtils.getAccountScreenName(context, getDefaultAccountId(context));
    }

    public static int getDefaultTextSize(final Context context) {
        if (context == null) return 15;
        return context.getResources().getInteger(R.integer.default_text_size);
    }

    public static String getErrorMessage(final Context context, final CharSequence message) {
        if (context == null) return ParseUtils.parseString(message);
        if (isEmpty(message)) return context.getString(R.string.error_unknown_error);
        return context.getString(R.string.error_message, message);
    }

    public static String getErrorMessage(final Context context, final CharSequence action, final CharSequence message) {
        if (context == null || isEmpty(action)) return ParseUtils.parseString(message);
        if (isEmpty(message)) return context.getString(R.string.error_unknown_error);
        return context.getString(R.string.error_message_with_action, action, message);
    }

    public static String getErrorMessage(final Context context, final CharSequence action, final Throwable t) {
        if (context == null) return null;
        if (t instanceof TwitterException)
            return getTwitterErrorMessage(context, action, (TwitterException) t);
        else if (t != null) return getErrorMessage(context, trimLineBreak(t.getMessage()));
        return context.getString(R.string.error_unknown_error);
    }

    public static String getErrorMessage(final Context context, final Throwable t) {
        if (t == null) return null;
        if (context != null && t instanceof TwitterException)
            return getTwitterErrorMessage(context, (TwitterException) t);
        return t.getMessage();
    }

    public static int getFirstChildOffset(final AbsListView list) {
        if (list == null || list.getChildCount() == 0) return 0;
        final View child = list.getChildAt(0);
        final int[] location = new int[2];
        child.getLocationOnScreen(location);
        Log.d(LOGTAG, String.format("getFirstChildOffset %d vs %d", child.getTop(), location[1]));
        return child.getTop();
    }


    public static String getImageMimeType(final File image) {
        if (image == null) return null;
        final BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image.getPath(), o);
        return o.outMimeType;
    }

    public static String getImageMimeType(final InputStream is) {
        if (is == null) return null;
        final BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, o);
        return o.outMimeType;
    }

    public static String getImagePathFromUri(final Context context, final Uri uri) {
        if (context == null || uri == null) return null;

        final String mediaUriStart = ParseUtils.parseString(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        if (ParseUtils.parseString(uri).startsWith(mediaUriStart)) {

            final String[] proj = {MediaStore.Images.Media.DATA};
            final Cursor cur = ContentResolverUtils.query(context.getContentResolver(), uri, proj, null, null, null);

            if (cur == null) return null;

            final int idxData = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cur.moveToFirst();
            try {
                return cur.getString(idxData);
            } finally {
                cur.close();
            }
        } else {
            final String path = uri.getPath();
            if (path != null && new File(path).exists()) return path;
        }
        return null;
    }

    public static String getImageUploadStatus(@NonNull final Context context,
                                              @Nullable final CharSequence[] links,
                                              @Nullable final CharSequence text) {
        if (ArrayUtils.isEmpty(links) || text == null) return ParseUtils.parseString(text);
        return text + " " + TwidereArrayUtils.toString(links, ' ', false);
    }

    public static File getInternalCacheDir(final Context context, final String cacheDirName) {
        if (context == null) throw new NullPointerException();
        final File cacheDir = new File(context.getCacheDir(), cacheDirName);
        if (cacheDir.isDirectory() || cacheDir.mkdirs()) return cacheDir;
        return new File(context.getCacheDir(), cacheDirName);
    }

    @Nullable
    public static File getExternalCacheDir(final Context context, final String cacheDirName) {
        if (context == null) throw new NullPointerException();
        final File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir == null) return null;
        final File cacheDir = new File(externalCacheDir, cacheDirName);
        if (cacheDir.isDirectory() || cacheDir.mkdirs()) return cacheDir;
        return new File(context.getCacheDir(), cacheDirName);
    }

    public static CharSequence getKeywordBoldedText(final CharSequence orig, final String... keywords) {
        return getKeywordHighlightedText(orig, new StyleSpan(Typeface.BOLD), keywords);
    }

    public static CharSequence getKeywordHighlightedText(final CharSequence orig, final CharacterStyle style,
                                                         final String... keywords) {
        if (keywords == null || keywords.length == 0 || orig == null) return orig;
        final SpannableStringBuilder sb = SpannableStringBuilder.valueOf(orig);
        final StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0, j = keywords.length; i < j; i++) {
            if (i != 0) {
                patternBuilder.append('|');
            }
            patternBuilder.append(Pattern.quote(keywords[i]));
        }
        final Matcher m = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE).matcher(orig);
        while (m.find()) {
            sb.setSpan(style, m.start(), m.end(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return sb;
    }

    public static String getLinkHighlightingStyleName(final Context context) {
        if (context == null) return null;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LINK_HIGHLIGHT_OPTION, VALUE_LINK_HIGHLIGHT_OPTION_NONE);
    }

    @HighlightStyle
    public static int getLinkHighlightingStyle(final Context context) {
        return getLinkHighlightingStyleInt(getLinkHighlightingStyleName(context));
    }

    @HighlightStyle
    public static int getLinkHighlightingStyleInt(final String option) {
        if (option == null) return VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE;
        switch (option) {
            case VALUE_LINK_HIGHLIGHT_OPTION_BOTH:
                return VALUE_LINK_HIGHLIGHT_OPTION_CODE_BOTH;
            case VALUE_LINK_HIGHLIGHT_OPTION_HIGHLIGHT:
                return VALUE_LINK_HIGHLIGHT_OPTION_CODE_HIGHLIGHT;
            case VALUE_LINK_HIGHLIGHT_OPTION_UNDERLINE:
                return VALUE_LINK_HIGHLIGHT_OPTION_CODE_UNDERLINE;
        }
        return VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE;
    }

    public static String getLocalizedNumber(final Locale locale, final Number number) {
        final NumberFormat nf = NumberFormat.getInstance(locale);
        return nf.format(number);
    }

    public static long[] getMatchedNicknameIds(final String str, UserColorNameManager manager) {
        if (isEmpty(str)) return new long[0];
        final List<Long> list = new ArrayList<>();
        for (final Entry<String, ?> entry : manager.getNameEntries()) {
            final String value = ParseUtils.parseString(entry.getValue());
            final long def = -1;
            final long key = NumberUtils.toLong(entry.getKey(), def);
            if (key == -1 || isEmpty(value)) {
                continue;
            }
            if (TwidereStringUtils.startsWithIgnoreCase(value, str)) {
                list.add(key);
            }
        }
        return TwidereArrayUtils.fromList(list);
    }

    public static String getNonEmptyString(final SharedPreferences pref, final String key, final String def) {
        if (pref == null) return def;
        final String val = pref.getString(key, def);
        return isEmpty(val) ? def : val;
    }

    public static String getNonEmptyString(final SharedPreferencesWrapper pref, final String key, final String def) {
        if (pref == null) return def;
        final String val = pref.getString(key, def);
        return isEmpty(val) ? def : val;
    }

    public static String getNormalTwitterProfileImage(final String url) {
        return getTwitterProfileImageOfSize(url, "normal");
    }

    public static Uri getNotificationUri(final int tableId, final Uri def) {
        switch (tableId) {
            case TABLE_ID_DIRECT_MESSAGES:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
                return DirectMessages.CONTENT_URI;
        }
        return def;
    }

    public static String getOriginalTwitterProfileImage(final String url) {
        if (url == null) return null;
        if (PATTERN_TWITTER_PROFILE_IMAGES.matcher(url).matches())
            return replaceLast(url, "_" + TWITTER_PROFILE_IMAGES_AVAILABLE_SIZES, "");
        return url;
    }

    @ShapeStyle
    public static int getProfileImageStyle(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final String style = prefs.getString(KEY_PROFILE_IMAGE_STYLE, null);
        return getProfileImageStyle(style);
    }

    @ShapeStyle
    public static int getProfileImageStyle(String style) {
        if (VALUE_PROFILE_IMAGE_STYLE_SQUARE.equalsIgnoreCase(style)) {
            return ShapedImageView.SHAPE_RECTANGLE;
        }
        return ShapedImageView.SHAPE_CIRCLE;
    }

    @PreviewStyle
    public static int getMediaPreviewStyle(String style) {
        if (VALUE_MEDIA_PREVIEW_STYLE_SCALE.equalsIgnoreCase(style)) {
            return VALUE_MEDIA_PREVIEW_STYLE_CODE_SCALE;
        }
        return VALUE_MEDIA_PREVIEW_STYLE_CODE_CROP;
    }

    public static String getQuoteStatus(final Context context, long statusId, final String screen_name, final String text) {
        if (context == null) return null;
        String quoteFormat = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(
                KEY_QUOTE_FORMAT, DEFAULT_QUOTE_FORMAT);
        if (isEmpty(quoteFormat)) {
            quoteFormat = DEFAULT_QUOTE_FORMAT;
        }
        String result = quoteFormat.replace(FORMAT_PATTERN_LINK, LinkCreator.getTwitterStatusLink(screen_name, statusId).toString());
        result = result.replace(FORMAT_PATTERN_NAME, screen_name);
        result = result.replace(FORMAT_PATTERN_TEXT, text);
        return result;
    }

    public static String getReasonablySmallTwitterProfileImage(final String url) {
        return getTwitterProfileImageOfSize(url, "reasonably_small");
    }

    public static int getResId(final Context context, final String string) {
        if (context == null || string == null) return 0;
        Matcher m = PATTERN_RESOURCE_IDENTIFIER.matcher(string);
        final Resources res = context.getResources();
        if (m.matches()) return res.getIdentifier(m.group(2), m.group(1), context.getPackageName());
        m = PATTERN_XML_RESOURCE_IDENTIFIER.matcher(string);
        if (m.matches()) return res.getIdentifier(m.group(1), "xml", context.getPackageName());
        return 0;
    }


    public static String getSenderUserName(final Context context, final ParcelableDirectMessage user) {
        if (context == null || user == null) return null;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final boolean display_name = prefs.getBoolean(KEY_NAME_FIRST, true);
        return display_name ? user.sender_name : "@" + user.sender_screen_name;
    }

    public static String getShareStatus(final Context context, final CharSequence title, final CharSequence text) {
        if (context == null) return null;
        String share_format = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(
                KEY_SHARE_FORMAT, DEFAULT_SHARE_FORMAT);
        if (isEmpty(share_format)) {
            share_format = DEFAULT_SHARE_FORMAT;
        }
        if (isEmpty(title)) return ParseUtils.parseString(text);
        return share_format.replace(FORMAT_PATTERN_TITLE, title).replace(FORMAT_PATTERN_TEXT, text != null ? text : "");
    }

    public static String getTabDisplayOption(final Context context) {
        if (context == null) return null;
        final String defaultOption = context.getString(R.string.default_tab_display_option);
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TAB_DISPLAY_OPTION, defaultOption);
    }

    public static int getTabDisplayOptionInt(final Context context) {
        return getTabDisplayOptionInt(getTabDisplayOption(context));
    }

    public static int getTabDisplayOptionInt(final String option) {
        if (VALUE_TAB_DISPLAY_OPTION_ICON.equals(option))
            return VALUE_TAB_DISPLAY_OPTION_CODE_ICON;
        else if (VALUE_TAB_DISPLAY_OPTION_LABEL.equals(option))
            return VALUE_TAB_DISPLAY_OPTION_CODE_LABEL;
        return VALUE_TAB_DISPLAY_OPTION_CODE_BOTH;
    }

    public static long getTimestampFromDate(final Date date) {
        if (date == null) return -1;
        return date.getTime();
    }

    public static boolean hasNavBar(@NonNull Context context) {
        final Resources resources = context.getResources();
        if (resources == null) return false;
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            return resources.getBoolean(id);
        } else {
            // Check for keys
            return !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
                    && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
        }
    }

    public static String getTwitterErrorMessage(final Context context, final CharSequence action,
                                                final TwitterException te) {
        if (context == null) return null;
        if (te == null) return context.getString(R.string.error_unknown_error);
        if (te.exceededRateLimitation()) {
            final RateLimitStatus status = te.getRateLimitStatus();
            final long secUntilReset = status.getSecondsUntilReset() * 1000;
            final String nextResetTime = ParseUtils.parseString(getRelativeTimeSpanString(System.currentTimeMillis()
                    + secUntilReset));
            if (isEmpty(action))
                return context.getString(R.string.error_message_rate_limit, nextResetTime.trim());
            return context.getString(R.string.error_message_rate_limit_with_action, action, nextResetTime.trim());
        } else if (te.getErrorCode() > 0) {
            final String msg = StatusCodeMessageUtils.getTwitterErrorMessage(context, te.getErrorCode());
            return getErrorMessage(context, action, msg != null ? msg : trimLineBreak(te.getMessage()));
        } else if (te.getCause() instanceof SSLException) {
            final String msg = te.getCause().getMessage();
            if (msg != null && msg.contains("!="))
                return getErrorMessage(context, action, context.getString(R.string.ssl_error));
            else
                return getErrorMessage(context, action, context.getString(R.string.network_error));
        } else if (te.getCause() instanceof IOException)
            return getErrorMessage(context, action, context.getString(R.string.network_error));
        else if (te.getCause() instanceof JSONException)
            return getErrorMessage(context, action, context.getString(R.string.api_data_corrupted));
        else
            return getErrorMessage(context, action, trimLineBreak(te.getMessage()));
    }

    public static String getTwitterErrorMessage(final Context context, final TwitterException te) {
        if (te == null) return null;
        if (StatusCodeMessageUtils.containsTwitterError(te.getErrorCode()))
            return StatusCodeMessageUtils.getTwitterErrorMessage(context, te.getErrorCode());
        else if (StatusCodeMessageUtils.containsHttpStatus(te.getStatusCode()))
            return StatusCodeMessageUtils.getHttpStatusMessage(context, te.getStatusCode());
        else
            return te.getMessage();
    }


    public static String getTwitterProfileImageOfSize(final String url, final String size) {
        if (url == null) return null;
        if (PATTERN_TWITTER_PROFILE_IMAGES.matcher(url).matches())
            return replaceLast(url, "_" + TWITTER_PROFILE_IMAGES_AVAILABLE_SIZES, String.format("_%s", size));
        return url;
    }

    @DrawableRes
    public static int getUserTypeIconRes(final boolean isVerified, final boolean isProtected) {
        if (isVerified)
            return R.drawable.ic_user_type_verified;
        else if (isProtected) return R.drawable.ic_user_type_protected;
        return 0;
    }

    @StringRes
    public static int getUserTypeDescriptionRes(final boolean isVerified, final boolean isProtected) {
        if (isVerified)
            return R.string.user_type_verified;
        else if (isProtected) return R.string.user_type_protected;
        return 0;
    }

    public static boolean hasAccountSignedWithOfficialKeys(final Context context) {
        if (context == null) return false;
        final Cursor cur = ContentResolverUtils.query(context.getContentResolver(), Accounts.CONTENT_URI,
                Accounts.COLUMNS, null, null, null);
        if (cur == null) return false;
        final String[] keySecrets = context.getResources().getStringArray(R.array.values_official_consumer_secret_crc32);
        final ParcelableCredentialsCursorIndices indices = new ParcelableCredentialsCursorIndices(cur);
        cur.moveToFirst();
        final CRC32 crc32 = new CRC32();
        try {
            while (!cur.isAfterLast()) {
                final String consumerSecret = cur.getString(indices.consumer_secret);
                if (consumerSecret != null) {
                    final byte[] consumerSecretBytes = consumerSecret.getBytes(Charset.forName("UTF-8"));
                    crc32.update(consumerSecretBytes, 0, consumerSecretBytes.length);
                    final long value = crc32.getValue();
                    crc32.reset();
                    for (final String keySecret : keySecrets) {
                        if (Long.parseLong(keySecret, 16) == value) return true;
                    }
                }
                cur.moveToNext();
            }
        } finally {
            cur.close();
        }
        return false;
    }

    public static boolean hasAutoRefreshAccounts(final Context context) {
        final long[] accountIds = DataStoreUtils.getAccountIds(context);
        return !ArrayUtils.isEmpty(AccountPreferences.getAutoRefreshEnabledAccountIds(context, accountIds));
    }

    public static boolean hasStaggeredTimeline() {
        return false;
    }

    public static void initAccountColor(final Context context) {
        if (context == null) return;
        final Cursor cur = ContentResolverUtils.query(context.getContentResolver(), Accounts.CONTENT_URI, new String[]{
                Accounts.ACCOUNT_ID, Accounts.COLOR}, null, null, null);
        if (cur == null) return;
        final int id_idx = cur.getColumnIndex(Accounts.ACCOUNT_ID), color_idx = cur.getColumnIndex(Accounts.COLOR);
        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            DataStoreUtils.sAccountColors.put(cur.getLong(id_idx), cur.getInt(color_idx));
            cur.moveToNext();
        }
        cur.close();
    }

    public static boolean isBatteryOkay(final Context context) {
        if (context == null) return false;
        final Context app = context.getApplicationContext();
        final IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent intent = app.registerReceiver(null, filter);
        if (intent == null) return false;
        final boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        final float level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        final float scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return plugged || level / scale > 0.15f;
    }

    public static boolean isCompactCards(final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs != null && prefs.getBoolean(KEY_COMPACT_CARDS, false);
    }

    public static boolean isDatabaseReady(final Context context) {
        final Cursor c = context.getContentResolver().query(TwidereDataStore.CONTENT_URI_DATABASE_READY, null, null, null,
                null);
        try {
            return c != null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static boolean isMyAccount(final Context context, final long accountId) {
        if (context == null) return false;
        final ContentResolver resolver = context.getContentResolver();
        final String where = Expression.equals(Accounts.ACCOUNT_ID, accountId).getSQL();
        final Cursor cur = ContentResolverUtils.query(resolver, Accounts.CONTENT_URI, new String[0], where, null, null);
        try {
            return cur != null && cur.getCount() > 0;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    public static boolean isMyAccount(final Context context, final String screen_name) {
        if (context == null) return false;
        final ContentResolver resolver = context.getContentResolver();
        final String where = Expression.equalsArgs(Accounts.SCREEN_NAME).getSQL();
        final Cursor cur = ContentResolverUtils.query(resolver, Accounts.CONTENT_URI, new String[0], where,
                new String[]{screen_name}, null);
        try {
            return cur != null && cur.getCount() > 0;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    public static boolean isMyRetweet(final ParcelableStatus status) {
        return status != null && isMyRetweet(status.account_id, status.retweeted_by_user_id, status.my_retweet_id);
    }

    public static boolean isMyRetweet(final long accountId, final long retweetedById, final long myRetweetId) {
        return retweetedById == accountId || myRetweetId > 0;
    }

    public static boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static boolean isRedirected(final int code) {
        return code == 301 || code == 302 || code == 307;
    }

    public static boolean isRTL(final Context context) {
        if (context == null) return false;
        final Resources res = context.getResources();
        return "ar".equals(res.getConfiguration().locale.getLanguage());
        // return
        // ConfigurationAccessor.getLayoutDirection(res.getConfiguration()) ==
        // SCREENLAYOUT_LAYOUTDIR_RTL;
    }

    public static boolean isUserLoggedIn(final Context context, final long accountId) {
        if (context == null) return false;
        final long[] ids = DataStoreUtils.getAccountIds(context);
        for (final long id : ids) {
            if (id == accountId) return true;
        }
        return false;
    }

    public static int matchLinkId(@Nullable final Uri uri) {
        if (uri == null) return UriMatcher.NO_MATCH;
        return LINK_HANDLER_URI_MATCHER.match(uri);
    }


    public static int matchTabCode(@Nullable final Uri uri) {
        if (uri == null) return UriMatcher.NO_MATCH;
        return HOME_TABS_URI_MATCHER.match(uri);
    }


    public static String matchTabType(@Nullable final Uri uri) {
        return getTabType(matchTabCode(uri));
    }

    public static String getTabType(final int code) {
        switch (code) {
            case TAB_CODE_HOME_TIMELINE: {
                return CustomTabType.HOME_TIMELINE;
            }
            case TAB_CODE_NOTIFICATIONS_TIMELINE: {
                return CustomTabType.NOTIFICATIONS_TIMELINE;
            }
            case TAB_CODE_DIRECT_MESSAGES: {
                return CustomTabType.DIRECT_MESSAGES;
            }
        }
        return null;
    }

    public static void openMessageConversation(final Context context, final long accountId,
                                               final long recipientId) {
        if (context == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_DIRECT_MESSAGES_CONVERSATION);
        if (accountId > 0 && recipientId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
            builder.appendQueryParameter(QUERY_PARAM_RECIPIENT_ID, String.valueOf(recipientId));
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }


    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static <T extends Parcelable> T[] newParcelableArray(Parcelable[] array, Parcelable.Creator<T> creator) {
        if (array == null) return null;
        final T[] result = creator.newArray(array.length);
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    public static void openIncomingFriendships(final Context context, final long accountId) {
        if (context == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_INCOMING_FRIENDSHIPS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openMap(final Context context, final double latitude, final double longitude) {
        if (context == null || !ParcelableLocation.isValidLocation(latitude, longitude)) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_MAP);
        builder.appendQueryParameter(QUERY_PARAM_LAT, String.valueOf(latitude));
        builder.appendQueryParameter(QUERY_PARAM_LNG, String.valueOf(longitude));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openMutesUsers(final Activity activity, final long accountId) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_MUTES_USERS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        activity.startActivity(intent);
    }

    public static void openScheduledStatuses(final Activity activity, final long accountId) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_SCHEDULED_STATUSES);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        activity.startActivity(intent);
    }

    public static void openSavedSearches(final Activity activity, final long account_id) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_SAVED_SEARCHES);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(account_id));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        activity.startActivity(intent);
    }

    public static void openSearch(final Context context, final long accountId, final String query) {
        openSearch(context, accountId, query, null);
    }

    public static void openSearch(final Context context, final long accountId, final String query, String type) {
        if (context == null) return;
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        // Some devices cannot process query parameter with hashes well, so add this intent extra
        intent.putExtra(EXTRA_QUERY, query);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);

        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_SEARCH);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        builder.appendQueryParameter(QUERY_PARAM_QUERY, query);
        if (!TextUtils.isEmpty(type)) {
            builder.appendQueryParameter(QUERY_PARAM_TYPE, type);
            intent.putExtra(EXTRA_TYPE, type);
        }
        intent.setData(builder.build());

        context.startActivity(intent);
    }

    public static void openStatus(final Context context, final long accountId, final long statusId) {
        if (context == null || accountId <= 0 || statusId <= 0) return;
        final Uri uri = LinkCreator.getTwidereStatusLink(accountId, statusId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    public static void openStatus(final Context context, final ParcelableStatus status, Bundle activityOptions) {
        if (context == null || status == null) return;
        final long account_id = status.account_id, status_id = status.id;
        final Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_STATUS, status);
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_STATUS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(account_id));
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, String.valueOf(status_id));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setExtrasClassLoader(context.getClassLoader());
        intent.putExtras(extras);
        if (context instanceof Activity) {
            ActivityCompat.startActivity((Activity) context, intent, activityOptions);
        } else {
            context.startActivity(intent);
        }
    }

    public static void openStatuses(final Context context, final List<ParcelableStatus> statuses) {
        if (context == null || statuses == null) return;
        final Bundle extras = new Bundle();
        extras.putParcelableArrayList(EXTRA_STATUSES, new ArrayList<>(statuses));
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_STATUSES);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    public static void openStatusFavoriters(final Context context, final long accountId, final long statusId) {
        if (context == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_STATUS_FAVORITERS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, String.valueOf(statusId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openStatusReplies(final Context context, final long accountId, final long statusId,
                                         final String screenName) {
        if (context == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_STATUS_REPLIES);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, String.valueOf(statusId));
        builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openStatusRetweeters(final Context context, final long accountId, final long statusId) {
        if (context == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_STATUS_RETWEETERS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, String.valueOf(statusId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openTweetSearch(final Context context, final long accountId, final String query) {
        openSearch(context, accountId, query, QUERY_PARAM_VALUE_TWEETS);
    }

    public static void openUserBlocks(final Activity activity, final long accountId) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_BLOCKS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserFavorites(final Activity activity, final long accountId,
                                         final long userId, final String screenName) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_FAVORITES);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        if (userId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);

    }

    public static void openUserFollowers(final Activity activity, final long account_id, final long user_id,
                                         final String screen_name) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_FOLLOWERS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(account_id));
        if (user_id > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(user_id));
        }
        if (screen_name != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screen_name);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserFriends(final Activity activity, final long account_id, final long user_id,
                                       final String screen_name) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_FRIENDS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(account_id));
        if (user_id > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(user_id));
        }
        if (screen_name != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screen_name);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);

    }

    public static void openUserListDetails(final Context context, final long accountId, final long listId,
                                           final long userId, final String screenName, final String listName) {
        if (context == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_LIST);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        if (listId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_ID, String.valueOf(listId));
        }
        if (userId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        if (listName != null) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, listName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openUserListDetails(final Activity activity, final ParcelableUserList userList) {
        if (activity == null || userList == null) return;
        final long accountId = userList.account_id, userId = userList.user_id;
        final long listId = userList.id;
        final Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_USER_LIST, userList);
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_LIST);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        builder.appendQueryParameter(QUERY_PARAM_LIST_ID, String.valueOf(listId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setExtrasClassLoader(activity.getClassLoader());
        intent.putExtras(extras);
        activity.startActivity(intent);
    }

    public static void openUserListMembers(final Activity activity, final long accountId, final long listId,
                                           final long userId, final String screenName, final String listName) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_LIST_MEMBERS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        if (listId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_ID, String.valueOf(listId));
        }
        if (userId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        if (listName != null) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, listName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserListMembers(final Activity activity, final ParcelableUserList list) {
        if (activity == null || list == null) return;
        openUserListMembers(activity, list.account_id, list.id, list.user_id, list.user_screen_name, list.name);
    }

    public static void openUserListMemberships(final Activity activity, final long account_id, final long user_id,
                                               final String screen_name) {
        if (activity == null || account_id <= 0 || user_id <= 0 && isEmpty(screen_name)) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_LIST_MEMBERSHIPS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(account_id));
        if (user_id > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(user_id));
        }
        if (screen_name != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screen_name);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserLists(final Activity activity, final long account_id, final long user_id,
                                     final String screen_name) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_LISTS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(account_id));
        if (user_id > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(user_id));
        }
        if (screen_name != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screen_name);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserListSubscribers(final Activity activity, final long accountId, final long listId,
                                               final long userId, final String screenName, final String listName) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_LIST_SUBSCRIBERS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        if (listId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_ID, String.valueOf(listId));
        }
        if (userId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        if (listName != null) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, listName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserListSubscribers(final Activity activity, final ParcelableUserList list) {
        if (activity == null || list == null) return;
        openUserListSubscribers(activity, list.account_id, list.id, list.user_id, list.user_screen_name, list.name);
    }

    public static void openUserListTimeline(final Activity activity, final long accountId, final long listId,
                                            final long userId, final String screenName, final String listName) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_LIST_TIMELINE);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        if (listId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_ID, String.valueOf(listId));
        }
        if (userId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        if (listName != null) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, listName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserListTimeline(final Activity activity, final ParcelableUserList list) {
        if (activity == null || list == null) return;
        openUserListTimeline(activity, list.account_id, list.id, list.user_id, list.user_screen_name, list.name);
    }

    public static boolean setNdefPushMessageCallback(Activity activity, CreateNdefMessageCallback callback) {
        try {
            final NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
            if (adapter == null) return false;
            adapter.setNdefPushMessageCallback(callback, activity);
            return true;
        } catch (SecurityException e) {
            Log.w(LOGTAG, e);
        }
        return false;
    }

    public static int getInsetsTopWithoutActionBarHeight(Context context, int top) {
        final int actionBarHeight;
        if (context instanceof AppCompatActivity) {
            actionBarHeight = getActionBarHeight(((AppCompatActivity) context).getSupportActionBar());
        } else if (context instanceof Activity) {
            actionBarHeight = getActionBarHeight(((Activity) context).getActionBar());
        } else {
            return top;
        }
        if (actionBarHeight > top) {
            return top;
        }
        return top - actionBarHeight;
    }

    public static int getInsetsTopWithoutActionBarHeight(Context context, int top, int actionBarHeight) {
        if (actionBarHeight > top) {
            return top;
        }
        return top - actionBarHeight;
    }

    public static void openUserMediaTimeline(final Activity activity, final long account_id, final long user_id,
                                             final String screen_name) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_USER_MEDIA_TIMELINE);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(account_id));
        if (user_id > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(user_id));
        }
        if (screen_name != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screen_name);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static String replaceLast(final String text, final String regex, final String replacement) {
        if (text == null || regex == null || replacement == null) return text;
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }

    public static void restartActivity(final Activity activity) {
        if (activity == null) return;
        final int enterAnim = android.R.anim.fade_in;
        final int exitAnim = android.R.anim.fade_out;
        activity.finish();
        activity.overridePendingTransition(enterAnim, exitAnim);
        activity.startActivity(activity.getIntent());
        activity.overridePendingTransition(enterAnim, exitAnim);
    }

    public static void scrollListToPosition(final AbsListView list, final int position) {
        scrollListToPosition(list, position, 0);
    }

    public static void scrollListToPosition(final AbsListView absListView, final int position, final int offset) {
        if (absListView == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            if (absListView instanceof ListView) {
                final ListView listView = ((ListView) absListView);
                listView.setSelectionFromTop(position, offset);
            } else {
                absListView.setSelection(position);
            }
            stopListView(absListView);
        } else {
            stopListView(absListView);
            if (absListView instanceof ListView) {
                final ListView listView = ((ListView) absListView);
                listView.setSelectionFromTop(position, offset);
            } else {
                absListView.setSelection(position);
            }
        }
    }

    public static void scrollListToTop(final AbsListView list) {
        if (list == null) return;
        scrollListToPosition(list, 0);
    }

    public static void setMenuForStatus(final Context context, final SharedPreferencesWrapper preferences,
                                        final Menu menu, final ParcelableStatus status,
                                        final AsyncTwitterWrapper twitter) {
        if (status == null) return;
        final ParcelableCredentials account = DataStoreUtils.getCredentials(context, status.account_id);
        setMenuForStatus(context, preferences, menu, status, account, twitter);
    }

    public static void setMenuForStatus(final Context context, final SharedPreferencesWrapper preferences,
                                        final Menu menu, final ParcelableStatus status,
                                        final ParcelableCredentials account, final AsyncTwitterWrapper twitter) {
        if (context == null || menu == null || status == null || account == null) return;
        final Resources resources = context.getResources();
        final int retweetHighlight = ContextCompat.getColor(context, R.color.highlight_retweet);
        final int favoriteHighlight = ContextCompat.getColor(context, R.color.highlight_favorite);
        final int likeHighlight = ContextCompat.getColor(context, R.color.highlight_like);
        final boolean isMyRetweet;
        if (twitter.isCreatingRetweet(status.account_id, status.id)) {
            isMyRetweet = true;
        } else if (twitter.isDestroyingStatus(status.account_id, status.id)) {
            isMyRetweet = false;
        } else {
            isMyRetweet = status.retweeted || isMyRetweet(status);
        }
        final MenuItem delete = menu.findItem(R.id.delete);
        if (delete != null) {
            delete.setVisible(isMyStatus(status));
        }
        final MenuItem retweet = menu.findItem(R.id.retweet);
        if (retweet != null) {
            ActionIconDrawable.setMenuHighlight(retweet, new TwidereMenuInfo(isMyRetweet, retweetHighlight));
            retweet.setTitle(isMyRetweet ? R.string.cancel_retweet : R.string.retweet);
        }
        final MenuItem favorite = menu.findItem(R.id.favorite);
        if (favorite != null) {
            final boolean isFavorite;
            if (twitter.isCreatingFavorite(status.account_id, status.id)) {
                isFavorite = true;
            } else if (twitter.isDestroyingFavorite(status.account_id, status.id)) {
                isFavorite = false;
            } else {
                isFavorite = status.is_favorite;
            }
            ActionProvider provider = MenuItemCompat.getActionProvider(favorite);
            final boolean useStar = preferences.getBoolean(KEY_I_WANT_MY_STARS_BACK);
            if (provider instanceof FavoriteItemProvider) {


            } else {
                if (useStar) {
                    final Drawable oldIcon = favorite.getIcon();
                    if (oldIcon instanceof ActionIconDrawable) {
                        final Drawable starIcon = ContextCompat.getDrawable(context, R.drawable.ic_action_star);
                        favorite.setIcon(new ActionIconDrawable(starIcon, ((ActionIconDrawable) oldIcon).getDefaultColor()));
                    } else {
                        favorite.setIcon(R.drawable.ic_action_star);
                    }
                    ActionIconDrawable.setMenuHighlight(favorite, new TwidereMenuInfo(isFavorite, favoriteHighlight));
                } else {
                    ActionIconDrawable.setMenuHighlight(favorite, new TwidereMenuInfo(isFavorite, likeHighlight));
                }
            }
            if (useStar) {
                favorite.setTitle(isFavorite ? R.string.unfavorite : R.string.favorite);
            } else {
                favorite.setTitle(isFavorite ? R.string.undo_like : R.string.like);
            }
        }
        final MenuItem translate = menu.findItem(R.id.translate);
        if (translate != null) {
            final boolean isOfficialKey = isOfficialCredentials(context, account);
            final SharedPreferencesWrapper prefs = SharedPreferencesWrapper.getInstance(context, SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            final boolean forcePrivateApis = prefs.getBoolean(KEY_FORCE_USING_PRIVATE_APIS, false);
            MenuUtils.setMenuItemAvailability(menu, R.id.translate, forcePrivateApis || isOfficialKey);
        }
        menu.removeGroup(MENU_GROUP_STATUS_EXTENSION);
        addIntentToMenuForExtension(context, menu, MENU_GROUP_STATUS_EXTENSION, INTENT_ACTION_EXTENSION_OPEN_STATUS,
                EXTRA_STATUS, EXTRA_STATUS_JSON, status);
        final MenuItem shareItem = menu.findItem(R.id.share);
        final ActionProvider shareProvider = MenuItemCompat.getActionProvider(shareItem);
        if (shareProvider instanceof SupportStatusShareProvider) {
            ((SupportStatusShareProvider) shareProvider).setStatus(status);
        } else if (shareProvider instanceof ShareActionProvider) {
            final Intent shareIntent = createStatusShareIntent(context, status);
            ((ShareActionProvider) shareProvider).setShareIntent(shareIntent);
        } else if (shareItem.hasSubMenu()) {
            final Menu shareSubMenu = shareItem.getSubMenu();
            final Intent shareIntent = createStatusShareIntent(context, status);
            shareSubMenu.removeGroup(MENU_GROUP_STATUS_SHARE);
            MenuUtils.addIntentToMenu(context, shareSubMenu, shareIntent, MENU_GROUP_STATUS_SHARE);
        } else {
            final Intent shareIntent = createStatusShareIntent(context, status);
            final Intent chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_status));
            addCopyLinkIntent(context, chooserIntent, LinkCreator.getTwitterStatusLink(status));
            shareItem.setIntent(chooserIntent);
        }

    }

    public static void addCopyLinkIntent(Context context, Intent chooserIntent, Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        final Intent copyLinkIntent = new Intent(context, CopyLinkActivity.class);
        copyLinkIntent.setData(uri);
        final Intent[] alternateIntents = {copyLinkIntent};
        chooserIntent.putExtra(Intent.EXTRA_ALTERNATE_INTENTS, alternateIntents);
    }

    private static boolean isMyStatus(ParcelableStatus status) {
        if (isMyRetweet(status)) return true;
        return status.account_id == status.user_id;
    }

    public static boolean shouldForceUsingPrivateAPIs(final Context context) {
        if (context == null) return false;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FORCE_USING_PRIVATE_APIS, false);
    }

    public static boolean shouldStopAutoRefreshOnBatteryLow(final Context context) {
        final SharedPreferences mPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return mPreferences.getBoolean(KEY_STOP_AUTO_REFRESH_WHEN_BATTERY_LOW, true);
    }

    public static void showErrorMessage(final Context context, final CharSequence message, final boolean longMessage) {
        if (context == null) return;
        final Toast toast = Toast.makeText(context, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showErrorMessage(final Context context, final CharSequence action,
                                        final CharSequence message, final boolean longMessage) {
        if (context == null) return;
        showErrorMessage(context, getErrorMessage(context, action, message), longMessage);
    }

    public static void showErrorMessage(final Context context, final CharSequence action,
                                        final Throwable t, final boolean longMessage) {
        if (context == null) return;
        if (t instanceof TwitterException) {
            showTwitterErrorMessage(context, action, (TwitterException) t, longMessage);
            return;
        }
        showErrorMessage(context, getErrorMessage(context, action, t), longMessage);
    }

    public static void showErrorMessage(final Context context, final int actionRes, final String desc,
                                        final boolean longMessage) {
        if (context == null) return;
        showErrorMessage(context, context.getString(actionRes), desc, longMessage);
    }

    public static void showErrorMessage(final Context context, final int action, final Throwable t,
                                        final boolean long_message) {
        if (context == null) return;
        showErrorMessage(context, context.getString(action), t, long_message);
    }

    public static void showInfoMessage(final Context context, final CharSequence message, final boolean long_message) {
        if (context == null || isEmpty(message)) return;
        final Toast toast = Toast.makeText(context, message, long_message ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showInfoMessage(final Context context, final int resId, final boolean long_message) {
        if (context == null) return;
        showInfoMessage(context, context.getText(resId), long_message);
    }

    public static void showMenuItemToast(final View v, final CharSequence text) {
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);
        final int height = v.getHeight();
        final int midy = screenPos[1] + height / 2;
        showMenuItemToast(v, text, midy >= displayFrame.height());
    }

    public static void showMenuItemToast(final View v, final CharSequence text, final boolean isBottomBar) {
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);
        final int width = v.getWidth();
        final int height = v.getHeight();
        final int screenWidth = v.getResources().getDisplayMetrics().widthPixels;
        final Toast cheatSheet = Toast.makeText(v.getContext().getApplicationContext(), text, Toast.LENGTH_SHORT);
        if (isBottomBar) {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
        } else {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP | GravityCompat.END, screenWidth - screenPos[0] - width / 2, height);
        }
        cheatSheet.show();
    }

    public static void showOkMessage(final Context context, final CharSequence message, final boolean longMessage) {
        if (context == null || isEmpty(message)) return;
        final Toast toast = Toast.makeText(context, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showOkMessage(final Context context, final int resId, final boolean long_message) {
        if (context == null) return;
        showOkMessage(context, context.getText(resId), long_message);
    }

    public static void showTwitterErrorMessage(final Context context, final CharSequence action,
                                               final TwitterException te, final boolean long_message) {
        if (context == null) return;
        final String message;
        if (te != null) {
            if (action != null) {
                final RateLimitStatus status = te.getRateLimitStatus();
                if (te.exceededRateLimitation() && status != null) {
                    final long secUntilReset = status.getSecondsUntilReset() * 1000;
                    final String nextResetTime = ParseUtils.parseString(getRelativeTimeSpanString(System
                            .currentTimeMillis() + secUntilReset));
                    message = context.getString(R.string.error_message_rate_limit_with_action, action,
                            nextResetTime.trim());
                } else if (isErrorCodeMessageSupported(te)) {
                    final String msg = StatusCodeMessageUtils
                            .getMessage(context, te.getStatusCode(), te.getErrorCode());
                    message = context.getString(R.string.error_message_with_action, action, msg != null ? msg
                            : trimLineBreak(te.getMessage()));
                } else if (te.getCause() instanceof SSLException) {
                    final String msg = te.getCause().getMessage();
                    if (msg != null && msg.contains("!=")) {
                        message = context.getString(R.string.error_message_with_action, action,
                                context.getString(R.string.ssl_error));
                    } else {
                        message = context.getString(R.string.error_message_with_action, action,
                                context.getString(R.string.network_error));
                    }
                } else if (te.getCause() instanceof IOException) {
                    message = context.getString(R.string.error_message_with_action, action,
                            context.getString(R.string.network_error));
                } else {
                    message = context.getString(R.string.error_message_with_action, action,
                            trimLineBreak(te.getMessage()));
                }
            } else {
                message = context.getString(R.string.error_message, trimLineBreak(te.getMessage()));
            }
        } else {
            message = context.getString(R.string.error_unknown_error);
        }
        showErrorMessage(context, message, long_message);
    }

    public static void showWarnMessage(final Context context, final CharSequence message, final boolean longMessage) {
        if (context == null || isEmpty(message)) return;
        final Toast toast = Toast.makeText(context, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showWarnMessage(final Context context, final int resId, final boolean long_message) {
        if (context == null) return;
        showWarnMessage(context, context.getText(resId), long_message);
    }

    public static void startRefreshServiceIfNeeded(@NonNull final Context context) {
        final Context appContext = context.getApplicationContext();
        if (appContext == null) return;
        final Intent refreshServiceIntent = new Intent(appContext, RefreshService.class);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable(appContext) && hasAutoRefreshAccounts(appContext)) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LOGTAG, "Start background refresh service");
                    }
                    appContext.startService(refreshServiceIntent);
                } else {
                    appContext.stopService(refreshServiceIntent);
                }
            }
        });
    }

    public static void startStatusShareChooser(final Context context, final ParcelableStatus status) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        final String name = status.user_name, screenName = status.user_screen_name;
        final String timeString = formatToLongTimeString(context, status.timestamp);
        final String subject = context.getString(R.string.status_share_subject_format_with_time, name, screenName, timeString);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, status.text_plain);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
    }

    public static void stopListView(final AbsListView list) {
        if (list == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_CANCEL, 0, 0, 0));
        } else {
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN, 0, 0, 0));
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP, 0, 0, 0));
        }
    }

    public static String trim(final String str) {
        return str != null ? str.trim() : null;
    }

    public static String trimLineBreak(final String orig) {
        if (orig == null) return null;
        return orig.replaceAll("\\n+", "\n");
    }

    public static boolean truncateMessages(final List<DirectMessage> in, final List<DirectMessage> out,
                                           final long sinceId) {
        if (in == null) return false;
        for (final DirectMessage message : in) {
            if (sinceId > 0 && message.getId() <= sinceId) {
                continue;
            }
            out.add(message);
        }
        return in.size() != out.size();
    }

    public static boolean truncateStatuses(final List<Status> in, final List<Status> out,
                                           final long sinceId) {
        if (in == null) return false;
        for (final Status status : in) {
            if (sinceId > 0 && status.getId() <= sinceId) {
                continue;
            }
            out.add(status);
        }
        return in.size() != out.size();
    }

    public static void updateRelationship(Context context, Relationship relationship, long accountId) {
        final ContentResolver resolver = context.getContentResolver();
        final ContentValues values = ContentValuesCreator.createCachedRelationship(relationship, accountId);
        resolver.insert(CachedRelationships.CONTENT_URI, values);
    }

    public static boolean useShareScreenshot() {
        return Boolean.parseBoolean("false");
    }

    private static Drawable getMetadataDrawable(final PackageManager pm, final ActivityInfo info, final String key) {
        if (pm == null || info == null || info.metaData == null || key == null || !info.metaData.containsKey(key))
            return null;
        return pm.getDrawable(info.packageName, info.metaData.getInt(key), info.applicationInfo);
    }

    public static boolean handleMenuItemClick(@NonNull final Context context,
                                              @Nullable final Fragment fragment,
                                              @NonNull final FragmentManager fm,
                                              @NonNull final UserColorNameManager colorNameManager,
                                              @NonNull final AsyncTwitterWrapper twitter,
                                              @NonNull final ParcelableStatus status,
                                              @NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.copy: {
                if (ClipboardUtils.setText(context, status.text_plain)) {
                    showOkMessage(context, R.string.text_copied, false);
                }
                break;
            }
            case R.id.retweet: {
                if (isMyRetweet(status)) {
                    twitter.cancelRetweetAsync(status.account_id, status.id, status.my_retweet_id);
                } else {
                    twitter.retweetStatusAsync(status.account_id, status.id);
                }
                break;
            }
            case R.id.quote: {
                final Intent intent = new Intent(INTENT_ACTION_QUOTE);
                intent.putExtra(EXTRA_STATUS, status);
                context.startActivity(intent);
                break;
            }
            case R.id.reply: {
                final Intent intent = new Intent(INTENT_ACTION_REPLY);
                intent.putExtra(EXTRA_STATUS, status);
                context.startActivity(intent);
                break;
            }
            case R.id.favorite: {
                if (status.is_favorite) {
                    twitter.destroyFavoriteAsync(status.account_id, status.id);
                } else {
                    ActionProvider provider = MenuItemCompat.getActionProvider(item);
                    if (provider instanceof FavoriteItemProvider) {
                        ((FavoriteItemProvider) provider).invokeItem(item,
                                new DefaultOnLikedListener(twitter, status));
                    } else {
                        twitter.createFavoriteAsync(status.account_id, status.id);
                    }
                }
                break;
            }
            case R.id.delete: {
                DestroyStatusDialogFragment.show(fm, status);
                break;
            }
            case R.id.add_to_filter: {
                AddStatusFilterDialogFragment.show(fm, status);
                break;
            }
            case R.id.set_color: {
                final Intent intent = new Intent(context, ColorPickerDialogActivity.class);
                final int color = colorNameManager.getUserColor(status.user_id, true);
                if (color != 0) {
                    intent.putExtra(EXTRA_COLOR, color);
                }
                intent.putExtra(EXTRA_CLEAR_BUTTON, color != 0);
                intent.putExtra(EXTRA_ALPHA_SLIDER, false);
                if (fragment != null) {
                    fragment.startActivityForResult(intent, REQUEST_SET_COLOR);
                } else if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, REQUEST_SET_COLOR);
                }
                break;
            }
            case R.id.clear_nickname: {
                colorNameManager.clearUserNickname(status.user_id);
                break;
            }
            case R.id.set_nickname: {
                final String nick = colorNameManager.getUserNickname(status.user_id, true);
                SetUserNicknameDialogFragment.show(fm, status.user_id, nick);
                break;
            }
            case R.id.open_with_account: {
                final Intent intent = new Intent(INTENT_ACTION_SELECT_ACCOUNT);
                intent.setClass(context, AccountSelectorActivity.class);
                intent.putExtra(EXTRA_SINGLE_SELECTION, true);
                if (fragment != null) {
                    fragment.startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
                } else if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
                }
                break;
            }
            case R.id.open_in_browser: {
                final Intent intent = new Intent(Intent.ACTION_VIEW, LinkCreator.getTwitterStatusLink(status));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
//                IntentSupport.setSelector(intent, new Intent(Intent.ACTION_VIEW).addCategory(IntentSupport.CATEGORY_APP_BROWSER));
                context.startActivity(intent);
                break;
            }
            default: {
                if (item.getIntent() != null) {
                    try {
                        context.startActivity(item.getIntent());
                    } catch (final ActivityNotFoundException e) {
                        Log.w(LOGTAG, e);
                        return false;
                    }
                }
                break;
            }
        }
        return true;
    }

    private static boolean isErrorCodeMessageSupported(final TwitterException te) {
        if (te == null) return false;
        return StatusCodeMessageUtils.containsHttpStatus(te.getStatusCode())
                || StatusCodeMessageUtils.containsTwitterError(te.getErrorCode());
    }

    private static boolean isExtensionUseJSON(final ResolveInfo info) {
        if (info == null || info.activityInfo == null) return false;
        final ActivityInfo activityInfo = info.activityInfo;
        if (activityInfo.metaData != null && activityInfo.metaData.containsKey(METADATA_KEY_EXTENSION_USE_JSON))
            return activityInfo.metaData.getBoolean(METADATA_KEY_EXTENSION_USE_JSON);
        final ApplicationInfo appInfo = activityInfo.applicationInfo;
        if (appInfo == null) return false;
        return appInfo.metaData != null && appInfo.metaData.getBoolean(METADATA_KEY_EXTENSION_USE_JSON, false);
    }

    public static int getActionBarHeight(@Nullable ActionBar actionBar) {
        if (actionBar == null) return 0;
        final Context context = actionBar.getThemedContext();
        final TypedValue tv = new TypedValue();
        final int height = actionBar.getHeight();
        if (height > 0) return height;
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    public static int getActionBarHeight(@Nullable android.support.v7.app.ActionBar actionBar) {
        if (actionBar == null) return 0;
        final Context context = actionBar.getThemedContext();
        final TypedValue tv = new TypedValue();
        final int height = actionBar.getHeight();
        if (height > 0) return height;
        if (context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    public static void makeListFragmentFitsSystemWindows(ListFragment fragment) {
        final FragmentActivity activity = fragment.getActivity();
        if (!(activity instanceof SystemWindowsInsetsCallback)) return;
        final SystemWindowsInsetsCallback callback = (SystemWindowsInsetsCallback) activity;
        final Rect insets = new Rect();
        if (callback.getSystemWindowsInsets(insets)) {
            makeListFragmentFitsSystemWindows(fragment, insets);
        }
    }


    public static void makeListFragmentFitsSystemWindows(ListFragment fragment, Rect insets) {
        final ListView listView = fragment.getListView();
        listView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        listView.setClipToPadding(false);
    }

    @Nullable
    public static ParcelableUser getUserForConversation(Context context, long accountId,
                                                        long conversationId) {
        final ContentResolver cr = context.getContentResolver();
        final Expression where = Expression.and(Expression.equals(ConversationEntries.ACCOUNT_ID, accountId),
                Expression.equals(ConversationEntries.CONVERSATION_ID, conversationId));
        final Cursor c = cr.query(ConversationEntries.CONTENT_URI, null, where.getSQL(), null, null);
        if (c == null) return null;
        try {
            if (c.moveToFirst()) return ParcelableUser.fromDirectMessageConversationEntry(c);
        } finally {
            c.close();
        }
        return null;
    }

    @SafeVarargs
    public static Bundle makeSceneTransitionOption(final Activity activity,
                                                   final Pair<View, String>... sharedElements) {
        if (ThemeUtils.isTransparentBackground(activity)) return null;
        return ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElements).toBundle();
    }


    public static void setSharedElementTransition(Context context, Window window, int transitionRes) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        UtilsL.setSharedElementTransition(context, window, transitionRes);
    }

    public static void openAccountsManager(Context context) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_ACCOUNTS);
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openDrafts(Context context) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_DRAFTS);
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openProfileEditor(Context context, long accountId) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_PROFILE_EDITOR);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, ParseUtils.parseString(accountId));
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openFilters(Context context) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWIDERE);
        builder.authority(AUTHORITY_FILTERS);
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static <T> Object findFieldOfTypes(T obj, Class<? extends T> cls, Class<?>... checkTypes) {
        labelField:
        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);
            final Object fieldObj;
            try {
                fieldObj = field.get(obj);
            } catch (Exception ignore) {
                continue;
            }
            if (fieldObj != null) {
                final Class<?> type = fieldObj.getClass();
                for (Class<?> checkType : checkTypes) {
                    if (!checkType.isAssignableFrom(type)) continue labelField;
                }
                return fieldObj;
            }
        }
        return null;
    }

    public static boolean isCustomConsumerKeySecret(String consumerKey, String consumerSecret) {
        if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) return false;
        return (!TWITTER_CONSUMER_KEY.equals(consumerKey) && !TWITTER_CONSUMER_SECRET.equals(consumerKey))
                && (!TWITTER_CONSUMER_KEY_LEGACY.equals(consumerKey) && !TWITTER_CONSUMER_SECRET_LEGACY.equals(consumerSecret));
    }

    public static boolean isStreamingEnabled() {
        return Boolean.parseBoolean("false");
    }

    public static int getErrorNo(Throwable t) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
        return UtilsL.getErrorNo(t);
    }

    public static boolean isOutOfMemory(Throwable ex) {
        if (ex == null) return false;
        final Throwable cause = ex.getCause();
        if (cause == null || cause == ex) return false;
        if (cause instanceof OutOfMemoryError) return true;
        return isOutOfMemory(cause);
    }

    public static void logOpenNotificationFromUri(Context context, Uri uri) {
        if (!uri.getBooleanQueryParameter(QUERY_PARAM_FROM_NOTIFICATION, false)) return;
        final String type = uri.getQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE);
        final long accountId = NumberUtils.toLong(uri.getQueryParameter(QUERY_PARAM_ACCOUNT_ID), -1);
        final long itemId = NumberUtils.toLong(UriExtraUtils.getExtra(uri, "item_id"), -1);
        final long itemUserId = NumberUtils.toLong(UriExtraUtils.getExtra(uri, "item_user_id"), -1);
        final boolean itemUserFollowing = Boolean.parseBoolean(UriExtraUtils.getExtra(uri, "item_user_following"));
        final long timestamp = NumberUtils.toLong(uri.getQueryParameter(QUERY_PARAM_TIMESTAMP), -1);
        if (!NotificationEvent.isSupported(type) || accountId < 0 || itemId < 0 || timestamp < 0)
            return;
        final NotificationEvent event = NotificationEvent.open(context, timestamp, type, accountId,
                itemId, itemUserId, itemUserFollowing);
        HotMobiLogger.getInstance(context).log(accountId, event);
    }

    public static boolean hasOfficialAPIAccess(Context context, SharedPreferences preferences, ParcelableCredentials account) {
        if (preferences.getBoolean(KEY_FORCE_USING_PRIVATE_APIS, false)) return true;
        return isOfficialCredentials(context, account);
    }

    public static int getNotificationId(int baseId, long accountId) {
        int result = baseId;
        result = 31 * result + (int) (accountId ^ (accountId >>> 32));
        return result;
    }

    @SuppressLint("InlinedApi")
    public static boolean isCharging(final Context context) {
        if (context == null) return false;
        final Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return false;
        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB
                || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    public static boolean shouldUsePrivateAPIs(Context context, long accountId) {
        if (shouldForceUsingPrivateAPIs(context)) return true;
        return TwitterAPIFactory.isOfficialKeyAccount(context, accountId);
    }

    public static boolean isMediaPreviewEnabled(Context context, SharedPreferencesWrapper preferences) {
        if (!preferences.getBoolean(KEY_MEDIA_PREVIEW)) return false;
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return !ConnectivityManagerCompat.isActiveNetworkMetered(cm) || !preferences.getBoolean(KEY_BANDWIDTH_SAVING_MODE);
    }

    static class UtilsL {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        static void setSharedElementTransition(Context context, Window window, int transitionRes) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            final TransitionInflater inflater = TransitionInflater.from(context);
            final Transition transition = inflater.inflateTransition(transitionRes);
            window.setSharedElementEnterTransition(transition);
            window.setSharedElementExitTransition(transition);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public static int getErrorNo(Throwable t) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
            if (t instanceof ErrnoException) {
                return ((ErrnoException) t).errno;
            }
            return 0;
        }
    }

    /**
     * Send Notifications to Pebble smartwatches
     *
     * @param context Context
     * @param message String
     */
    public static void sendPebbleNotification(final Context context, final String message) {
        if (context == null || TextUtils.isEmpty(message)) return;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_PEBBLE_NOTIFICATIONS, false)) {

            final String appName = context.getString(R.string.app_name);

            final List<PebbleMessage> messages = new ArrayList<>();
            messages.add(new PebbleMessage(appName, message));

            final Intent intent = new Intent(INTENT_ACTION_PEBBLE_NOTIFICATION);
            intent.putExtra("messageType", "PEBBLE_ALERT");
            intent.putExtra("sender", appName);
            intent.putExtra("notificationData", JsonSerializer.serialize(messages, PebbleMessage.class));

            context.getApplicationContext().sendBroadcast(intent);
        }
    }

    @Nullable
    public static GeoLocation getCachedGeoLocation(Context context) {
        final Location location = getCachedLocation(context);
        if (location == null) return null;
        return new GeoLocation(location.getLatitude(), location.getLongitude());
    }

    @Nullable
    public static Location getCachedLocation(Context context) {
        Location location = null;
        final LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return null;
        try {
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException ignore) {

        }
        if (location != null) return location;
        try {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignore) {

        }
        return location;
    }

}
