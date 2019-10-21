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

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.utils.datas
import com.google.zxing.Result
import xinlake.zxing.Decoder
import xinlake.zxing.view.ScannerView

class ScannerActivity : AppCompatActivity(), ScannerView.ResultHandler {
    companion object {
        private const val REQUEST_IMPORT = 2
        private const val REQUEST_IMPORT_OR_FINISH = 3

        private const val PERMISSION_NEED = android.Manifest.permission.CAMERA
        private const val CODE_REQUEST_PERMISSIONS = 10
    }

    private fun startImport(manual: Boolean = false) =
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }, if (manual) REQUEST_IMPORT else REQUEST_IMPORT_OR_FINISH)

    override fun handleResult(rawResult: Result?) {
        Profile.findAllUrls(rawResult?.text, Core.currentProfile?.first)
            .forEach { ProfileManager.createProfile(it) }
        onSupportNavigateUp()
    }

    private var vScanner: ScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 25) getSystemService<ShortcutManager>()!!.reportShortcutUsed("scan")
        if (try {
                getSystemService<CameraManager>()?.cameraIdList?.isEmpty()
            } catch (_: CameraAccessException) {
                true
            } != false) {
            startImport()
            return
        }

        setContentView(R.layout.layout_scanner)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        vScanner = findViewById(R.id.xinlake_zxing_scanner)
        vScanner!!.setResultHandler(this)

        //XinLake. Check and request the Camera permission.
        if (checkSelfPermission(PERMISSION_NEED) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(PERMISSION_NEED), CODE_REQUEST_PERMISSIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        vScanner?.startCamera()
    }

    override fun onPause() {
        super.onPause()
        vScanner?.stopCamera()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scanner_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_import_clipboard -> {
            startImport(true)
            true
        }
        else -> false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_IMPORT, REQUEST_IMPORT_OR_FINISH ->
                if (resultCode == Activity.RESULT_OK) {
                    val feature = Core.currentProfile?.first
                    var success = false

                    for (uri in data!!.datas) {
                        Decoder.DecodeQRCode(uri, contentResolver)?.let { result ->
                            Profile.findAllUrls(result.text, feature).forEach { profile ->
                                ProfileManager.createProfile(profile)
                                success = true
                            }
                        }
                    }

                    Toast.makeText(this,
                        if (success) R.string.action_import_msg else R.string.action_import_err, Toast.LENGTH_SHORT).show()
                    onSupportNavigateUp()
                } else if (requestCode == REQUEST_IMPORT_OR_FINISH) onSupportNavigateUp()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Note: It is possible that the permissions request interaction with the user is interrupted.
     * In this case you will receive empty permissions and results arrays which should be treated as a cancellation.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == CODE_REQUEST_PERMISSIONS) {
            if (checkSelfPermission(PERMISSION_NEED) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.add_profile_scanner_permission_required, Toast.LENGTH_SHORT).show()
                startImport()
            }
        }
    }
}
