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

package org.mariotaku.twidere.loader.support;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.mariotaku.twidere.api.twitter.Twitter;
import org.mariotaku.twidere.api.twitter.TwitterException;
import org.mariotaku.twidere.api.twitter.model.Paging;
import org.mariotaku.twidere.api.twitter.model.ResponseList;
import org.mariotaku.twidere.api.twitter.model.Status;
import org.mariotaku.twidere.model.ParcelableStatus;
import org.mariotaku.twidere.util.DataStoreUtils;
import org.mariotaku.twidere.util.TwitterContentUtils;

import java.util.List;

public class UserTimelineLoader extends TwitterAPIStatusesLoader {

    private final long mUserId;
    private final String mUserScreenName;
    private final boolean mIsMyTimeline;

    public UserTimelineLoader(final Context context, final long accountId, final long userId,
                              final String screenName, final long sinceId, final long maxId,
                              final List<ParcelableStatus> data, final String[] savedStatusesArgs,
                              final int tabPosition, boolean fromUser) {
        super(context, accountId, sinceId, maxId, data, savedStatusesArgs, tabPosition, fromUser);
        mUserId = userId;
        mUserScreenName = screenName;
        mIsMyTimeline = userId > 0 ? accountId == userId : accountId == DataStoreUtils.getAccountId(context, screenName);
    }

    @NonNull
    @Override
    protected ResponseList<Status> getStatuses(@NonNull final Twitter twitter, final Paging paging) throws TwitterException {
        if (mUserId != -1)
            return twitter.getUserTimeline(mUserId, paging);
        else if (mUserScreenName != null)
            return twitter.getUserTimeline(mUserScreenName, paging);
        else
            throw new TwitterException("Invalid user");
    }

    @Override
    protected boolean shouldFilterStatus(final SQLiteDatabase database, final ParcelableStatus status) {
        final long retweetUserId = status.is_retweet ? status.user_id : -1;
        return !mIsMyTimeline && TwitterContentUtils.isFiltered(database, retweetUserId, status.text_plain,
                status.text_html, status.source, -1, status.quoted_user_id);
    }
}
