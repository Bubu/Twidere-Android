/*
 * Twidere - Twitter client for Android
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

package org.mariotaku.twidere.fragment.support;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;

import com.squareup.otto.Subscribe;

import org.mariotaku.twidere.adapter.ListParcelableStatusesAdapter;
import org.mariotaku.twidere.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition;
import org.mariotaku.twidere.adapter.iface.IStatusesAdapter;
import org.mariotaku.twidere.model.ParcelableStatus;
import org.mariotaku.twidere.model.message.FavoriteTaskEvent;
import org.mariotaku.twidere.model.message.StatusDestroyedEvent;
import org.mariotaku.twidere.model.message.StatusListChangedEvent;
import org.mariotaku.twidere.model.message.StatusRetweetedEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by mariotaku on 14/12/3.
 */
public abstract class ParcelableStatusesFragment extends AbsStatusesFragment<List<ParcelableStatus>> {

    private long mLastId;

    public final void deleteStatus(final long statusId) {
        final List<ParcelableStatus> list = getAdapterData();
        if (statusId <= 0 || list == null) return;
        final Set<ParcelableStatus> dataToRemove = new HashSet<>();
        for (int i = 0, j = list.size(); i < j; i++) {
            final ParcelableStatus status = list.get(i);
            if (status.id == statusId || status.retweet_id > 0 && status.retweet_id == statusId) {
                dataToRemove.add(status);
            } else if (status.my_retweet_id == statusId) {
                status.my_retweet_id = -1;
                status.retweet_count = status.retweet_count - 1;
            }
        }
        list.removeAll(dataToRemove);
        setAdapterData(list);
    }

    @Override
    public boolean getStatuses(long[] accountIds, final long[] maxIds, final long[] sinceIds) {
        final Bundle args = new Bundle(getArguments());
        if (maxIds != null) {
            args.putLong(EXTRA_MAX_ID, maxIds[0]);
            args.putBoolean(EXTRA_MAKE_GAP, false);
        }
        if (sinceIds != null) {
            args.putLong(EXTRA_SINCE_ID, sinceIds[0]);
        }
        args.putBoolean(EXTRA_FROM_USER, true);
        getLoaderManager().restartLoader(0, args, this);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        mBus.register(this);
    }

    @Override
    public void onStop() {
        mBus.unregister(this);
        super.onStop();
    }

    @Override
    protected boolean hasMoreData(List<ParcelableStatus> list) {
        if (list == null || list.isEmpty()) return false;
        return (mLastId != (mLastId = list.get(list.size() - 1).id));
    }

    @Override
    protected long[] getAccountIds() {
        return new long[]{getAccountId()};
    }

    @Override
    protected Object createMessageBusCallback() {
        return new ParcelableStatusesBusCallback();
    }

    @NonNull
    @Override
    protected ListParcelableStatusesAdapter onCreateAdapter(final Context context, final boolean compact) {
        return new ListParcelableStatusesAdapter(context, compact);
    }

    @Override
    protected void onLoadingFinished() {
        showContent();
        setRefreshEnabled(true);
        setRefreshing(false);
        setLoadMoreIndicatorPosition(IndicatorPosition.NONE);
    }


    @Override
    public void onLoadMoreContents(int position) {
        // Only supports load from end, skip START flag
        if ((position & IndicatorPosition.START) != 0) return;
        super.onLoadMoreContents(position);
        if (position == 0) return;
        final IStatusesAdapter<List<ParcelableStatus>> adapter = getAdapter();
        final long[] maxIds = new long[]{adapter.getStatusId(adapter.getStatusCount() - 1)};
        getStatuses(null, maxIds, null);
    }

    public final void replaceStatus(final ParcelableStatus status) {
        final List<ParcelableStatus> data = getAdapterData();
        if (status == null || data == null || data.isEmpty()) return;
        for (int i = 0, j = data.size(); i < j; i++) {
            if (status.equals(data.get(i))) {
                data.set(i, status);
            }
        }
        setAdapterData(data);
    }

    @Override
    public boolean triggerRefresh() {
        super.triggerRefresh();
        final IStatusesAdapter<List<ParcelableStatus>> adapter = getAdapter();
        final long[] accountIds = getAccountIds();
        if (adapter.getStatusCount() > 0) {
            final long[] sinceIds = new long[]{adapter.getStatus(0).id};
            getStatuses(accountIds, null, sinceIds);
        } else {
            getStatuses(accountIds, null, null);
        }
        return true;
    }

    protected long getAccountId() {
        final Bundle args = getArguments();
        return args != null ? args.getLong(EXTRA_ACCOUNT_ID, -1) : -1;
    }

    @Override
    public boolean isRefreshing() {
        final LoaderManager lm = getLoaderManager();
        return lm.hasRunningLoaders();
    }

    protected String[] getSavedStatusesFileArgs() {
        return null;
    }

    private void updateFavoritedStatus(ParcelableStatus status) {
        final Context context = getActivity();
        if (context == null) return;
        if (status.account_id == getAccountId()) {
            replaceStatus(status);
        }
    }

    private void updateRetweetedStatuses(ParcelableStatus status) {
        final List<ParcelableStatus> data = getAdapterData();
        if (status == null || status.retweet_id <= 0 || data == null) return;
        for (int i = 0, j = data.size(); i < j; i++) {
            final ParcelableStatus orig = data.get(i);
            if (orig.account_id == status.account_id && orig.id == status.retweet_id) {
                orig.my_retweet_id = status.my_retweet_id;
                orig.retweet_count = status.retweet_count;
            }
        }
        setAdapterData(data);
    }

    protected class ParcelableStatusesBusCallback {

        @Subscribe
        public void notifyFavoriteTask(FavoriteTaskEvent event) {
            if (event.isSucceeded()) {
                updateFavoritedStatus(event.getStatus());
            }
        }


        @Subscribe
        public void notifyStatusDestroyed(StatusDestroyedEvent event) {
            deleteStatus(event.status.id);
        }

        @Subscribe
        public void notifyStatusListChanged(StatusListChangedEvent event) {
            getAdapter().notifyDataSetChanged();
        }

        @Subscribe
        public void notifyStatusRetweeted(StatusRetweetedEvent event) {
            updateRetweetedStatuses(event.status);
        }

    }

}
