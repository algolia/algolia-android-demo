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

package algolia.com.demo.moviesearch.io

import algolia.com.demo.moviesearch.model.Movie
import org.json.JSONObject

/**
 * Parses `Movie` instances from their JSON representation.
 */
class MovieJsonParser {
    /**
     * Parse a single movie record.

     * @param jsonObject JSON object.
     * *
     * @return Parsed movie, or null if error.
     */
    fun parse(jsonObject: JSONObject?): Movie? {
        if (jsonObject == null) {
            return null
        }

        val title = jsonObject.optString("title")
        val image = jsonObject.optString("image")
        val rating = jsonObject.optInt("rating", -1)
        val year = jsonObject.optInt("year", 0)
        if (title != null && image != null && rating >= 0 && year != 0) {
            return Movie(title, image, rating, year)
        }
        return null
    }
}
