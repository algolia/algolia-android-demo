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
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.widget.AbsListView;

import com.algolia.instantsearch.SearchHelper;
import com.algolia.search.saas.Query;

import algolia.com.demo.moviesearch.R;

public class MovieSearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, AbsListView.OnScrollListener {
    // UI:
    private SearchView searchView;
    private ResultsListView moviesListView;

    // Constants
    private static final int LOAD_MORE_THRESHOLD = 5;
    private static final int HITS_PER_PAGE = 20;

    private SearchHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_search);

        // Bind UI components.
        moviesListView = (ResultsListView) findViewById(R.id.listview_movies);
        moviesListView.setOnScrollListener(this);

        // Init Algolia.
        helper = new SearchHelper(moviesListView, "latency", "dce4286c2833e8cf4b7b1f2d3fa1dbcb", "movies");

        // Pre-build query.
        helper.setBaseQuery(new Query().setAttributesToRetrieve("title", "image", "rating", "year")
                .setAttributesToHighlight("title")
                .setHitsPerPage(HITS_PER_PAGE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_movie_search, menu);

        // Configure search view.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    // SearchView.OnQueryTextListener

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        // Nothing to do: the search has already been performed by `onQueryTextChange()`.
        // We do try to close the keyboard, though.
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
        helper.search(searchView.getQuery().toString());
        return true;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState)
    {
        // Nothing to do.
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
    {
        // Abort if list is empty or the end has already been reached.
        if (totalItemCount == 0 || !helper.shouldLoadMore())
            return;

        // Load more if we are sufficiently close to the end of the list.
        int firstInvisibleItem = firstVisibleItem + visibleItemCount;
        if (firstInvisibleItem + LOAD_MORE_THRESHOLD >= totalItemCount)
            helper.loadMore();
    }
}
