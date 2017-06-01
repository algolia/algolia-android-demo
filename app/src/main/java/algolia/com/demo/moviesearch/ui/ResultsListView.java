package algolia.com.demo.moviesearch.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ListView;

import com.algolia.instantsearch.helpers.Searcher;
import com.algolia.instantsearch.model.SearchResults;
import com.algolia.instantsearch.ui.views.AlgoliaWidget;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Query;

import java.util.List;

import algolia.com.demo.moviesearch.R;
import algolia.com.demo.moviesearch.io.SearchResultsJsonParser;
import algolia.com.demo.moviesearch.model.HighlightedResult;
import algolia.com.demo.moviesearch.model.Movie;

public class ResultsListView extends ListView implements AlgoliaWidget {
    private final MovieAdapter adapter;
    private SearchResultsJsonParser resultsParser = new SearchResultsJsonParser();

    public ResultsListView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        setAdapter(adapter = new MovieAdapter(context, R.layout.cell_movie));
    }

    @Override public void initWithSearcher(@NonNull Searcher searcher) {
    }

    @Override public void onResults(@NonNull SearchResults results, boolean isLoadingMore) {
        if (!isLoadingMore) {
            List<HighlightedResult<Movie>> resultList = resultsParser.parseResults(results.content);
            adapter.clear();
            adapter.addAll(resultList);
            // Scroll the list back to the top.
            smoothScrollToPosition(0);
        } else {
            List<HighlightedResult<Movie>> resultList = resultsParser.parseResults(results.content);
            adapter.addAll(resultList);
        }
    }

    @Override public void onError(Query query, AlgoliaException error) {
    }

    @Override public void onReset() {
    }
}
