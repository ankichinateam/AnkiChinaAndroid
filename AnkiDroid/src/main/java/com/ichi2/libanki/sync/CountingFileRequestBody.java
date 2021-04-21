/****************************************************************************************
 * Copyright (c) 2019 Mike Hardy <github@mikehardy.net>                                 *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.libanki.sync;

import com.ichi2.libanki.Consts;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;


// Note that in current versions of OkHTTP this is unnecessary as they support
// Decorators / hooks more easily with the builder API, allowing upload transfer tracking
// without a separate object. I believe we will have to move to API21+ for that to be possible
public class CountingFileRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final File file;
    private final ProgressListener listener;
    private final String contentType;

    public CountingFileRequestBody(File file, String contentType, ProgressListener listener) {
        this.file = file;
        this.contentType = contentType;
        this.listener = listener;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);
            long read;
            long bytesReadBandwidth = 0L;
            long lastTime = System.currentTimeMillis();
            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                sink.flush();

                this.listener.transferred(read);
                bytesReadBandwidth += read;
                if (bytesReadBandwidth >= Consts.UPLOAD_LIMIT_BANDWIDTH_BYTE&&Consts.loginAnkiChina()) {
                    long offsetTime = System.currentTimeMillis() - lastTime;
                    if (offsetTime < 1000L) {
                        try {
                            if (offsetTime < 0) offsetTime = 0;
                            Thread.sleep(1000L - offsetTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    lastTime = System.currentTimeMillis();
                    bytesReadBandwidth = 0;
                }
            }
        } finally {
            Util.closeQuietly(source);
        }
    }

    public interface ProgressListener {
        void transferred(long num);
    }
}