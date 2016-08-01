package algolia.com.demo.moviesearch.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ListView;

import com.algolia.instantsearch.SearchHelper;
import com.algolia.instantsearch.views.AlgoliaResultsView;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Query;

import org.json.JSONObject;

import java.util.List;

import algolia.com.demo.moviesearch.R;
import algolia.com.demo.moviesearch.io.SearchResultsJsonParser;
import algolia.com.demo.moviesearch.model.HighlightedResult;
import algolia.com.demo.moviesearch.model.Movie;

public class ResultsListView extends ListView implements AlgoliaResultsListener {
    private final MovieAdapter adapter;
    private SearchResultsJsonParser resultsParser = new SearchResultsJsonParser();

    public ResultsListView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        setAdapter(adapter = new MovieAdapter(context, R.layout.cell_movie));
    }

    @Override public void onInit(@NonNull Searcher helper) {
    }

    @Override public void onUpdateView(@Nullable JSONObject hits, boolean isLoadingMore) {
        if (!isLoadingMore) {
            List<HighlightedResult<Movie>> results = resultsParser.parseResults(hits);
            adapter.clear();
            adapter.addAll(results);
            // Scroll the list back to the top.
            smoothScrollToPosition(0);
        } else {
            List<HighlightedResult<Movie>> results = resultsParser.parseResults(hits);
            adapter.addAll(results);
        }
    }

    @Override public void onError(Query query, AlgoliaException error) {
    }

    @Override public void onReset() {
    }
}
