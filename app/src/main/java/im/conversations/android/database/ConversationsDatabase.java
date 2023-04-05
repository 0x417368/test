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

package im.conversations.android.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import im.conversations.android.database.dao.AccountDao;
import im.conversations.android.database.dao.ArchiveDao;
import im.conversations.android.database.dao.AvatarDao;
import im.conversations.android.database.dao.AxolotlDao;
import im.conversations.android.database.dao.BlockingDao;
import im.conversations.android.database.dao.BookmarkDao;
import im.conversations.android.database.dao.CertificateTrustDao;
import im.conversations.android.database.dao.ChatDao;
import im.conversations.android.database.dao.DiscoDao;
import im.conversations.android.database.dao.MessageDao;
import im.conversations.android.database.dao.NickDao;
import im.conversations.android.database.dao.PresenceDao;
import im.conversations.android.database.dao.RosterDao;
import im.conversations.android.database.dao.ServiceRecordDao;
import im.conversations.android.database.entity.AccountEntity;
import im.conversations.android.database.entity.ArchivePageEntity;
import im.conversations.android.database.entity.AvatarAdditionalEntity;
import im.conversations.android.database.entity.AvatarEntity;
import im.conversations.android.database.entity.AxolotlDeviceListEntity;
import im.conversations.android.database.entity.AxolotlDeviceListItemEntity;
import im.conversations.android.database.entity.AxolotlIdentityEntity;
import im.conversations.android.database.entity.AxolotlIdentityKeyPairEntity;
import im.conversations.android.database.entity.AxolotlPreKeyEntity;
import im.conversations.android.database.entity.AxolotlSessionEntity;
import im.conversations.android.database.entity.AxolotlSignedPreKeyEntity;
import im.conversations.android.database.entity.BlockedItemEntity;
import im.conversations.android.database.entity.BookmarkEntity;
import im.conversations.android.database.entity.BookmarkGroupEntity;
import im.conversations.android.database.entity.CertificateTrustEntity;
import im.conversations.android.database.entity.ChatEntity;
import im.conversations.android.database.entity.DiscoEntity;
import im.conversations.android.database.entity.DiscoExtensionEntity;
import im.conversations.android.database.entity.DiscoExtensionFieldEntity;
import im.conversations.android.database.entity.DiscoExtensionFieldValueEntity;
import im.conversations.android.database.entity.DiscoFeatureEntity;
import im.conversations.android.database.entity.DiscoIdentityEntity;
import im.conversations.android.database.entity.DiscoItemEntity;
import im.conversations.android.database.entity.GroupEntity;
import im.conversations.android.database.entity.MessageContentEntity;
import im.conversations.android.database.entity.MessageEntity;
import im.conversations.android.database.entity.MessageReactionEntity;
import im.conversations.android.database.entity.MessageStateEntity;
import im.conversations.android.database.entity.MessageVersionEntity;
import im.conversations.android.database.entity.MucStatusCodeEntity;
import im.conversations.android.database.entity.NickEntity;
import im.conversations.android.database.entity.PresenceEntity;
import im.conversations.android.database.entity.RosterItemEntity;
import im.conversations.android.database.entity.RosterItemGroupEntity;
import im.conversations.android.database.entity.ServiceRecordCacheEntity;

@Database(
        entities = {
            AccountEntity.class,
            ArchivePageEntity.class,
            AvatarAdditionalEntity.class,
            AvatarEntity.class,
            AxolotlDeviceListEntity.class,
            AxolotlDeviceListItemEntity.class,
            AxolotlIdentityEntity.class,
            AxolotlIdentityKeyPairEntity.class,
            AxolotlPreKeyEntity.class,
            AxolotlSessionEntity.class,
            AxolotlSignedPreKeyEntity.class,
            BlockedItemEntity.class,
            BookmarkEntity.class,
            BookmarkGroupEntity.class,
            CertificateTrustEntity.class,
            ChatEntity.class,
            DiscoEntity.class,
            DiscoExtensionEntity.class,
            DiscoExtensionFieldEntity.class,
            DiscoExtensionFieldValueEntity.class,
            DiscoFeatureEntity.class,
            DiscoIdentityEntity.class,
            DiscoItemEntity.class,
            GroupEntity.class,
            MessageEntity.class,
            MessageStateEntity.class,
            MessageContentEntity.class,
            MessageVersionEntity.class,
            MucStatusCodeEntity.class,
            NickEntity.class,
            PresenceEntity.class,
            MessageReactionEntity.class,
            RosterItemEntity.class,
            RosterItemGroupEntity.class,
            ServiceRecordCacheEntity.class
        },
        version = 1)
@TypeConverters(Converters.class)
public abstract class ConversationsDatabase extends RoomDatabase {

    private static volatile ConversationsDatabase INSTANCE = null;

    public static ConversationsDatabase getInstance(final Context context) {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (ConversationsDatabase.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            final Context application = context.getApplicationContext();
            INSTANCE =
                    Room.databaseBuilder(application, ConversationsDatabase.class, "conversations")
                            .build();
            return INSTANCE;
        }
    }

    public abstract AccountDao accountDao();

    public abstract ArchiveDao archiveDao();

    public abstract AvatarDao avatarDao();

    public abstract AxolotlDao axolotlDao();

    public abstract BlockingDao blockingDao();

    public abstract BookmarkDao bookmarkDao();

    public abstract CertificateTrustDao certificateTrustDao();

    public abstract ChatDao chatDao();

    public abstract DiscoDao discoDao();

    public abstract MessageDao messageDao();

    public abstract NickDao nickDao();

    public abstract PresenceDao presenceDao();

    public abstract RosterDao rosterDao();

    public abstract ServiceRecordDao serviceRecordDao();
}
