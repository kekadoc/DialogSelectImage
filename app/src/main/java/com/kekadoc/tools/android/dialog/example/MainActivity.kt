package com.kekadoc.tools.android.dialog.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.kekadoc.tools.android.dialog.DialogSelectImage
import com.kekadoc.tools.android.dialog.example.AvatarsProvider.EMPTY_INDEX

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG: String = "MainActivity-TAG"
        const val KEY_BUTTON_EXPANDED = "ButtonExpanded"
        const val KEY_IMAGE_SELECTED = "ImageSelected"
    }

    var editTextMin: EditText? = null
    var editTextMax: EditText? = null
    var editTextCount: EditText? = null
    var editCustomUrl: EditText? = null
    var imageView: ImageView? = null
    var button: ExtendedFloatingActionButton? = null
    var buttonCustomUrl: Button? = null

    private var selectedIndex = EMPTY_INDEX

    private fun getFragmentInputData(): Bundle {
        return DialogSelectImage.putParameters(bundleOf(), DialogSelectImage.Parameters(
                editTextMin!!.text.toString().toInt(),
                editTextMax!!.text.toString().toInt(),
                editTextCount!!.text.toString().toInt(),
                //selected = intArrayOf(1, 2, 3),
                //disabled = intArrayOf(1, 4, 5)
        ))
    }

    private fun createDialogSelectImage(): DialogSelectAvatar {
        supportFragmentManager.setFragmentResult(DialogSelectImage.requestInputKey, getFragmentInputData())
        return DialogSelectAvatar()
    }

    private fun showFragmentForResultImage() {
        val dialog = createDialogSelectImage()
        dialog.show(supportFragmentManager, null)
    }

    private fun loadImage(index: Int) {
        selectedIndex = index
        if (index == -1) imageView?.setImageResource(R.drawable.ic_baseline_account_circle_24)
        else imageView?.load(AvatarsProvider.getImageUrl(index)) {
            transformations(CircleCropTransformation())
            error(R.drawable.ic_baseline_error_outline_24)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextMin = findViewById(R.id.editText_min)
        editTextMax = findViewById(R.id.editText_max)
        editCustomUrl = findViewById(R.id.editText_customUrl)
        editTextCount = findViewById(R.id.editText_count)
        imageView = findViewById(R.id.imageView)
        buttonCustomUrl = findViewById<Button>(R.id.button_addCustomUrl).apply {
            setOnClickListener {
                val url = editCustomUrl!!.text.toString()
                if (url.isNotEmpty()) {
                    AvatarsProvider.addCustomUrl(url)
                    editCustomUrl!!.text.clear()
                }
            }
        }
        loadImage(savedInstanceState?.getInt(KEY_IMAGE_SELECTED, EMPTY_INDEX) ?: EMPTY_INDEX)
        button = findViewById<ExtendedFloatingActionButton>(R.id.floatingActionButton).apply {
            val expanded = savedInstanceState?.getBoolean(KEY_BUTTON_EXPANDED, false) ?: false
            if (expanded) extend()
            else shrink()
            setOnClickListener {
                showFragmentForResultImage()
            }
            setOnLongClickListener {
                if (isExtended) shrink()
                else extend()
                true
            }
        }

        findViewById<SwitchMaterial>(R.id.switchView).apply {
            val mode = AppCompatDelegate.getDefaultNightMode()
            isChecked = mode == AppCompatDelegate.MODE_NIGHT_YES

            setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

        }

        supportFragmentManager.setFragmentResultListener(
            DialogSelectImage.requestResultKey,
            this, { requestKey, result ->
                DialogSelectImage.handleResult(result, DialogSelectImage.OnResultListener.create(
                    onSingle = { loadImage(it) },
                    onMulti = { loadImage(if (it.isEmpty()) EMPTY_INDEX else it.first()) },
                    onEmpty = { loadImage(EMPTY_INDEX) },
                    onError = { loadImage(EMPTY_INDEX) }
                ))
            }
        )

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        button?.let {
            outState.putBoolean(KEY_BUTTON_EXPANDED, it.isExtended)
        }
        outState.putInt(KEY_IMAGE_SELECTED, selectedIndex)
    }

}