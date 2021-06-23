package com.kekadoc.tools.android.dialog.example

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.load
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.kekadoc.tools.android.ThemeColor
import com.kekadoc.tools.android.animation.animator
import com.kekadoc.tools.android.animation.scale
import com.kekadoc.tools.android.fragment.dpToPx
import com.kekadoc.tools.android.fragment.themeColor
import com.kekadoc.tools.android.dialog.DialogSelectImage
import com.kekadoc.tools.data.Offer
import kotlinx.coroutines.*

class DialogSelectAvatar : DialogSelectImage() {

    companion object {
        private const val TAG: String = "DialogSelectAvatar-TAG"
    }

    init {
        setImageStateHandler(object : BaseImageHolderStateHandler() {
            override fun onStateChange(
                view: ImageViewHolder,
                oldState: ImageState,
                newState: ImageState
            ) {
                super.onStateChange(view, oldState, newState)
            }
            override fun onSelected(view: ImageViewHolder) {
                super.onSelected(view)
                view.strokeWidth = dpToPx(3f).toInt()
                view.strokeColor = themeColor(ThemeColor.SECONDARY_VARIANT)
            }
            override fun onNormal(view: ImageViewHolder) {
                super.onNormal(view)
                view.strokeWidth = 0
                view.strokeColor = themeColor(ThemeColor.SECONDARY)
            }
            override fun onHide(view: ImageViewHolder) {
                super.onHide(view)
                view.strokeWidth = 0
                view.strokeColor = 0
            }
            override fun onDisable(view: ImageViewHolder) {
                super.onDisable(view)
                view.strokeWidth = dpToPx(3f).toInt()
                view.strokeColor = themeColor(ThemeColor.ERROR)
            }
        })

        title {
            setText(R.string.dialog_select_avatar_title)
        }
        action {
            setOnLongClickListener {
                selection.getAll().filter { it.state.isDisabled() }.forEach { it.state = ImageState.NORMAL }
                true
            }
        }
    }

    override fun onBindImagePreview(imagePreview: ShapeableImageView, index: Int) {
        super.onBindImagePreview(imagePreview, index)
        imagePreview.animator {
            scale(fromX = 0f, fromY = 0f, toX = 1f, toY = 1f) {
                duration(200L)
                interpolator(OvershootInterpolator(1f))
            }
        }.start()
    }

    override fun onBindCounterView(counterView: TextView, count: Int) {
        super.onBindCounterView(counterView, count)
        counterView.animator {
            scale(fromX = 0f, fromY = 0f, toX = 1f, toY = 1f) {
                duration(200L)
                interpolator(OvershootInterpolator(1f))
            }
        }.start()
    }

    override fun onAttachParameters(params: Offer<Parameters?>) {
        if (params.get() == null) return super.onAttachParameters(params)

        val parameters: Parameters = params.get()!!

        if (parameters.count == 0) {
            message {
                imageView.setImageResource(R.drawable.ic_dialog_select_image_error_default)
                textView.setText(R.string.dialog_select_image_error_empty)
            }.show()
            params.accept()
        } else if (parameters.count < 0) {
            message {
                imageView.setImageResource(R.drawable.ic_dialog_select_image_error_default)
                textView.setText(R.string.dialog_select_image_error_data)
            }.show()
            params.accept()
        } else {
            val indicator = indicator().apply { show() }
            val message = message {
                imageView.setImageResource(R.drawable.ic_baseline_cloud_download_56)
                textView.setText(R.string.loading)
            }.apply { show() }

            lifecycleScope.launch(Dispatchers.IO) {
                val loader = Coil.imageLoader(requireContext())
                (0 until params.get()!!.count).map {
                    async {
                        val request = ImageRequest.Builder(requireContext())
                                .data(AvatarsProvider.getImageUrl(it))
                                .lifecycle(this@DialogSelectAvatar)
                                .build()
                        loader.execute(request)
                    }
                }.awaitAll()
                withContext(Dispatchers.Main) {
                    params.accept()
                    indicator.hide()
                    message.hide()
                }

            }.start()
        }

    }

    override fun onBindImage(imageView: ImageView, index: Int) {
        imageView.load(AvatarsProvider.getImageUrl(index)) {
            placeholder(R.drawable.ic_baseline_cloud_download_56)
            crossfade(true)
            error(R.drawable.ic_baseline_error_outline_24)
            crossfade(100)
            transformations(CircleCropTransformation())
            listener(onError = {_, error ->
                selection.disable(index)
            })
        }
    }

    override fun onImageHolderViewCreated(imageHolderView: ImageViewHolder) {
        super.onImageHolderViewCreated(imageHolderView)

        val shape = ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(RelativeCornerSize(0.10f))
            .build()

        imageHolderView.shapeAppearanceModel = shape

        imageHolderView.setOnLongClickListener {
            selection.disable(imageHolderView.index)
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 2
            orientation = RecyclerView.HORIZONTAL
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            spanCount = 3
            orientation = RecyclerView.VERTICAL
        }
    }

    override fun onResultEmpty(empty: Offer<Unit>) {
        Log.e(TAG, "onResultEmpty: ${empty.get()}")
        super.onResultEmpty(empty)
    }
    override fun onResultImage(index: Offer<Int>) {
        Log.e(TAG, "onResultImage: ${index.get()}")
        super.onResultImage(index)
    }
    override fun onResultImageArray(array: Offer<IntArray>) {
        Log.e(TAG, "onResultImageArray: ${array.get().contentToString()}")
        super.onResultImageArray(array)
    }

}