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

package algolia.com.demo.moviesearch.ui

import algolia.com.demo.moviesearch.R
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.widget.AbsListView
import com.algolia.instantsearch.helpers.InstantSearch
import com.algolia.instantsearch.helpers.Searcher
import com.algolia.search.saas.AlgoliaException
import com.algolia.search.saas.Client
import com.algolia.search.saas.CompletionHandler
import com.algolia.search.saas.Query
import kotlinx.android.synthetic.main.activity_movie_search.*
import org.json.JSONObject

class MovieSearchActivity : AppCompatActivity(), AbsListView.OnScrollListener {
    private var searcher: Searcher? = null
    private var helper: InstantSearch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_search)

        // Bind UI components.
        val moviesListView = this.listview_movies!!
        moviesListView.setOnScrollListener(this)

        // Init Algolia.
        val client = Client("latency", "dce4286c2833e8cf4b7b1f2d3fa1dbcb")
        searcher = Searcher.create(client.initIndex("movies"))
        helper = InstantSearch(moviesListView, searcher!!)

        // Pre-build query.
        searcher!!.query = Query().setAttributesToRetrieve("title", "image", "rating", "year")
                .setAttributesToHighlight("title")
                .setHitsPerPage(HITS_PER_PAGE)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_movie_search, menu)
        // Configure search view.
        helper!!.registerSearchView(this, menu, R.id.search)
        return true
    }

    // AbsListView.OnScrollListener

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
        // Nothing to do.
    }

    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        // Abort if list is empty or the end has already been reached.
        if (totalItemCount == 0 || !searcher!!.hasMoreHits()) {
            return
        }

        // Load more if we are sufficiently close to the end of the list.
        val firstInvisibleItem = firstVisibleItem + visibleItemCount
        if (firstInvisibleItem + LOAD_MORE_THRESHOLD >= totalItemCount) {
            searcher!!.loadMore()
        }
    }

    companion object {

        // Constants
        private val LOAD_MORE_THRESHOLD = 5
        private val HITS_PER_PAGE = 20
    }
}
