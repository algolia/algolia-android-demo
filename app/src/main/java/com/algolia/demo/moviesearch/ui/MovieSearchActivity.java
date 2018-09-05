/*
 * Copyright (c) 2015 Algolia
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

package com.algolia.demo.moviesearch.ui;

import android.app.SearchManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.algolia.demo.moviesearch.R;
import com.algolia.demo.moviesearch.io.ImageLoaderManager;
import com.algolia.demo.moviesearch.io.SearchResultsJsonParser;
import com.algolia.demo.moviesearch.model.HighlightedResult;
import com.algolia.demo.moviesearch.model.Movie;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.MirroredIndex;
import com.algolia.search.saas.OfflineClient;
import com.algolia.search.saas.OfflineIndex;
import com.algolia.search.saas.Query;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MovieSearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, AbsListView.OnScrollListener {
    // BL:
    private MirroredIndex index;
    private Query query;
    private SearchResultsJsonParser resultsParser = new SearchResultsJsonParser();
    private int lastSearchedSeqNo;
    private int lastDisplayedSeqNo;
    private int lastRequestedPage;
    private int lastDisplayedPage;
    private boolean endReached;

    // UI:
    private SearchView searchView;
    private ListView moviesListView;
    private MovieAdapter moviesListAdapter;
    private ImageLoader imageLoader;
    private DisplayImageOptions displayImageOptions;
    private HighlightRenderer highlightRenderer;

    // Constants

    private static final int HITS_PER_PAGE = 20;

    /**
     * Number of items before the end of the list past which we start loading more content.
     */
    private static final int LOAD_MORE_THRESHOLD = 5;

    // Lifecycle
    private File createFileFromInputStream(InputStream inputStream, String name) {
        try {
            File f = new File(getFilesDir(), name);
            if (!f.exists()) {
                f.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_search);

        Log.e("PLN", "License:" + getResources().getString(R.string.ALGOLIA_OFFLINE_SDK_LICENSE_KEY));

        OfflineClient client = new OfflineClient(this, "latency", getResources().getString(R.string.ALGOLIA_API_KEY));
        client.enableOfflineMode(getResources().getString(R.string.ALGOLIA_OFFLINE_SDK_LICENSE_KEY));

//        Log.e("Files", "Before");
//        try {
//            Log.e("Files", "Before getAssets");
//            String[] assets = getAssets().list("");
//            Log.e("Files", "After gotAssets, got " + (assets != null ? assets.length : 0));
//            for (String asset : assets) {
//                Log.e("Asset name: ", asset);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        Log.e("Files", "After");
//
//        File resourcesDir = new File(getPackageResourcePath() + "/src/app/res/raw/");
//        Log.e("PLN", "resourcePath: " + getPackageResourcePath());
//        File[] files = new File(getPackageResourcePath()).listFiles();
//        Log.d("Files", "Size: "+ files.length);
//        for (int i = 0; i < files.length; i++)
//        {
//            Log.d("Files", "FileName:" + files[i].getName());
//        }
        createIndex(client, 100, 0);

        Log.e("PLN", "Done.");
//        // Init Algolia.
//        index = AlgoliaManager.getInstance(this).getMoviesIndex();
//
//        // Launch a sync if needed.
//        AlgoliaSyncService.syncIfNeededAndPossible(this);

        // Bind UI components.
        moviesListView = (ListView) findViewById(R.id.listview_movies);
        moviesListView.setAdapter(moviesListAdapter = new MovieAdapter(this, R.layout.cell_movie));
        moviesListView.setOnScrollListener(this);

        // Pre-build query.
        query = new Query();
        query.setAttributesToRetrieve("title", "image", "rating", "year");
        query.setAttributesToHighlight("title");
        query.setHitsPerPage(HITS_PER_PAGE);

        // Configure Universal Image Loader.
        imageLoader = ImageLoaderManager.getInstance(this).getImageLoader();
        displayImageOptions = ImageLoaderManager.getInstance(this).getDisplayImageOptions();
        highlightRenderer = new HighlightRenderer(this);
    }
//
//    private class Task extends AsyncTask<Context, Void, Void> {
//        @Override
//        protected Void doInBackground(Context... contexts) {
//            final Context context = contexts[0];
//
//            Log.e("PLN", "License:" + context.getResources().getString(R.string.ALGOLIA_OFFLINE_SDK_LICENSE_KEY));
//
//            File resourcesDir = new File(context.getPackageResourcePath() + "/src/app/res/raw/");
//
//            OfflineClient client = new OfflineClient(context, "latency", context.getResources().getString(R.string.ALGOLIA_API_KEY));
//            client.enableOfflineMode(context.getResources().getString(R.string.ALGOLIA_OFFLINE_SDK_LICENSE_KEY));
//
//            final ArrayList<File> objectFiles = new ArrayList<>();
//            for (int i = 0; i < 1000; i++) {
//                objectFiles.add(new File(resourcesDir, "dump.json"));
//            }
//            File settingsFile = new File(resourcesDir, "settings.json");
//
//
//            // Create the index.
//            final OfflineIndex index = client.getOfflineIndex("tests");
//
//            // Check that no offline data exists.
//            Log.e("PLN", "hasOfflineData should be false: " + index.hasOfflineData());
//
//            // Build the index.
//            File[] fileArray = new File[objectFiles.size()];
//            index.buildFromFiles(settingsFile, objectFiles.toArray(fileArray), new CompletionHandler() {
//                @Override
//                public void requestCompleted(JSONObject content, AlgoliaException error) {
//                    Log.e("PLN", "error should be null: " + error);
//                    Log.e("PLN", "hasOfflineData should be true: " + index.hasOfflineData());
//
//                    // Search.
//                    Query query1 = new Query().setQuery("peanuts").setFilters("kind:animal");
//                    index.searchAsync(query1, new CompletionHandler() {
//                        @Override
//                        public void requestCompleted(JSONObject jsonObject, AlgoliaException e) {
//                            Log.e("PLN", "error should be null: " + e);
//                            Log.e("PLN", "hits: " + jsonObject);
//                        }
//                    });
//                }
//            });
//        }
//    }


    public void createIndex(final OfflineClient client, final int limit, final int count) {
        try {
            if (limit <= count) {
                Log.e("PLN", "Reached limit (" + limit + ")");
                return;
            }
            String indexName = UUID.randomUUID().toString();
            File dumpFile = createFileFromInputStream(getAssets().open("dump.json"), "dump" + indexName + ".json");
            File settingsFile = createFileFromInputStream(getAssets().open("settings.json"), "settings.json");
//                Log.e("PLN", "Sfile: " + settingsFile.toString());
                Log.e("PLN", "Dfile: " + dumpFile.toString());
            final ArrayList<File> objectFiles = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                objectFiles.add(dumpFile);
            }

            // Create the index.
            final OfflineIndex index = client.getOfflineIndex(indexName);

            // Check that no offline data exists.
//                Log.e("PLN", "hasOfflineData should be false: " + index.hasOfflineData());

            // Build the index.
            File[] fileArray = new File[objectFiles.size()];
            index.buildFromFiles(settingsFile, objectFiles.toArray(fileArray), new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject content, AlgoliaException error) {
                    //Log.e("PLN", "error should be null: " + error);
//                        Log.e("PLN", "hasOfflineData should be true: " + index.hasOfflineData());
                    createIndex(client, limit, count + 1);
                    Log.e("PLN", "Done creating index " + (count + 1));
//                        // Search.
//                        Query query1 = new Query();
//                        index.searchAsync(query1, new CompletionHandler() {
//                            @Override
//                            public void requestCompleted(JSONObject jsonObject, AlgoliaException e) {
////                                Log.e("PLN", "error should be null: " + e);
////                                Log.e("PLN", "hits: " + jsonObject);
//                            }
//                        });
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_movie_search, menu);

        // Configure search view.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);

        search();

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // WARNING: `onCreateOptionsMenu()` is called *after* `onResume()`--which is insane, but hey! what can we do?
        if (searchView != null) {
            search();
        }
    }

    // Actions

    private void search() {
        if (true) return;
        final int currentSearchSeqNo = ++lastSearchedSeqNo;
        query.setQuery(searchView.getQuery().toString());
        lastRequestedPage = 0;
        lastDisplayedPage = -1;
        endReached = false;
        index.searchAsync(query, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (content != null && error == null) {
                    // NOTE: Check that the received results are newer that the last displayed results.
                    //
                    // Rationale: Although TCP imposes a server to send responses in the same order as
                    // requests, nothing prevents the system from opening multiple connections to the
                    // same server, nor the Algolia client to transparently switch to another server
                    // between two requests. Therefore the order of responses is not guaranteed.
                    if (currentSearchSeqNo <= lastDisplayedSeqNo)
                        return;

                    List<HighlightedResult<Movie>> results = resultsParser.parseResults(content);
                    if (results == null || results.isEmpty()) {
                        endReached = true;
                    } else {
                        moviesListAdapter.clear();
                        moviesListAdapter.addAll(results);
                        moviesListAdapter.notifyDataSetChanged();
                        lastDisplayedSeqNo = currentSearchSeqNo;
                        lastDisplayedPage = 0;
                        updateResultsOrigin(content);
                    }

                    // Scroll the list back to the top.
                    moviesListView.smoothScrollToPosition(0);
                }
            }
        });
    }

    private void loadMore() {
        Query loadMoreQuery = new Query(query);
        loadMoreQuery.setPage(++lastRequestedPage);
        final int currentSearchSeqNo = lastSearchedSeqNo;
        index.searchAsync(loadMoreQuery, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (content != null && error == null) {
                    // Ignore results if they are for an older query.
                    if (lastDisplayedSeqNo != currentSearchSeqNo)
                        return;

                    List<HighlightedResult<Movie>> results = resultsParser.parseResults(content);
                    if (results.isEmpty()) {
                        endReached = true;
                    } else {
                        moviesListAdapter.addAll(results);
                        moviesListAdapter.notifyDataSetChanged();
                        lastDisplayedPage = lastRequestedPage;
                        updateResultsOrigin(content);
                    }
                }
            }
        });
    }

    private void updateResultsOrigin(@NonNull JSONObject content) {
        String origin = content.optString(MirroredIndex.JSON_KEY_ORIGIN, null);
        boolean isLocal = origin != null && origin.equals(MirroredIndex.JSON_VALUE_ORIGIN_LOCAL);
        moviesListView.setBackgroundColor(getResources().getColor(isLocal ? R.color.BackgroundLocal : R.color.BackgroundRemote));
    }

    // Data sources

    private class MovieAdapter extends ArrayAdapter<HighlightedResult<Movie>> {
        public MovieAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup cell = (ViewGroup) convertView;
            if (cell == null) {
                cell = (ViewGroup) getLayoutInflater().inflate(R.layout.cell_movie, null);
            }

            ImageView posterImageView = (ImageView) cell.findViewById(R.id.imageview_poster);
            TextView titleTextView = (TextView) cell.findViewById(R.id.textview_title);
            TextView yearTextView = (TextView) cell.findViewById(R.id.textview_year);

            HighlightedResult<Movie> result = moviesListAdapter.getItem(position);

            imageLoader.displayImage(result.getResult().getImage(), posterImageView, displayImageOptions);
            titleTextView.setText(highlightRenderer.renderHighlights(result.getHighlight("title").getHighlightedValue()));
            yearTextView.setText(String.format("%d", result.getResult().getYear()));

            return cell;
        }

        @Override
        public void addAll(Collection<? extends HighlightedResult<Movie>> items) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                super.addAll(items);
            } else {
                for (HighlightedResult<Movie> item : items) {
                    add(item);
                }
            }
        }
    }

    // SearchView.OnQueryTextListener

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Nothing to do: the search has already been performed by `onQueryTextChange()`.
        // We do try to close the keyboard, though.
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        search();
        return true;
    }

    // AbsListView.OnScrollListener

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Nothing to do.
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Abort if list is empty or the end has already been reached.
        if (totalItemCount == 0 || endReached)
            return;

        // Ignore if a new page has already been requested.
        if (lastRequestedPage > lastDisplayedPage)
            return;

        // Load more if we are sufficiently close to the end of the list.
        int firstInvisibleItem = firstVisibleItem + visibleItemCount;
        if (firstInvisibleItem + LOAD_MORE_THRESHOLD >= totalItemCount)
            loadMore();
    }
}
