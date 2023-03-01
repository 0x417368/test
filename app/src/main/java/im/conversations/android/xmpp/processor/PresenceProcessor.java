package im.conversations.android.xmpp.processor;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import im.conversations.android.database.model.PresenceShow;
import im.conversations.android.database.model.PresenceType;
import im.conversations.android.xmpp.Entity;
import im.conversations.android.xmpp.XmppConnection;
import im.conversations.android.xmpp.manager.DiscoManager;
import im.conversations.android.xmpp.model.stanza.Presence;
import java.util.function.Consumer;

public class PresenceProcessor extends XmppConnection.Delegate implements Consumer<Presence> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PresenceProcessor.class);

    public PresenceProcessor(final Context context, final XmppConnection connection) {
        super(context, connection);
    }

    @Override
    public void accept(final Presence presencePacket) {
        final var from = presencePacket.getFrom();
        final var address = from == null ? null : from.asBareJid();
        if (address == null) {
            LOGGER.warn("Received presence from account (from=null). This is unusual.");
            return;
        }
        final var resource = from.getResourceOrEmpty();
        final var typeAttribute = presencePacket.getAttribute("type");
        final PresenceType type;
        try {
            type = PresenceType.of(typeAttribute);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Received presence of type '{}' from {}", typeAttribute, from);
            return;
        }
        final var show = PresenceShow.of(presencePacket.findChildContent("show"));
        final var status = presencePacket.findChildContent("status");
        getDatabase().presenceDao().set(getAccount(), address, resource, type, show, status);

        // TODO store presence info (vCard + muc#user stuff + occupantId)

        // TODO do this only for contacts?
        fetchCapabilities(presencePacket);
    }

    private void fetchCapabilities(final Presence presencePacket) {
        final var entity = presencePacket.getFrom();
        final var nodeHash = presencePacket.getCapabilities();
        if (nodeHash != null) {
            getManager(DiscoManager.class)
                    .infoOrCache(Entity.presence(entity), nodeHash.node, nodeHash.hash);
        }
    }
}
