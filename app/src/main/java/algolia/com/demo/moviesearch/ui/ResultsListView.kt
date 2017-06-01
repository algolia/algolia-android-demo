package algolia.com.demo.moviesearch.ui

import algolia.com.demo.moviesearch.R
import algolia.com.demo.moviesearch.io.SearchResultsJsonParser
import android.content.Context
import android.util.AttributeSet
import android.widget.ListView
import com.algolia.instantsearch.helpers.Searcher
import com.algolia.instantsearch.model.SearchResults
import com.algolia.instantsearch.ui.views.AlgoliaWidget
import com.algolia.search.saas.AlgoliaException
import com.algolia.search.saas.Query

class ResultsListView(context: Context, attrs: AttributeSet) : ListView(context, attrs), AlgoliaWidget {
    private val adapter: MovieAdapter = MovieAdapter(context, R.layout.cell_movie)
    private val resultsParser = SearchResultsJsonParser()

    init {
        setAdapter(adapter)
    }

    override fun initWithSearcher(searcher: Searcher) {}

    override fun onResults(results: SearchResults, isLoadingMore: Boolean) {
        if (!isLoadingMore) {
            val resultList = resultsParser.parseResults(results.content)
            adapter.clear()
            adapter.addAll(resultList)
            // Scroll the list back to the top.
            smoothScrollToPosition(0)
        } else {
            val resultList = resultsParser.parseResults(results.content)
            adapter.addAll(resultList)
        }
    }

    override fun onError(query: Query, error: AlgoliaException) {}

    override fun onReset() {}
}
