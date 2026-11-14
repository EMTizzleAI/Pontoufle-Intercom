package ai.emtizzle.pontoufleintercom;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class PontoufleWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = new RemoteViews(
                    context.getPackageName(), R.layout.widget_pontoufle);

            Intent talk = new Intent(context, MainActivity.class);
            talk.setAction(MainActivity.ACTION_TALK);
            talk.putExtra("listen_now", true);
            PendingIntent talkPending = PendingIntent.getActivity(
                    context, id * 2, talk,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, talkPending);

            Intent read = new Intent(context, MainActivity.class);
            read.setAction(MainActivity.ACTION_READ_REPLY);
            PendingIntent readPending = PendingIntent.getActivity(
                    context, id * 2 + 1, read,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_read_reply, readPending);

            manager.updateAppWidget(id, views);
        }
    }
}
