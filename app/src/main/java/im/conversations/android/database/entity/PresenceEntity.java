package im.conversations.android.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import im.conversations.android.database.model.PresenceShow;
import im.conversations.android.database.model.PresenceType;
import im.conversations.android.xmpp.model.muc.Affiliation;
import im.conversations.android.xmpp.model.muc.Role;
import im.conversations.android.xmpp.model.muc.user.MucUser;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

@Entity(
        tableName = "presence",
        foreignKeys = {
            @ForeignKey(
                    entity = AccountEntity.class,
                    parentColumns = {"id"},
                    childColumns = {"accountId"},
                    onDelete = ForeignKey.CASCADE),
            @ForeignKey(
                    entity = DiscoEntity.class,
                    parentColumns = {"id"},
                    childColumns = {"discoId"},
                    onDelete = ForeignKey.CASCADE)
        },
        indices = {
            @Index(
                    value = {"accountId", "address", "resource"},
                    unique = true),
            @Index(value = {"discoId"})
        })
public class PresenceEntity {

    @PrimaryKey(autoGenerate = true)
    public Long id;

    @NonNull public Long accountId;

    @NonNull public BareJid address;

    @NonNull public Resourcepart resource;

    @Nullable public PresenceType type;

    @Nullable public PresenceShow show;

    @Nullable public String status;

    @Nullable public String vCardPhoto;

    @Nullable public String occupantId;

    @Nullable public Affiliation mucUserAffiliation;

    @Nullable public Role mucUserRole;

    @Nullable public Jid mucUserJid;

    // set to true if presence has status code 110 (this means we are online)
    public boolean mucUserSelf;

    public Long discoId;

    public static PresenceEntity of(
            long account,
            @NonNull BareJid address,
            @NonNull Resourcepart resource,
            final PresenceType type,
            final PresenceShow show,
            final String status,
            final String vCardPhoto,
            final String occupantId,
            final MucUser mucUser) {
        final var mucUserItem = mucUser == null ? null : mucUser.getItem();
        final var entity = new PresenceEntity();
        entity.accountId = account;
        entity.address = address;
        entity.resource = resource;
        entity.type = type;
        entity.show = show;
        entity.status = status;
        entity.vCardPhoto = vCardPhoto;
        if (mucUserItem != null) {
            entity.occupantId = occupantId;
            entity.mucUserAffiliation = mucUserItem.getAffiliation();
            entity.mucUserRole = mucUserItem.getRole();
            entity.mucUserJid = mucUserItem.getJid();
            entity.mucUserSelf = mucUser.getStatus().contains(110);
        }
        return entity;
    }
}