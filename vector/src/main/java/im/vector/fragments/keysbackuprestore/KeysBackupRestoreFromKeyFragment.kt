/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.fragments.keysbackuprestore

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged
import im.vector.R
import im.vector.fragments.VectorBaseFragment
import org.matrix.androidsdk.util.Log

class KeysBackupRestoreFromKeyFragment : VectorBaseFragment() {

    companion object {
        fun newInstance() = KeysBackupRestoreFromKeyFragment()

        private const val REQUEST_TEXT_FILE_GET = 1
    }

    override fun getLayoutResId() = R.layout.fragment_keys_backup_restore_from_key

    private lateinit var viewModel: KeysBackupRestoreFromKeyViewModel
    private lateinit var sharedViewModel: KeysBackupRestoreSharedViewModel

    @BindView(R.id.keys_backup_key_enter_til)
    lateinit var mKeyInputLayout: TextInputLayout
    @BindView(R.id.keys_restore_key_enter_edittext)
    lateinit var mKeyTextEdit: EditText

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(KeysBackupRestoreFromKeyViewModel::class.java)
        sharedViewModel = activity?.run {
            ViewModelProviders.of(this).get(KeysBackupRestoreSharedViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        mKeyTextEdit.setText(viewModel.recoveryCode.value)
        mKeyTextEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onNext()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        mKeyInputLayout.error = viewModel.recoveryCodeErrorText.value
        viewModel.recoveryCodeErrorText.observe(this, Observer { newValue ->
            mKeyInputLayout.error = newValue
        })

    }

    @OnTextChanged(R.id.keys_restore_key_enter_edittext)
    fun onPassphraseTextEditChange(s: Editable?) {
        s?.toString()?.let {
            viewModel.updateCode(it)
        }
    }

    @OnClick(R.id.keys_restore_button)
    fun onNext() {
        val value = viewModel.recoveryCode.value
        if (value.isNullOrBlank()) {
            viewModel.recoveryCodeErrorText.value = context?.getString(R.string.keys_backup_recovery_code_empty_error_message)
        } else {
            viewModel.recoverKeys(context!!, sharedViewModel)
        }
    }

    @OnClick(R.id.keys_backup_import)
    fun onImport() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"
        }
        if (intent.resolveActivity(activity!!.packageManager) != null) {
            startActivityForResult(intent, REQUEST_TEXT_FILE_GET)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TEXT_FILE_GET && resultCode == Activity.RESULT_OK) {
            val dataURI = data?.data
            if (dataURI != null) {
                try {
                    activity
                            ?.contentResolver
                            ?.openInputStream(dataURI)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?.let {
                                mKeyTextEdit.setText(it)
                                mKeyTextEdit.setSelection(it.length)
                            }
                } catch (e: Exception) {
                    Log.e(KeysBackupRestoreFromKeyFragment::javaClass.name, "Failed to read recovery kay from text")
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}