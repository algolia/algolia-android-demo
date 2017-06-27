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

package algolia.com.demo.moviesearch.ui;

import android.app.SearchManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

import algolia.com.demo.moviesearch.R;
import algolia.com.demo.moviesearch.io.SearchResultsJsonParser;
import algolia.com.demo.moviesearch.model.HighlightedResult;
import algolia.com.demo.moviesearch.model.Movie;

public class MovieSearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, AbsListView.OnScrollListener {
    // BL:
    private Client client;
    private Index index;
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

    /** Number of items before the end of the list past which we start loading more content. */
    private static final int LOAD_MORE_THRESHOLD = 5;

    // Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_search);

        // Bind UI components.
        moviesListView = (ListView) findViewById(R.id.listview_movies);
        moviesListView.setAdapter(moviesListAdapter = new MovieAdapter(this, R.layout.cell_movie));
        moviesListView.setOnScrollListener(this);

        // Init Algolia.
        client = new Client("latency", "dce4286c2833e8cf4b7b1f2d3fa1dbcb");
        index = client.getIndex("movies");

        // Pre-build query.
        query = new Query();
        query.setAttributesToRetrieve("title", "image", "rating", "year");
        query.setAttributesToHighlight("title");
        query.setHitsPerPage(HITS_PER_PAGE);

        // Configure Universal Image Loader.
        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .resetViewBeforeLoading(true)
                .displayer(new FadeInBitmapDisplayer(300))
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                imageLoader = ImageLoader.getInstance();
                ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(MovieSearchActivity.this)
                        .memoryCacheSize(2 * 1024 * 1024)
                        .memoryCacheSizePercentage(13) // default
                        .build();
                imageLoader.init(configuration);
            }
        }).start();

        highlightRenderer = new HighlightRenderer(this);
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

        return true;
    }

    // Actions

    private void search() {
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
                    if (currentSearchSeqNo <= lastDisplayedSeqNo) {
                        return;
                    }

                    List<HighlightedResult<Movie>> results = resultsParser.parseResults(content);
                    if (results.isEmpty()) {
                        endReached = true;
                    } else {
                        moviesListAdapter.clear();
                        moviesListAdapter.addAll(results);
                        moviesListAdapter.notifyDataSetChanged();
                        lastDisplayedSeqNo = currentSearchSeqNo;
                        lastDisplayedPage = 0;
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
                    if (lastDisplayedSeqNo != currentSearchSeqNo) {
                        return;
                    }

                    List<HighlightedResult<Movie>> results = resultsParser.parseResults(content);
                    if (results.isEmpty()) {
                        endReached = true;
                    } else {
                        moviesListAdapter.addAll(results);
                        moviesListAdapter.notifyDataSetChanged();
                        lastDisplayedPage = lastRequestedPage;
                    }
                }
            }
        });
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
        if (totalItemCount == 0 || endReached) {
            return;
        }

        // Ignore if a new page has already been requested.
        if (lastRequestedPage > lastDisplayedPage) {
            return;
        }

        // Load more if we are sufficiently close to the end of the list.
        int firstInvisibleItem = firstVisibleItem + visibleItemCount;
        if (firstInvisibleItem + LOAD_MORE_THRESHOLD >= totalItemCount) {
            loadMore();
        }
    }
}
