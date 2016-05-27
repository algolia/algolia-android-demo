/*
 * Copyright (c) 2012-2016 Algolia
 * http://www.algolia.com/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package algolia.com.demo.moviesearch.logic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.MirroredIndex;
import com.algolia.search.saas.Query;
import com.algolia.search.saas.Request;
import com.algolia.search.saas.SyncListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import algolia.com.demo.moviesearch.BuildConfig;
import algolia.com.demo.moviesearch.io.AlgoliaManager;
import algolia.com.demo.moviesearch.io.ImageLoaderManager;
import algolia.com.demo.moviesearch.io.MovieJsonParser;
import algolia.com.demo.moviesearch.model.Movie;

public class AlgoliaSyncService extends Service implements SyncListener {
    private MirroredIndex index;
    private boolean shouldSyncImages = false;
    private ImageLoader imageLoader;
    private Queue<String> imagesToLoad = new ArrayDeque<>();
    private int imagesLoading;

    public static final String ACTION_SYNC_IF_NEEDED = "algolia.sync.ifneeded";
    public static final String ACTION_SYNC_ALWAYS = "algolia.sync.always";

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // This is not a bound service.
        return null;
    }

    @Override
    public void onCreate() {
        imageLoader = ImageLoaderManager.getInstance(this).getImageLoader();
        index = AlgoliaManager.getInstance(this).getMoviesIndex();
        index.addSyncListener(this);

        // Shorten the delay between syncs in debug configuration.
        if (BuildConfig.DEBUG) {
            index.setDelayBetweenSyncs(30, TimeUnit.MINUTES);
        }
    }

    @Override
    public void onDestroy() {
        index.removeSyncListener(this);
        index = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_SYNC_IF_NEEDED: {
                    index.syncIfNeeded();
                    break;
                }
                case ACTION_SYNC_ALWAYS: {
                    index.sync();
                    break;
                }
            }
        }
        return Service.START_NOT_STICKY;
    }

    public static void sync(@NonNull Context context) {
        context.startService(new Intent(AlgoliaSyncService.ACTION_SYNC_ALWAYS, null, context, AlgoliaSyncService.class));
    }

    public static void syncIfNeededAndPossible(@NonNull Context context) {
        if (ConnectivityReceiver.isNetworkOkForSync(context)) {
            context.startService(new Intent(AlgoliaSyncService.ACTION_SYNC_IF_NEEDED, null, context, AlgoliaSyncService.class));
        }
    }

    private void syncDidFinish(Throwable error) {
        if (error != null) {
            Log.e(this.getClass().getName(), "Sync failed on " + index, error);
            Toast.makeText(this, "Sync failed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sync succeeded", Toast.LENGTH_SHORT).show();
        }
    }

    // ----------------------------------------------------------------------
    // Linked resource pre-loading
    // ----------------------------------------------------------------------

    /** Currently ongoing browse request (if any). */
    private Request browseRequest;

    private void preloadImages() {
        cancelPreloadImages();
        browseRequest = index.browseMirrorAsync(new Query(), browseHandler);
    }

    private void cancelPreloadImages() {
        if (browseRequest != null) {
            browseRequest.cancel();
            browseRequest = null;
        }
    }

    private CompletionHandler browseHandler = new CompletionHandler() {
        @Override
        public void requestCompleted(JSONObject content, AlgoliaException error) {
            browseRequest = null;
            if (error != null) {
                syncDidFinish(error);
                return; // TODO: Handle better
            }
            try {
                final MovieJsonParser parser = new MovieJsonParser();
                String cursor = content.optString("cursor", null);
                JSONArray hits = content.getJSONArray("hits");
                for (int i = 0; i < hits.length(); ++i) {
                    JSONObject hit = hits.getJSONObject(i);
                    Movie movie = parser.parse(hit);
                    if (movie != null && movie.getImage() != null) {
                        imagesToLoad.add(movie.getImage());
                    }
                }
                dequeueNext();

                // Fetch next batch of results if necessary.
                if (cursor != null) {
                    browseRequest = index.browseMirrorFromAsync(cursor, this);
                }
            }
            catch (JSONException e) {
                syncDidFinish(e);
            }
        }
    };

    private void dequeueNext() {
        String imageUrl = imagesToLoad.poll();
        if (imageUrl != null) {
            ++imagesLoading;
            imageLoader.loadImage(imageUrl, ImageLoaderManager.getInstance(this).getDisplayImageOptions(), imageLoadingListener);
        } else if (imagesLoading == 0) {
            syncDidFinish(null);
        }
    }

    private ImageLoadingListener imageLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
            // Nothing to do.
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            --imagesLoading;
            dequeueNext();
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            --imagesLoading;
            try {
                imageLoader.getDiskCache().save(imageUri, loadedImage);
            }
            catch (IOException e) {
                Log.e(AlgoliaSyncService.class.getName(), "Could not save image for URI " + imageUri, e);
            }
            dequeueNext();
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            --imagesLoading;
        }
    };

    // ----------------------------------------------------------------------
    // Listeners
    // ----------------------------------------------------------------------

    // SyncListener

    @Override
    public void syncDidStart(MirroredIndex index) {
        Log.d(this.getClass().getName(), "Sync started on " + index);
        Toast.makeText(this, "Sync started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void syncDidFinish(MirroredIndex index, Throwable error, MirroredIndex.SyncStats stats) {
        if (error == null && shouldSyncImages) {
            Log.d(this.getClass().getName(), "Sync succeeded on " + index + "; stats: " + stats);
            Toast.makeText(this, "Data sync finished; syncing images now...", Toast.LENGTH_SHORT).show();
            preloadImages();
        } else {
            syncDidFinish(error);
        }
    }


}
