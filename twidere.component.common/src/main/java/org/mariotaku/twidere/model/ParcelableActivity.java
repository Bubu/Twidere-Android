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

package org.mariotaku.twidere.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import org.mariotaku.library.objectcursor.annotation.CursorField;
import org.mariotaku.library.objectcursor.annotation.CursorObject;
import org.mariotaku.twidere.api.twitter.model.Activity;
import org.mariotaku.twidere.model.util.LoganSquareCursorFieldConverter;
import org.mariotaku.twidere.model.util.LongArrayConverter;
import org.mariotaku.twidere.provider.TwidereDataStore.Activities;

import java.util.Arrays;

@ParcelablePlease(allFields = false)
@JsonObject
@CursorObject(valuesCreator = true)
public class ParcelableActivity implements Comparable<ParcelableActivity>, Parcelable {

    public static final Creator<ParcelableActivity> CREATOR = new Creator<ParcelableActivity>() {
        public ParcelableActivity createFromParcel(Parcel source) {
            ParcelableActivity target = new ParcelableActivity();
            ParcelableActivityParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public ParcelableActivity[] newArray(int size) {
            return new ParcelableActivity[size];
        }
    };

    @ParcelableThisPlease
    @CursorField(value = Activities._ID, excludeWrite = true)
    public long _id;
    @ParcelableThisPlease
    @JsonField(name = "account_id")
    @CursorField(value = Activities.ACCOUNT_ID)
    public long account_id;
    @ParcelableThisPlease
    @JsonField(name = "timestamp")
    @CursorField(value = Activities.TIMESTAMP)
    public long timestamp;
    @ParcelableThisPlease
    @JsonField(name = "max_position")
    @CursorField(value = Activities.MAX_POSITION)
    public long max_position;
    @ParcelableThisPlease
    @JsonField(name = "min_position")
    @CursorField(value = Activities.MIN_POSITION)
    public long min_position;
    @ParcelableThisPlease
    @JsonField(name = "action")
    @CursorField(value = Activities.ACTION)
    public String action;

    @ParcelableThisPlease
    @JsonField(name = "source_ids")
    @CursorField(value = Activities.SOURCE_IDS, converter = LongArrayConverter.class)
    public long[] source_ids;

    @ParcelableThisPlease
    @JsonField(name = "sources")
    @CursorField(value = Activities.SOURCES, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableUser[] sources;
    @ParcelableThisPlease
    @JsonField(name = "target_users")
    @CursorField(value = Activities.TARGET_USERS, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableUser[] target_users;
    @ParcelableThisPlease
    @JsonField(name = "target_statuses")
    @CursorField(value = Activities.TARGET_STATUSES, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableStatus[] target_statuses;
    @ParcelableThisPlease
    @JsonField(name = "target_user_lists")
    @CursorField(value = Activities.TARGET_USER_LISTS, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableUserList[] target_user_lists;

    @ParcelableThisPlease
    @JsonField(name = "target_object_user_lists")
    @CursorField(value = Activities.TARGET_OBJECT_USER_LISTS, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableUserList[] target_object_user_lists;
    @ParcelableThisPlease
    @JsonField(name = "target_object_statuses")
    @CursorField(value = Activities.TARGET_OBJECT_STATUSES, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableStatus[] target_object_statuses;
    @ParcelableThisPlease
    @JsonField(name = "target_object_users")
    @CursorField(value = Activities.TARGET_OBJECT_USERS, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableUser[] target_object_users;
    @ParcelableThisPlease
    @JsonField(name = "is_gap")
    @CursorField(value = Activities.IS_GAP)
    public boolean is_gap;
    @ParcelableThisPlease
    @JsonField(name = "status_user_following")
    @CursorField(value = Activities.STATUS_USER_FOLLOWING, excludeWrite = true)
    public boolean status_user_following;
    public transient long[] after_filtered_source_ids;
    public transient ParcelableUser[] after_filtered_sources;


    public ParcelableActivity() {
    }

    @Nullable
    public static ParcelableStatus getActivityStatus(@NonNull ParcelableActivity activity) {
        if (Activity.Action.MENTION.equals(activity.action)) {
            return activity.target_object_statuses[0];
        } else if (Activity.Action.REPLY.equals(activity.action)) {
            return activity.target_statuses[0];
        } else if (Activity.Action.QUOTE.equals(activity.action)) {
            return activity.target_statuses[0];
        }
        return null;
    }

    public static int calculateHashCode(long account_id, long timestamp, long max_position, long min_position) {
        int result = (int) (account_id ^ (account_id >>> 32));
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (int) (max_position ^ (max_position >>> 32));
        result = 31 * result + (int) (min_position ^ (min_position >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ParcelableActivity{" +
                "account_id=" + account_id +
                ", timestamp=" + timestamp +
                ", max_position=" + max_position +
                ", min_position=" + min_position +
                ", action=" + action +
                ", sources=" + Arrays.toString(sources) +
                ", target_users=" + Arrays.toString(target_users) +
                ", target_statuses=" + Arrays.toString(target_statuses) +
                ", target_user_lists=" + Arrays.toString(target_user_lists) +
                ", target_object_user_lists=" + Arrays.toString(target_object_user_lists) +
                ", target_object_statuses=" + Arrays.toString(target_object_statuses) +
                ", target_object_users=" + Arrays.toString(target_object_users) +
                ", is_gap=" + is_gap +
                '}';
    }

    @Override
    public int hashCode() {
        return calculateHashCode(account_id, timestamp, max_position, min_position);
    }

    @Override
    public int compareTo(@NonNull final ParcelableActivity another) {
        final long delta = another.timestamp - timestamp;
        if (delta < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (delta > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) delta;
    }

    @Override
    public boolean equals(final Object that) {
        if (!(that instanceof ParcelableActivity)) return false;
        final ParcelableActivity activity = (ParcelableActivity) that;
        return max_position == activity.max_position && min_position == activity.min_position;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableActivityParcelablePlease.writeToParcel(this, dest, flags);
    }

}
