/*
 *                 Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import org.mariotaku.library.objectcursor.annotation.CursorField;
import org.mariotaku.library.objectcursor.annotation.CursorObject;
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts;

/**
 * Created by mariotaku on 15/5/26.
 */
@JsonObject
@CursorObject
@ParcelablePlease
public class ParcelableCredentials extends ParcelableAccount implements Parcelable {

    public static final Creator<ParcelableCredentials> CREATOR = new Creator<ParcelableCredentials>() {
        public ParcelableCredentials createFromParcel(Parcel source) {
            ParcelableCredentials target = new ParcelableCredentials();
            ParcelableCredentialsParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public ParcelableCredentials[] newArray(int size) {
            return new ParcelableCredentials[size];
        }
    };

    public static final int AUTH_TYPE_OAUTH = 0;
    public static final int AUTH_TYPE_XAUTH = 1;
    public static final int AUTH_TYPE_BASIC = 2;
    public static final int AUTH_TYPE_TWIP_O_MODE = 3;

    @ParcelableThisPlease
    @JsonField(name = "auth_type")
    @CursorField(Accounts.AUTH_TYPE)
    public int auth_type;
    @ParcelableThisPlease
    @JsonField(name = "consumer_key")
    @CursorField(Accounts.CONSUMER_KEY)
    public String consumer_key;
    @ParcelableThisPlease
    @JsonField(name = "consumer_secret")
    @CursorField(Accounts.CONSUMER_SECRET)
    public String consumer_secret;
    @ParcelableThisPlease
    @JsonField(name = "basic_auth_username")
    @CursorField(Accounts.BASIC_AUTH_USERNAME)
    public String basic_auth_username;
    @ParcelableThisPlease
    @JsonField(name = "basic_auth_password")
    @CursorField(Accounts.BASIC_AUTH_PASSWORD)
    public String basic_auth_password;
    @ParcelableThisPlease
    @JsonField(name = "oauth_token")
    @CursorField(Accounts.OAUTH_TOKEN)
    public String oauth_token;
    @ParcelableThisPlease
    @JsonField(name = "oauth_token_secret")
    @CursorField(Accounts.OAUTH_TOKEN_SECRET)
    public String oauth_token_secret;
    @ParcelableThisPlease
    @JsonField(name = "api_url_format")
    @CursorField(Accounts.API_URL_FORMAT)
    public String api_url_format;
    @ParcelableThisPlease
    @JsonField(name = "same_oauth_signing_url")
    @CursorField(Accounts.SAME_OAUTH_SIGNING_URL)
    public boolean same_oauth_signing_url;
    @ParcelableThisPlease
    @JsonField(name = "no_version_suffix")
    @CursorField(Accounts.NO_VERSION_SUFFIX)
    public boolean no_version_suffix;


    ParcelableCredentials() {
    }

    @Override
    public String toString() {
        return "ParcelableCredentials{" +
                "auth_type=" + auth_type +
                ", consumer_key='" + consumer_key + '\'' +
                ", consumer_secret='" + consumer_secret + '\'' +
                ", basic_auth_username='" + basic_auth_username + '\'' +
                ", basic_auth_password='" + basic_auth_password + '\'' +
                ", oauth_token='" + oauth_token + '\'' +
                ", oauth_token_secret='" + oauth_token_secret + '\'' +
                ", api_url_format='" + api_url_format + '\'' +
                ", same_oauth_signing_url=" + same_oauth_signing_url +
                ", no_version_suffix=" + no_version_suffix +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableCredentialsParcelablePlease.writeToParcel(this, dest, flags);
    }
}
