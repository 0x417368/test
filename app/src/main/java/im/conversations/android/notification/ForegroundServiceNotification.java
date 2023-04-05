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

package im.conversations.android.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import androidx.core.content.ContextCompat;
import im.conversations.android.R;
import im.conversations.android.ui.activity.MainActivity;
import im.conversations.android.xmpp.ConnectionPool;

public class ForegroundServiceNotification extends AbstractNotification {

    public static final int ID = 1;

    public ForegroundServiceNotification(final Service service) {
        super(service);
    }

    public Notification build(final ConnectionPool.Summary summary) {
        final Notification.Builder builder = new Notification.Builder(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // starting with Android 7 the app name is displayed as part of the notification
            // this means we do not have to repeat it in the 'content title'
            builder.setContentTitle(
                    context.getString(
                            R.string.connected_accounts, summary.connected, summary.total));
        } else {
            builder.setContentTitle(context.getString(R.string.app_name));
            builder.setContentText(
                    context.getString(
                            R.string.connected_accounts, summary.connected, summary.total));
        }
        builder.setContentIntent(buildPendingIntent());
        builder.setWhen(0)
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(
                        summary.isConnected()
                                ? R.drawable.ic_link_24dp
                                : R.drawable.ic_link_off_24dp)
                .setLocalOnly(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(Channels.CHANNEL_FOREGROUND);
        }

        return builder.build();
    }

    private PendingIntent buildPendingIntent() {
        return PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void update(final ConnectionPool.Summary summary) {
        final var notificationManager =
                ContextCompat.getSystemService(context, NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        final var notification = build(summary);
        notificationManager.notify(ID, notification);
    }
}
