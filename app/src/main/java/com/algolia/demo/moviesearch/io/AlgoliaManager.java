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

package com.algolia.demo.moviesearch.io;


import android.content.Context;
import android.support.annotation.NonNull;

import com.algolia.search.saas.MirroredIndex;
import com.algolia.search.saas.OfflineClient;
import com.algolia.search.saas.Query;

import com.algolia.demo.moviesearch.R;

/**
 * Manages Algolia indices (singleton).
 */
public class AlgoliaManager {
    /** The singleton instance. */
    private static AlgoliaManager instance;

    private Context context;
    private OfflineClient client;
    private MirroredIndex moviesIndex;

    // ----------------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------------

    private AlgoliaManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        client = new OfflineClient(context, "latency", context.getResources().getString(R.string.ALGOLIA_API_KEY));
        client.enableOfflineMode(context.getResources().getString(R.string.ALGOLIA_OFFLINE_SDK_LICENSE_KEY));
        moviesIndex = client.initIndex("movies");

        // Sync part of the index for offline usage.
        moviesIndex.setMirrored(true);
        moviesIndex.setDataSelectionQueries(
                new MirroredIndex.DataSelectionQuery(new Query().set("filters", "year >= 2000"), 500),
                new MirroredIndex.DataSelectionQuery(new Query().set("filters", "year < 2000"), 100)
        );
        moviesIndex.setRequestStrategy(MirroredIndex.Strategy.FALLBACK_ON_TIMEOUT);
        moviesIndex.setOfflineFallbackTimeout(500);

        // Bootstrap the `movies` index from the resources.
        moviesIndex.bootstrapFromRawResources(context.getResources(), R.raw.settings, R.raw.objects);
    }

    /**
     * Get the singleton instance.
     *
     * @param context An Android context.
     * @return The singleton instance.
     */
    public static @NonNull
    AlgoliaManager getInstance(@NonNull Context context) {
        // NOTE: Double-check to prevent unnecessary synchronization in the common case.
        if (instance == null) {
            synchronized (AlgoliaManager.class) {
                if (instance == null) {
                    instance = new AlgoliaManager(context);
                }
            }
        }
        return instance;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public OfflineClient getClient() {
        return client;
    }

    public MirroredIndex getMoviesIndex() {
        return moviesIndex;
    }
}
