/*
 * Copyright (c) 2023, Daniel Gultsch
 *
 * This file is part of Conversations.
 *
 * Conversations is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Conversations is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Conversations.  If not, see <https://www.gnu.org/licenses/>.
 */

package im.conversations.android.database.model;

import androidx.annotation.NonNull;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteSource;
import com.google.common.primitives.Ints;
import im.conversations.android.IDs;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.parts.Resourcepart;

public final class Account {

    public final long id;
    @NonNull public final BareJid address;

    @NonNull public final byte[] randomSeed;

    public Account(final long id, @NonNull final BareJid address, @NonNull byte[] randomSeed) {
        Preconditions.checkNotNull(address, "Account can not be instantiated without an address");
        Preconditions.checkArgument(
                randomSeed.length == 32, "RandomSeed must have exactly 32 bytes");
        this.id = id;
        this.address = address;
        this.randomSeed = randomSeed;
    }

    public boolean isOnion() {
        final String domain = address.getDomain().toString();
        return domain.endsWith(".onion");
    }

    public UUID getPublicDeviceId() {
        try {
            return IDs.uuid(
                    ByteSource.wrap(randomSeed).slice(0, 16).hash(Hashing.sha256()).asBytes());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPublicDeviceIdInt() {
        try {
            return Math.abs(Ints.fromByteArray(ByteSource.wrap(randomSeed).slice(0, 4).read()));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Resourcepart fallbackNick() {
        final var localPart = address.getLocalpartOrNull();
        if (localPart != null) {
            final var resourceFromLocalPart = Resourcepart.fromOrNull(localPart.toString());
            if (resourceFromLocalPart != null) {
                return resourceFromLocalPart;
            }
        }
        try {
            return Resourcepart.fromOrThrowUnchecked(
                    BaseEncoding.base32Hex()
                            .lowerCase()
                            .omitPadding()
                            .encode(ByteSource.wrap(randomSeed).slice(0, 6).read()));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id == account.id
                && address.equals(account.address)
                && Arrays.equals(randomSeed, account.randomSeed);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, address);
        result = 31 * result + Arrays.hashCode(randomSeed);
        return result;
    }
}
