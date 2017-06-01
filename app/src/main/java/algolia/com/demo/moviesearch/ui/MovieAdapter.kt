package algolia.com.demo.moviesearch.ui

import algolia.com.demo.moviesearch.R
import algolia.com.demo.moviesearch.model.HighlightedResult
import algolia.com.demo.moviesearch.model.Movie
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer

internal class MovieAdapter(context: Context, resource: Int) : ArrayAdapter<HighlightedResult<Movie>>(context, resource) {

    private val highlightRenderer: HighlightRenderer
    private var imageLoader: ImageLoader? = null
    private val displayImageOptions: DisplayImageOptions = DisplayImageOptions.Builder()
            .cacheOnDisk(true)
            .resetViewBeforeLoading(true)
            .displayer(FadeInBitmapDisplayer(300))
            .build()

    init {

        // Configure Universal Image Loader.
        Thread(Runnable {
            imageLoader = ImageLoader.getInstance()
            if (!imageLoader!!.isInited) {
                val configuration = ImageLoaderConfiguration.Builder(context)
                        .memoryCacheSize(2 * 1024 * 1024)
                        .memoryCacheSizePercentage(13) // default
                        .build()
                imageLoader!!.init(configuration)
            }
        }).start()

        highlightRenderer = HighlightRenderer(context)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var cell = convertView
        if (cell == null) {
            cell = LayoutInflater.from(context).inflate(R.layout.cell_movie, parent, false)
        }

        val posterImageView = cell!!.findViewById(R.id.imageview_poster) as ImageView
        val titleTextView = cell.findViewById(R.id.textview_title) as TextView
        val yearTextView = cell.findViewById(R.id.textview_year) as TextView

        val result = getItem(position)

        imageLoader!!.displayImage(result!!.result.image, posterImageView, displayImageOptions)
        titleTextView.text = highlightRenderer.renderHighlights(result.getHighlight("title")?.highlightedValue.toString())
        yearTextView.text = String.format("%d", result.result.year)

        return cell
    }

    override fun addAll(items: Collection<HighlightedResult<Movie>>?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.addAll(items)
        } else if (items?.isNotEmpty()?:false){
            for (item in items!!) {
                add(item)
            }
            notifyDataSetChanged()
        }
    }
}
