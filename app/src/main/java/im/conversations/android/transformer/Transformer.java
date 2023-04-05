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

package im.conversations.android.transformer;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import im.conversations.android.axolotl.AxolotlService;
import im.conversations.android.database.ConversationsDatabase;
import im.conversations.android.database.model.Account;
import im.conversations.android.database.model.ChatIdentifier;
import im.conversations.android.database.model.MessageIdentifier;
import im.conversations.android.database.model.MessageState;
import im.conversations.android.database.model.Modification;
import im.conversations.android.database.model.StanzaId;
import im.conversations.android.xml.Namespace;
import im.conversations.android.xmpp.Range;
import im.conversations.android.xmpp.manager.ArchiveManager;
import im.conversations.android.xmpp.model.DeliveryReceipt;
import im.conversations.android.xmpp.model.axolotl.Encrypted;
import im.conversations.android.xmpp.model.correction.Replace;
import im.conversations.android.xmpp.model.fallback.Fallback;
import im.conversations.android.xmpp.model.markers.Displayed;
import im.conversations.android.xmpp.model.muc.user.MucUser;
import im.conversations.android.xmpp.model.reactions.Reactions;
import im.conversations.android.xmpp.model.reply.Reply;
import im.conversations.android.xmpp.model.retract.Retract;
import im.conversations.android.xmpp.model.stanza.Message;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jxmpp.jid.Jid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Transformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Transformer.class);

    private final ConversationsDatabase database;
    private final Account account;

    private final AxolotlService axolotlService;

    public Transformer(
            final Account account,
            final Context context,
            final ConversationsDatabase conversationsDatabase) {
        this(
                account,
                conversationsDatabase,
                new AxolotlService(account, context, conversationsDatabase));
    }

    public Transformer(
            final Account account,
            final ConversationsDatabase database,
            final AxolotlService axolotlService) {
        Preconditions.checkArgument(account != null, "Account must not be null");
        this.database = database;
        this.account = account;
        this.axolotlService = axolotlService;
    }

    @VisibleForTesting
    public boolean transform(final MessageTransformation transformation) {
        return this.transform(transformation, null);
    }

    public boolean transform(final MessageTransformation transformation, final StanzaId stanzaId) {
        return database.runInTransaction(
                () -> {
                    final var sendDeliveryReceipts = transform(database, transformation);
                    axolotlService.executePostDecryptionHook();
                    if (stanzaId != null) {
                        database.archiveDao().setLivePageStanzaId(account, stanzaId);
                    }
                    return sendDeliveryReceipts;
                });
    }

    public void transform(
            List<MessageTransformation> messageTransformations,
            final Jid archive,
            Range queryRange,
            ArchiveManager.QueryResult queryResult,
            final boolean reachedMaxPagesReversing) {
        database.runInTransaction(
                () -> {
                    for (final MessageTransformation transformation : messageTransformations) {
                        transform(database, transformation);
                    }
                    database.archiveDao()
                            .submitPage(
                                    account,
                                    archive,
                                    queryRange,
                                    queryResult,
                                    reachedMaxPagesReversing);
                    axolotlService.executePostDecryptionHook();
                });
    }

    /**
     * @param transformation
     * @return returns true if there is something we want to send a delivery receipt for. Basically
     *     anything that created a new message in the database. Notably not something that only
     *     updated a status somewhere
     */
    private boolean transform(
            final ConversationsDatabase database, final MessageTransformation transformation) {
        Preconditions.checkState(
                database.inTransaction(), "Transformations must be run from a transaction");
        final var remote = transformation.remote;
        final var messageType = transformation.type;
        final var muc = transformation.getExtension(MucUser.class);

        final ChatIdentifier chat =
                database.chatDao()
                        .getOrCreateChat(account, remote, messageType, Objects.nonNull(muc));

        if (messageType == Message.Type.ERROR) {
            if (transformation.outgoing()) {
                LOGGER.info("Ignoring outgoing error to {}", transformation.to);
                return false;
            }
            database.messageDao()
                    .insertMessageState(
                            chat, transformation.messageId, MessageState.error(transformation));
            return false;
        }

        final Replace messageCorrection = transformation.getExtension(Replace.class);
        final Reactions reactions = transformation.getExtension(Reactions.class);
        final Retract retract = transformation.getExtension(Retract.class);
        final Encrypted encrypted = transformation.getExtension(Encrypted.class);
        final MessageContentWrapper contents;
        if (encrypted != null) {
            if (encrypted.hasPayload()) {
                contents =
                        axolotlService.decryptToMessageContent(
                                transformation.senderIdentity(), encrypted);
            } else {
                return axolotlService.decryptEmptyMessage(
                        transformation.senderIdentity(), encrypted);
            }
        } else {
            // TODO we need to remove fallbacks for reactions, retractions and potentially other
            // things
            contents = MessageContentWrapper.parseCleartext(transformation);
        }

        final boolean identifiableSender =
                Arrays.asList(Message.Type.NORMAL, Message.Type.CHAT).contains(messageType)
                        || Objects.nonNull(transformation.occupantId);
        final boolean isReaction =
                Objects.nonNull(reactions)
                        && Objects.nonNull(reactions.getId())
                        && identifiableSender;
        final boolean isMessageCorrection =
                Objects.nonNull(messageCorrection)
                        && Objects.nonNull(messageCorrection.getId())
                        && identifiableSender;
        final boolean isRetraction =
                Objects.nonNull(retract) && Objects.nonNull(retract.getId()) && identifiableSender;
        // TODO in a way it would be more appropriate to move this into the contents.isEmpty block
        // but for that to work we would need to properly ignore the fallback body
        if (isRetraction) {
            final var messageIdentifier =
                    database.messageDao()
                            .getOrCreateVersion(
                                    chat, transformation, retract.getId(), Modification.RETRACTION);
            database.messageDao()
                    .insertMessageContent(
                            messageIdentifier.version, MessageContentWrapper.RETRACTION);
            return true;
        } else if (contents.isEmpty()) {
            LOGGER.info("Received message from {} w/o contents", transformation.from);
            transformMessageState(chat, transformation);
            if (isReaction) {
                database.messageDao().insertReactions(chat, reactions, transformation);
            }
        } else {
            final MessageIdentifier messageIdentifier;
            try {
                if (isMessageCorrection) {
                    messageIdentifier =
                            database.messageDao()
                                    .getOrCreateVersion(
                                            chat,
                                            transformation,
                                            messageCorrection.getId(),
                                            Modification.CORRECTION);

                } else {
                    messageIdentifier =
                            database.messageDao().getOrCreateMessage(chat, transformation);
                    if (chat.archived) {
                        // only for "proper" messages do we want to unarchive chats
                        database.chatDao().setArchived(chat.id, false);
                    }
                }
            } catch (final IllegalStateException e) {
                LOGGER.warn("Could not get message identifier", e);
                return false;
            }
            database.messageDao().insertMessageContent(messageIdentifier.version, contents);
            final var reply = transformation.getExtension(Reply.class);
            if (Objects.nonNull(reply)
                    && Objects.nonNull(reply.getId())
                    && Objects.nonNull(reply.getTo())) {
                final var fallback =
                        Iterables.tryFind(
                                        transformation.getExtensions(Fallback.class),
                                        f -> Namespace.REPLY.equals(f.getFor()))
                                .orNull();
                final var fallbackBody = fallback == null ? null : fallback.getBody();
                LOGGER.info("fallbacks {}", transformation.getExtensions(Fallback.class));
                LOGGER.info("fallback body {}", fallbackBody);
                database.messageDao()
                        .setInReplyTo(
                                chat,
                                messageIdentifier,
                                messageType,
                                reply.getTo(),
                                reply.getId(),
                                fallbackBody);
            }
            return true;
        }
        return true;
    }

    private void transformMessageState(
            final ChatIdentifier chat, final MessageTransformation transformation) {
        final var displayed = transformation.getExtension(Displayed.class);
        if (displayed != null) {
            if (transformation.outgoing()) {
                LOGGER.info(
                        "Received outgoing displayed marker for chat with {}",
                        transformation.remote);
                return;
            }
            database.messageDao()
                    .insertMessageState(
                            chat, displayed.getId(), MessageState.displayed(transformation));
        }
        final var deliveryReceipt = transformation.getExtension(DeliveryReceipt.class);
        if (deliveryReceipt != null) {
            if (transformation.outgoing()) {
                LOGGER.info("Ignoring outgoing delivery receipt to {}", transformation.to);
                return;
            }
            database.messageDao()
                    .insertMessageState(
                            chat, deliveryReceipt.getId(), MessageState.delivered(transformation));
        }
    }
}
