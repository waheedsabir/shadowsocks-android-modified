/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.shadowsocks

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.net.TcpFastOpen
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.EditTextPreferenceModifiers
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.widget.MainListListener

class GlobalSettingsPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DataStore.publicStore
        DataStore.initGlobal()
        addPreferencesFromResource(R.xml.pref_global)

        val tfo = findPreference<SwitchPreference>(Key.tfo)!!
        tfo.isChecked = DataStore.tcpFastOpen
        tfo.setOnPreferenceChangeListener { _, value ->
            if (value as Boolean && !TcpFastOpen.sendEnabled) {
                val result = TcpFastOpen.enable()?.trim()
                if (TcpFastOpen.sendEnabled) true else {
                    (activity as MainActivity).snackbar(if (result.isNullOrEmpty()) getText(R.string.tcp_fastopen_failure) else result).show()
                    false
                }
            } else true
        }
        if (!TcpFastOpen.supported) {
            tfo.isEnabled = false
            tfo.summary = getString(R.string.tcp_fastopen_summary_unsupported, System.getProperty("os.version"))
        }

        val portProxy = findPreference<EditTextPreference>(Key.portProxy)!!
        portProxy.setOnBindEditTextListener(EditTextPreferenceModifiers.Port)
        val portLocalDns = findPreference<EditTextPreference>(Key.portLocalDns)!!
        portLocalDns.setOnBindEditTextListener(EditTextPreferenceModifiers.Port)

        val listener: (BaseService.State) -> Unit = {
            val stopped = it == BaseService.State.Stopped
            tfo.isEnabled = stopped
            portProxy.isEnabled = stopped
            portLocalDns.isEnabled = stopped
        }
        listener((activity as MainActivity).state)
        MainActivity.stateListener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.setOnApplyWindowInsetsListener(MainListListener)
    }

    override fun onDestroy() {
        MainActivity.stateListener = null
        super.onDestroy()
    }
}
