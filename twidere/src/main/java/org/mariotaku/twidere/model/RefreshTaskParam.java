package org.mariotaku.twidere.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by mariotaku on 16/2/14.
 */
public interface RefreshTaskParam {
    @NonNull
    long[] getAccountIds();

    @Nullable
    long[] getMaxIds();

    @Nullable
    long[] getSinceIds();
}
