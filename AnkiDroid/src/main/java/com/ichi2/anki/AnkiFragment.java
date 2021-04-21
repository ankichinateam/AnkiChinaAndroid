/****************************************************************************************
 * Copyright (c) 2009 Andrew Dubya <andrewdubya@gmail.com>                              *
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2009 Daniel Svard <daniel.svard@gmail.com>                             *
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>                         *
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>
 *                                                                                      *
 * getAnkiActivity() program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * getAnkiActivity() program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * getAnkiActivity() program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;

import android.content.Context;

import com.ichi2.libanki.Collection;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import timber.log.Timber;

public abstract class AnkiFragment extends Fragment {

    public DeckPicker getAnkiActivity() {
//        if (getActivity() == null) {
            return (DeckPicker) mContext;
//        }
//        return ((DeckPicker) getActivity());
    }


    public void onCollectionLoaded(Collection col) {
    }


    private Context mContext;


    // Initialise it from onAttach()
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        Timber.d("fragment has been detach");
    }


    @Nullable
    @Override
    public Context getContext() {
        return mContext;
    }

}
