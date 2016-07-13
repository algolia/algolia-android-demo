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

package com.algolia.demo.moviesearch.io;

import org.json.JSONObject;

import com.algolia.demo.moviesearch.model.Movie;

/**
 * Parses `Movie` instances from their JSON representation.
 */
public class MovieJsonParser
{
    /**
     * Parse a single movie record.
     *
     * @param jsonObject JSON object.
     * @return Parsed movie, or null if error.
     */
    public Movie parse(JSONObject jsonObject)
    {
        if (jsonObject == null)
            return null;

        String title = jsonObject.optString("title");
        String image = jsonObject.optString("image");
        int rating = jsonObject.optInt("rating", -1);
        int year = jsonObject.optInt("year", 0);
        if (title != null && image != null && rating >= 0 && year != 0) {
            return new Movie(title, image, rating, year);
        }
        return null;
    }
}
