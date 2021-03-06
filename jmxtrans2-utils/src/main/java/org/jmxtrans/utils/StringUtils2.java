/**
 * The MIT License
 * Copyright (c) 2014 JMXTrans Team
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
package org.jmxtrans.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Yet another {@code StringUtils} class.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public final class StringUtils2 {

    private StringUtils2() {
    }

    /**
     * Split given String. Delimiters are {@code ","}, {@code ";"} and {@code "\n"}.
     *
     * @param delimitedString the delimiter used to split the string
     * @return splitted string or {@code null} if given {@code delimitedString} is {@code null}.
     */
    @Nullable
    public static List<String> delimitedStringToList(@Nullable String delimitedString) {
        if (delimitedString == null) {
            return null;
        }
        String[] splits = delimitedString.split("[,;\\n]");

        List<String> result = new ArrayList<String>();

        for (String split : splits) {
            split = split.trim();
            if (!split.isEmpty()) {
                result.add(split);
            }
        }
        return result;
    }

    /**
     * Join given {@code tokens} with given {@code delimiter}.
     *
     * Sample: tokens {@code "com"}, "mycompany, "ecommerce", "server1"} with delimiter {@code "."}
     * returns {@code "com.mycompany.ecommerce.server1"}.
     *
     * @param tokens    a list of of token to join
     * @param delimiter the delimiter used to join the string
     * @return the joined tokens (<code>null</code> if given <code>tokens</code> is <code>null</code>
     */
    @Nullable
    public static String join(@Nullable List<String> tokens, @Nonnull String delimiter) {
        if (tokens == null) {
            return null;
        }
        Objects.requireNonNull(delimiter, "given delimiter can not be null");

        Iterator<String> it = tokens.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            String token = it.next();
            sb.append(token);
            if (it.hasNext()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Reverse tokens of given tokenized <code>str</code>.
     *
     * Sample: "server1.ecommerce.mycompany.com" returns {@code "com.mycompany.ecommerce.server1"}.
     *
     * @param str       The input string to reverse
     * @param delimiter The delimiter used to split the string
     * @return reversed string or <code>null</code> if given string is <code>null</code>
     */
    @Nullable
    public static String reverseTokens(@Nullable String str, @Nonnull String delimiter) {
        if (str == null) {
            return null;
        }
        Objects.requireNonNull(delimiter, "given delimiter can not be null");

        StringTokenizer st = new StringTokenizer(str, delimiter);
        List<String> tokens = new ArrayList<String>();

        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        Collections.reverse(tokens);

        return join(tokens, delimiter);
    }

    /**
     * Escape all non {@code a->z,A->Z, 0->9} and {@code '-'} with a {@code '_'}.
     *
     * {@code '.'} is escaped with a {@code '_'} if {@code escapeDot} is <code>true</code>.
     *
     * @param str       the string to escape
     * @param escapeDot indicates whether '.' should be escaped into '_' or not.
     * @param result    the {@linkplain StringBuilder} in which the escaped string is appended
     */
    public static void appendEscapedNonAlphaNumericChars(@Nonnull String str, boolean escapeDot, @Nonnull StringBuilder result) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isLetter(ch) || Character.isDigit(ch) || ch == '-') {
                result.append(ch);
            } else if (ch == '.') {
                result.append(escapeDot ? '_' : ch);
            } else if (ch == '"' && ((i == 0) || (i == chars.length - 1))) {
                // ignore starting and ending '"' that are used to quote() objectname's values (see ObjectName.value())
            } else {
                result.append('_');
            }
        }
    }
}
