package com.kekadoc.tools.android.dialog

import android.animation.Animator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.kekadoc.tools.android.*
import com.kekadoc.tools.android.animation.alpha
import com.kekadoc.tools.android.animation.animator
import com.kekadoc.tools.data.state.DataStatesCollector
import com.kekadoc.tools.data.state.StateKeeper
import com.kekadoc.tools.exeption.Wtf
import com.kekadoc.tools.ui.content.Content
import java.util.*
import kotlin.collections.HashSet
import androidx.lifecycle.LifecycleOwner
import androidx.fragment.app.FragmentManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.MaterialShapeUtils
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.kekadoc.tools.android.annotation.FractionValue
import com.kekadoc.tools.android.graph.drawing.DrawingCircle
import com.kekadoc.tools.android.shaper.corners.RelativeCornerSize
import com.kekadoc.tools.android.view.ViewUtils.setProgress
import com.kekadoc.tools.data.Offer
import com.kekadoc.tools.android.view.dialogselectimage.R
import com.kekadoc.tools.android.view.dpToPx
import com.kekadoc.tools.android.view.themeColor
import com.kekadoc.tools.fraction.Fraction
import com.kekadoc.tools.ifTrue
import com.kekadoc.tools.observable.MutableData
import com.kekadoc.tools.observable.ObservableData
import com.kekadoc.tools.observable.Observing
import com.kekadoc.tools.observable.observer
import kotlin.math.abs

/**
 * Result:
 * use [FragmentManager.setFragmentResultListener] with key [requestResultKey]
 * use [handleResult] for handle bundle result
 *
 * fragmentManager.setFragmentResultListener([requestResultKey], [LifecycleOwner], { key, result ->
 *      DialogSelectImage.handleResult(result, DialogSelectImage.OnResultListener.create(
 *          onSingle = { ... },
 *          onMulti = { ... },
 *          onEmpty = { ... },
 *          onError = { ... }
 *      ))
 *   })
 *
 * Input params:
 * use [Selection.parameters]
 * use [setArguments] and [putParameters]
 * use [FragmentManager.setFragmentResult] with key - [requestInputKey] and [putParameters]
 *
 */
open class DialogSelectImage : BottomSheetDialogFragment() {

    companion object {
        private const val TAG: String = "DialogSelectImage-TAG"
        const val EMPTY_INDEX = -1

        private const val RESULT_TYPE_EMPTY = -1
        private const val RESULT_TYPE_SINGLE = 1
        private const val RESULT_TYPE_MULTI = 2

        private val keyParameters = DialogSelectImage::class.simpleName + "Parameters"
        private val keyResultType = DialogSelectImage::class.simpleName + "ResultType"
        private val keyResultData = DialogSelectImage::class.simpleName + "ResultData"

        @JvmStatic
        val requestInputKey = DialogSelectImage::class.simpleName + "RequestInputKey"
        @JvmStatic
        val requestResultKey = DialogSelectImage::class.simpleName + "RequestResultKey"

        fun putParameters(bundle: Bundle, parameters: Parameters?): Bundle {
            bundle.putParcelable(keyParameters, parameters)
            return bundle
        }

        fun findParameters(bundle: Bundle?): Parameters? {
            return bundle?.getParcelable(keyParameters)
        }

        fun handleResult(result: Bundle, listener: OnResultListener) {
            val invalid = -10
            val type = result.getInt(keyResultType, invalid)
            if (type == invalid) listener.onError(IllegalStateException("Invalid result"))
            else {
                when(type) {
                    RESULT_TYPE_EMPTY -> listener.onEmptyResult()
                    RESULT_TYPE_SINGLE -> {
                        val index = result.getInt(keyResultData, invalid)
                        if (index == invalid) listener.onError(IllegalStateException("Invalid single result"))
                        else listener.onSingleResult(index)
                    }
                    RESULT_TYPE_MULTI -> {
                        val array = result.getIntArray(keyResultData)
                        if (array == null) listener.onError(IllegalStateException("Invalid multi result"))
                        else listener.onMultiResult(array)
                    }
                    else -> listener.onError(IllegalStateException("Invalid result type"))
                }
            }
        }

    }

    /**
     * @see BaseImageHolderStateHandler
     * @see setImageStateHandler
     */
    interface ImageStateHandler {
        fun onStateChange(view: ImageViewHolder, oldState: ImageState, newState: ImageState)
    }
    /**
     * @see handleResult
     */
    interface OnResultListener {

        companion object {
            fun create(
                    onEmpty: (() -> Unit)? = null,
                    onSingle: ((index: Int) -> Unit)? = null,
                    onMulti: ((array: IntArray) -> Unit)? = null,
                    onError: ((throwable: Throwable) -> Unit)? = null
            ): OnResultListener {
                return object : OnResultListener {
                    override fun onEmptyResult() {
                        onEmpty?.invoke()
                    }
                    override fun onSingleResult(index: Int) {
                        onSingle?.invoke(index)
                    }
                    override fun onMultiResult(array: IntArray) {
                        onMulti?.invoke(array)
                    }
                    override fun onError(throwable: Throwable) {
                        onError?.invoke(throwable)
                    }
                }
            }
        }

        fun onEmptyResult()
        fun onSingleResult(index: Int)
        fun onMultiResult(array: IntArray)
        fun onError(throwable: Throwable)

    }

    /**
     *
     */
    interface Operator {

        val min: Int
        val max: Int
        val count: Int
        val countSelected: Int
        val selected: Set<Int>

        var parameters: Parameters?

        fun action(index: Int)
        fun select(index: Int): Boolean
        fun unselect(index: Int): Boolean
        fun disable(index: Int): Boolean
        fun enable(index: Int): Boolean

    }

    protected var spanCount = 3
        set(value) {
            field = value
            layoutManager?.spanCount = field
        }
    protected var orientation = RecyclerView.VERTICAL
        set(value) {
            field = value
            layoutManager?.orientation = field
        }

    /**
     * Show selected image icon in title.
     * Work only with single selected image!
     */
    protected var isShowSelectedImageIcon = true
    /**
     * Show selected image counter in title.
     * Work only with several selected image!
     */
    protected var isShowSelectedImageCounter = true

    protected var isShowActionIndicator = true
        get() = contentCounter.view?.indicator ?: field
        set(value) {
            field = value
            contentAction.view?.indicator = value
        }
    protected var isShowCounterIndicator = true
        get() = contentCounter.view?.indicator ?: field
        set(value) {
            field = value
            contentCounter.view?.indicator = value
        }

    protected var isDismissOnResultHandled = true


    private val data = ImageData()
    private val adapter: Adapter = Adapter()
    protected val selection = Selection(data)

    private var layoutManager: GridLayoutManager? = null
    private val imageStateHandler = ImageStateHandlerWrapper(BaseImageHolderStateHandler())

    private var contentTitleText = ContentTitle()
    private var contentImagePreview = ContentImagePreview()
    private var contentCounter = ContentCounter()
    private var contentAction = ContentAction()
    private var contentIndicator = ContentIndicator()
    private var contentImages = ContentImages()
    private var contentMessage = ContentMessage()

    protected open fun onAttachParameters(params: Offer<Parameters?>) {
        params.accept()
    }

    protected open fun onImageHolderViewCreated(imageHolderView: ImageViewHolder) {
        imageHolderView.setOnClickListener {
            selection.action(imageHolderView.index)
        }
    }
    protected open fun onBindImageHolderView(imageHolderView: ImageViewHolder, index: Int) {}
    protected open fun onBindImage(imageView: ImageView, index: Int) {}

    protected open fun onBindImagePreview(imagePreview: ShapeableImageView, index: Int) {
        onBindImage(imagePreview, index)
    }
    protected open fun onBindCounterView(counterView: TextView, count: Int) {
        counterView.text = count.toString()
    }

    protected open fun onResultEmpty(empty: Offer<Unit>) {
        empty.accept()
    }
    protected open fun onResultImage(index: Offer<Int>) {
        index.accept()
    }
    protected open fun onResultImageArray(array: Offer<IntArray>) {
        array.accept()
    }

    protected open fun isImagesResultReady() = true

    protected fun setImageStateHandler(handler: ImageStateHandler?) {
        imageStateHandler.delegate = handler
    }

    protected fun indicator(builder: (LinearProgressIndicator.() -> Unit)? = null): Content.Progress {
        contentIndicator.builder = builder
        return contentIndicator
    }
    protected fun message(builder: (MessageView.() -> Unit)? = null): Content {
        if (builder == null) contentMessage.builder = null
        else contentMessage.builder = {
            builder.invoke(contentMessage.messageView!!)
        }
        return contentMessage
    }
    protected fun title(builder: TextView.() -> Unit) {
        contentTitleText.builder = builder
    }
    protected fun counter(builder: TextView.() -> Unit) {
        contentCounter.builder = builder
    }
    protected fun preview(builder: ShapeableImageView.() -> Unit) {
        contentImagePreview.builder = builder
    }
    protected fun action(builder: FloatingActionButton.() -> Unit) {
        contentAction.builder = builder
    }

    protected fun isReady() = data.countSelected in data.min..data.max && isImagesResultReady()

    protected fun handleResult() {
        val selected = selection.selected
        if (selected.size > selection.max) throw Wtf()
        if (isEmptySelect() || selected.isEmpty()) {
            val data = bundleOf(
                    keyResultType to RESULT_TYPE_EMPTY,
                    keyResultData to 0
            )
            setFragmentResult(requestResultKey, data)
            onResultEmpty(object : Offer.Instance<Unit>(Unit) {
                override fun onAccept() {
                    dismiss()
                }
            })
        } else if (isSingleSelect()) {
            if (selected.size > 1) throw Wtf()
            val index = selected.first()
            val data = bundleOf(
                    keyResultType to RESULT_TYPE_SINGLE,
                    keyResultData to index
            )
            setFragmentResult(requestResultKey, data)
            onResultImage(object : Offer.Instance<Int>(index) {
                override fun onAccept() {
                    dismiss()
                }
            })
        } else if (isMultiSelect()) {
            val array = selected.toIntArray()
            array.sort()
            val data = bundleOf(
                    keyResultType to RESULT_TYPE_MULTI,
                    keyResultData to array
            )
            setFragmentResult(requestResultKey, data)
            onResultImageArray(object : Offer.Instance<IntArray>(array) {
                override fun onAccept() {
                    dismiss()
                }
            })
        }

        if (isDismissOnResultHandled) dismiss()
    }

    protected fun isEmptySelect(): Boolean {
        return selection.count == 0 || selection.max == 0
    }
    protected fun isSingleSelect() = selection.max == 1
    protected fun isMultiSelect() = selection.max > 1

    private fun notifyTitleContent(contentVisible: Boolean) {
        if (contentVisible) {
            if (isSingleSelect() && isShowSelectedImageIcon) contentImagePreview.show()
            else contentImagePreview.hide()
            if (isMultiSelect() && isShowSelectedImageCounter) contentCounter.show()
            else contentCounter.hide()
            if (selection.count > 0) contentAction.show()
            else contentAction.hide()
        } else {
            contentCounter.hide()
            contentImagePreview.hide()
            contentAction.hide()
        }
    }

    private fun updateMessageViewPosition(bottomSheet: View) {
        val s = bottomSheet.height - contentMessage.view!!.height
        contentMessage.messageView!!.translationY = -(bottomSheet.y / 2) - (s / 2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) findParameters(savedInstanceState)?.let { selection.parameters = it }
        else findParameters(arguments)?.let { selection.parameters = it }

        setFragmentResultListener(requestInputKey) { _: String, bundle: Bundle ->
            findParameters(bundle)?.let { selection.parameters = it }
        }

    }
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_select_image, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentTitleText.view = view.findViewById(R.id.textView_title)
        contentImages.view = view.findViewById(R.id.recyclerView)
        contentMessage.view = view.findViewById(R.id.frameLayout_message)
        contentImagePreview.view = view.findViewById(R.id.selectedImage)
        contentCounter.view = view.findViewById(R.id.counterView)
        contentAction.view = view.findViewById(R.id.buttonSelectImage)
        contentIndicator.view = view.findViewById(R.id.progressIndicator)

        val bottomSheetView = view.parent as View
        val lp = bottomSheetView.layoutParams
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheetView.viewTreeObserver.addOnGlobalLayoutListener {
            updateMessageViewPosition(bottomSheetView)
        }
        val behavior = BottomSheetBehavior.from(bottomSheetView)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                updateMessageViewPosition(bottomSheet)
                if (slideOffset <= 0f) {
                    contentMessage.view!!.alpha = 1.0f - (abs(slideOffset) * 1.5f)
                }

            }
        })

        data.parametersData.observe(observer {
            notifyTitleContent(!contentMessage.isShown())
        })

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val params = data.parameters?.let { parameters ->
            Parameters(
                    parameters.min,
                    parameters.max,
                    parameters.count,
                    data.selected.toIntArray(),
                    data.getAll().filter { it.state.isDisabled() }.map { it.data }.toIntArray()
            )
        }
        outState.putParcelable(keyParameters, params)
    }

    private abstract class AbsContentView <V : View> (shown: Boolean) : Content.SimpleInstance(shown) {

        var builder: (V.() -> Unit)? = null
            set(value) {
                field = value
                if (view != null && builder != null) {
                    builder!!.invoke(view!!)
                    field = null
                }
            }

        var view: V? = null
            set(value) {
                field = value
                if (value != null) {
                    onViewAttach(value)
                    onViewApply(value)
                }
            }

        protected open fun onViewAttach(view: V) {
            if (isShown()) onShown()
            else onHide()
            if (builder != null) builder!!.invoke(view)
        }
        protected open fun onViewApply(view: V) {}

        override fun onHide() {
            view?.isVisible = false
        }
        override fun onShown() {
            view?.isVisible = true
        }

    }

    private class ContentTitle : AbsContentView<TextView>(true)

    private class ContentIndicator : AbsContentView<LinearProgressIndicator>(false), Content.Progress {

        var currentFraction: Double = 0.0

        override fun getFraction(): Double = currentFraction
        override fun setFraction(fraction: Double) {
            currentFraction = fraction
            if (view != null) onViewApply(view!!)
        }

        override fun onViewApply(view: LinearProgressIndicator) {
            view.setProgress(currentFraction.toFloat())
        }

        override fun onHide() {
            view?.hide()
        }
        override fun onShown() {
            view?.show()
        }

    }

    private inner class ContentCounter : AbsContentView<CounterView>(false) {

        private var count: Int = 0
            set(value) {
                field = value
                view?.apply { onViewApply(this)}
            }

        init {
            data.selection.observe(observer { count = it.selected })
        }

        override fun onViewAttach(view: CounterView) {
            super.onViewAttach(view)
            view.indicator = isShowCounterIndicator
        }
        override fun onViewApply(view: CounterView) {
            onBindCounterView(view, count)
            if (isMultiSelect()) view.setProgress(count.toDouble() / data.max.toDouble(), view.isShown)
        }

    }
    private inner class ContentImagePreview : AbsContentView<ShapeableImageView>(false) {

        private var index: Int = EMPTY_INDEX
            set(value) {
                field = value
                view?.let { onViewApply(it) }
            }

        init {
            data.lastSelected.observe(observer { index = it })
        }

        override fun onViewApply(view: ShapeableImageView) {
            super.onViewApply(view)
            if (isShown()) onBindImagePreview(view, index)
        }
        override fun onShown() {
            super.onShown()
            view?.let { onBindImage(it, index) }
        }

    }
    private inner class ContentAction : AbsContentView<ActionView>(false) {

        private var count: Int = 0
            set(value) {
                field = value
                view?.let { onViewApply(it) }
            }

        init {
            data.selection.observe(observer {
                count = it.selected
            })
        }

        override fun onViewAttach(view: ActionView) {
            super.onViewAttach(view)
            view.apply {
                indicator = isShowActionIndicator
                setOnClickListener {
                    handleResult()
                }
            }
        }
        override fun onViewApply(view: ActionView) {
            if (isMultiSelect())
                view.setProgress(count.toDouble() / data.min.toDouble(), view.isShown)
            view.isEnabled = isReady()
        }

    }
    private inner class ContentImages : AbsContentView<RecyclerView>(true) {

        override fun onViewAttach(view: RecyclerView) {
            super.onViewAttach(view)
            view.apply {
                this@DialogSelectImage.layoutManager = GridLayoutManager(
                        view.context,
                        spanCount,
                        orientation,
                        false
                )
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                            outRect: Rect,
                            view: View,
                            parent: RecyclerView,
                            state: RecyclerView.State
                    ) {
                        val size = parent.context.dpToPx(6f).toInt() // TODO: 06.03.2021
                        outRect.set(size, size, size, size)
                    }
                })
                layoutManager = this@DialogSelectImage.layoutManager
                adapter = this@DialogSelectImage.adapter
            }
        }

    }
    private inner class ContentMessage : AbsContentView<FrameLayout>(false) {

        var messageView: MessageView? = null

        override fun onHide() {
            super.onHide()
            notifyTitleContent(true)
        }
        override fun onShown() {
            super.onShown()
            notifyTitleContent(false)
        }

        override fun onViewAttach(view: FrameLayout) {
            messageView = view.findViewById(R.id.messageView)
            super.onViewAttach(view)
        }

    }

    @MainThread
    inner class Selection(private val operator: Operator): Operator by operator {

        override var parameters: Parameters?
            get() = operator.parameters
            set(value) {
                onAttachParameters(ParametersOffer(value))
            }

        fun getAll(): Collection<ImageHolder> = data.getAll()

        private inner class ParametersOffer(private val params: Parameters?) : Offer<Parameters?> {

            private var reviewed = false

            override fun get(): Parameters? = params
            override fun accept() {
                operator.parameters = params
                reviewed = true
            }
            override fun reject() {
                reviewed = true
            }
            override fun isReviewed(): Boolean = reviewed
        }

    }

    data class Parameters(val min: Int = 0,
                          val max: Int = 0,
                          val count: Int = 0,
                          val selected: IntArray? = null,
                          val disabled: IntArray? = null) : Parcelable {

        constructor(parcel: Parcel) : this(
                parcel.readInt(),
                parcel.readInt(),
                parcel.readInt(),
                parcel.createIntArray(),
                parcel.createIntArray())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(min)
            parcel.writeInt(max)
            parcel.writeInt(count)
            parcel.writeIntArray(selected)
            parcel.writeIntArray(disabled)
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Parameters

            if (min != other.min) return false
            if (max != other.max) return false
            if (count != other.count) return false
            if (selected != null) {
                if (other.selected == null) return false
                if (!selected.contentEquals(other.selected)) return false
            } else if (other.selected != null) return false
            if (disabled != null) {
                if (other.disabled == null) return false
                if (!disabled.contentEquals(other.disabled)) return false
            } else if (other.disabled != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = min
            result = 31 * result + max
            result = 31 * result + count
            result = 31 * result + (selected?.contentHashCode() ?: 0)
            result = 31 * result + (disabled?.contentHashCode() ?: 0)
            return result
        }

        companion object CREATOR : Parcelable.Creator<Parameters> {
            override fun createFromParcel(parcel: Parcel): Parameters {
                return Parameters(parcel)
            }

            override fun newArray(size: Int): Array<Parameters?> {
                return arrayOfNulls(size)
            }
        }
    }

    private class ImageStateHandlerWrapper(var delegate: ImageStateHandler?): ImageStateHandler {
        override fun onStateChange(view: ImageViewHolder, oldState: ImageState, newState: ImageState) {
            delegate?.onStateChange(view, oldState, newState)
        }
    }
    open class BaseImageHolderStateHandler : ImageStateHandler {

        override fun onStateChange(view: ImageViewHolder, oldState: ImageState, newState: ImageState) {
            when (newState) {
                ImageState.NORMAL -> onNormal(view)
                ImageState.SELECTED -> onSelected(view)
                ImageState.HIDDEN -> onHide(view)
                ImageState.DISABLE -> onDisable(view)
            }
        }
        protected open fun onSelected(view: ImageViewHolder) {
            if (view.isShown) {
                view.animator().alpha(to = 1f) { duration(200L) }.start()
            } else view.alpha = 1f
            view.isChecked = true
            view.isEnabled = true
        }
        protected open fun onNormal(view: ImageViewHolder) {
            if (view.isShown) {
                view.animator().alpha(to = 1f) { duration(200L) }.start()
            } else view.alpha = 1f
            view.isChecked = false
            view.isEnabled = true
        }
        protected open fun onHide(view: ImageViewHolder) {
            if (view.isShown) {
                view.animator().alpha(to = 0.7f) { duration(200L) }.start()
            } else view.alpha = 0.66f
            view.isChecked = false
            view.isEnabled = true
        }
        protected open fun onDisable(view: ImageViewHolder) {
            if (view.isShown) {
                view.animator().alpha(to = 0.5f) { duration(200L) }.start()
            } else view.alpha = 0.33f
            view.isChecked = false
            view.isEnabled = false
        }

    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        init {
            data.selection.observe(observer {
                itemsCount = it.count
            })
        }

        var itemsCount = 0
            set(value) {
                if (field == value) return
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = ImageViewHolder(parent.context)
            onImageHolderViewCreated(view)
            return ViewHolder(view, imageStateHandler)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val stateKeeper = data.getKeeperOrCreate(position)
            holder.view._index = stateKeeper.data
            holder.stateKeeper = stateKeeper
            onBindImageHolderView(holder.view, position)
            onBindImage(holder.view.imageView, position)
        }

        override fun getItemCount(): Int {
            return itemsCount
        }

    }
    private class ViewHolder(view: ImageViewHolder,
                             private val listener: ImageStateHandler
    ) : RecyclerView.ViewHolder(view) {

        val view
            get() = itemView as ImageViewHolder

        var stateKeeper: StateKeeper<Int, ImageState>? = null
            set(value) {
                field = value
                view._index = value?.data ?: EMPTY_INDEX
                if (observing != null) observing!!.remove()
                field?.let {
                    observing = it.observe { oldState: ImageState, newState: ImageState ->
                        listener.onStateChange(view, oldState, newState)
                    }
                }
            }

        private var observing: Observing? = null

    }

    enum class ImageState {
        SELECTED, NORMAL, HIDDEN, DISABLE;
        fun isSelected() = this == SELECTED
        fun isNormal() = this == NORMAL
        fun isHidden() = this == HIDDEN
        fun isDisabled() = this == DISABLE
    }

    class ImageHolder(t: Int, initState: ImageState) : StateKeeper.Default<Int, ImageState>(t, initState) {
        override fun toString(): String {
            return "StateKeeperImpl(data=$data, state=$state)"
        }
    }

    private class ImageData : DataStatesCollector<Int, ImageState, ImageHolder>(), Operator {

        interface SelectionParams {

            val min: Int
            val max: Int
            val count: Int
            val selected: Int

        }
        private class SelectionParamsImpl : SelectionParams {

            override val min: Int
                get() = minValue
            override val max: Int
                get() = maxValue
            override val count: Int
                get() = countValue
            override val selected: Int
                get() = selectedValue

            var minValue: Int = 0
            var maxValue: Int = 0
            var countValue: Int = 0
            var selectedValue: Int = 0

            override fun toString(): String {
                return "SelectionParamsImpl(min=$min, max=$max, count=$count, selected=$selected)"
            }

        }

        val selection: ObservableData<SelectionParams>
            get() = selectionData
        val parametersData: ObservableData<Parameters?>
            get() = _parametersData
        val lastSelected: ObservableData<Int>
            get() = _lastSelected

        override var parameters: Parameters?
            get() = _parametersData.getValue()
            set(value) {
                _parametersData.setValue(value)
            }

        override val min: Int
            get() = selectionParams.min
        override val max: Int
            get() = selectionParams.max
        override val count: Int
            get() = selectionParams.count
        override val countSelected: Int
            get() = selectedKeepers.size
        override val selected: Set<Int>
            get() {
                return selectedKeepers.map { it.data }.toSet()
            }

        private val selectionParams = SelectionParamsImpl()
        private val selectionData = MutableData<SelectionParams>(selectionParams)
        private val _parametersData = object : MutableData<Parameters?>(null) {
            override fun onChange(oldValue: Parameters?, newValue: Parameters?) {
                clearSelected()
                clear()
                notifySelectionParams()
                newValue?.disabled?.let { disabled ->
                    disabled.forEach { disable(it) }
                }
                newValue?.selected?.let { selected ->
                    selected.forEach { select(it) }
                }
            }
        }

        private val selectedKeepers: MutableSet<ImageHolder> = HashSet()

        private val _lastSelected: MutableData<Int> = MutableData(EMPTY_INDEX)

        fun getKeeperOrCreate(position: Int): StateKeeper<Int, ImageState> {
            var stateKeeper = getStateKeeper(position)
            if (stateKeeper != null) return stateKeeper
            stateKeeper = add(position, getDefaultState(position))

            return stateKeeper
        }

        private fun notifySelectionParams() {
            selectionParams.minValue = parametersData.getValue()?.min ?: 0
            selectionParams.maxValue = parametersData.getValue()?.max ?: 0
            selectionParams.countValue = parametersData.getValue()?.count ?: 0
            selectionParams.selectedValue = selectedKeepers.size
            selectionData.notifyValue()
        }

        private fun clearSelected() {
            selectedKeepers.forEach { removeFocus(it) }
            selectedKeepers.clear()
        }
        private fun hideAllNormal() {
            for (keeper in getAll()) {
                if (keeper.state == ImageState.NORMAL) keeper.state = ImageState.HIDDEN
            }
        }
        private fun showAllHided() {
            for (keeper in getAll()) if (keeper.state == ImageState.HIDDEN) keeper.state =
                ImageState.NORMAL
        }

        private fun onFocused() {
            hideAllNormal()
        }
        private fun onUnfocused() {
            showAllHided()
        }

        private fun getDefaultState(index: Int): ImageState {
            return ImageState.NORMAL
        }

        override fun action(index: Int) {
            val keeper = getKeeperOrCreate(index)
            if (keeper.state == ImageState.DISABLE) return
            if (keeper.state == ImageState.SELECTED) keeper.state = ImageState.NORMAL
            else {
                if (selectedKeepers.size < selection.getValue().max)
                    keeper.state = ImageState.SELECTED else if (isFocused() && selection.getValue().max == 1) {
                    selectedKeepers.first().state = ImageState.NORMAL
                    keeper.state = ImageState.SELECTED
                }
            }
        }
        override fun select(index: Int): Boolean {
            val keeper = getKeeperOrCreate(index)
            if (keeper.state == ImageState.DISABLE) return false
            if (keeper.state == ImageState.SELECTED) return false

            if (selectedKeepers.size < selection.getValue().max) {
                keeper.state = ImageState.SELECTED
                return true
            } else if (isFocused() && selection.getValue().max == 1) {
                selectedKeepers.first().state = ImageState.NORMAL
                keeper.state = ImageState.SELECTED
                return true
            }
            return false
        }
        override fun unselect(index: Int): Boolean {
            val keeper = getKeeperOrCreate(index)
            if (keeper.state == ImageState.SELECTED) {
                keeper.state = ImageState.NORMAL
                return true
            }
            return false
        }
        override fun enable(index: Int): Boolean {
            val keeper = getKeeperOrCreate(index)
            if (keeper.state == ImageState.DISABLE) {
                keeper.state = ImageState.NORMAL
                return true
            }
            return false
        }
        override fun disable(index: Int): Boolean {
            val keeper = getKeeperOrCreate(index)
            if (keeper.state == ImageState.DISABLE) return false
            if (keeper.state == ImageState.SELECTED) {
                if (unselect(index)) {
                    keeper.state = ImageState.DISABLE
                    return true
                }
            }
            if (keeper.state == ImageState.NORMAL) {
                keeper.state = ImageState.DISABLE
                return true
            }
            return false
        }

        fun isFocused(): Boolean = selectedKeepers.size > 0

        private fun addFocus(keeper: ImageHolder) {
            val focused = isFocused()
            selectedKeepers.add(keeper).ifTrue {
                notifySelectionParams()
                _lastSelected.updateValue(keeper.data)
            }
            if (!focused && isFocused()) onFocused()
        }
        private fun removeFocus(keeper: ImageHolder) {
            val focused = isFocused()
            selectedKeepers.remove(keeper).ifTrue {
                notifySelectionParams()
                if (keeper.data == lastSelected.getValue()) _lastSelected.setValue(EMPTY_INDEX)
            }
            if (focused && !isFocused()) onUnfocused()
        }

        override fun onDataStateChange(
            keeper: ImageHolder,
            oldState: ImageState,
            newState: ImageState
        ) {
            super.onDataStateChange(keeper, oldState, newState)
            if (newState == ImageState.SELECTED) addFocus(keeper)
            else if (oldState == ImageState.SELECTED) removeFocus(keeper)
            if (isFocused()) hideAllNormal() else showAllHided()
        }
        override fun onCreateStateKeeper(data: Int, state: ImageState): ImageHolder {
            return ImageHolder(data, state)
        }

    }


    class ImageViewHolder @JvmOverloads internal constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : MaterialCardView(context, attrs, defStyleAttr) {

        val imageView: ImageView

        internal var _index: Int = EMPTY_INDEX
        val index: Int
            get() = _index

        init {
            isCheckable = true
            isFocusable = true
            isClickable = true
            checkedIconTint = ColorStateList.valueOf(themeColor(ThemeColor.ACCENT))
            this.imageView = createImageView(context)
            addView(this.imageView)
        }

        private fun createImageView(context: Context): ImageView {
            return AppCompatImageView(context).apply {
                adjustViewBounds = true
                setPadding(dpToPx(6f).toInt())
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            var size = Math.min(measuredWidth, measuredHeight)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                size = measuredHeight
            setMeasuredDimension(size, size)
        }

        override fun setAlpha(alpha: Float) {
            super.setAlpha(alpha)
            imageView.alpha = alpha
        }

    }
    class MessageView @JvmOverloads internal constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

        companion object {
            const val DEFAULT_IMAGE_SIZE_DP = 128f
        }

        val imageView: ImageView
        val textView: TextView

        init {
            val iconSize = context.dpToPx(DEFAULT_IMAGE_SIZE_DP).toInt()
            orientation = VERTICAL
            gravity = Gravity.CENTER
            imageView = createImageView(context)
            textView = createTextView(context)
            addView(imageView, iconSize, iconSize)
            addView(textView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        fun builder(builder: (MessageView.() -> Unit)) = builder
        fun build(builder: (MessageView.() -> Unit)) {
            builder.invoke(this)
        }

        private fun createImageView(context: Context): ImageView {
            return ImageView(context).apply {
                setImageResource(R.drawable.none)
                imageTintList = ColorStateList.valueOf(themeColor(ThemeColor.ON_SURFACE))
            }
        }
        private fun createTextView(context: Context): TextView {
            return TextView(context).apply {
                setText(R.string.message)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                val dp = dpToPx(1f)
                setPadding((dp * 16f).toInt(), (dp * 16f).toInt(), (dp * 16f).toInt(), (dp * 16f).toInt())
                ellipsize = TextUtils.TruncateAt.END
                textAlignment = TEXT_ALIGNMENT_CENTER
                setTextColor(themeColor(ThemeColor.ON_SURFACE))
            }
        }

    }

    internal class ActionView @JvmOverloads internal constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : FloatingActionButton(context, attrs, defStyleAttr), Content.Progress {

        companion object {
            private const val TAG: String = "ActionView-TAG"
        }

        private var drawingCircle = DrawingCircle(RectF(), Paint().apply {
            setColor(context.themeColor(ThemeColor.ACCENT))
            this.strokeWidth = context.dpToPx(2f)
            style = Paint.Style.STROKE
            setFlags(Paint.ANTI_ALIAS_FLAG)
        })
        private var fraction: Double = 0.0
        private var animation: Animator? = null
        internal var indicator: Boolean = true

        fun setProgress(@FractionValue fraction: Double, animate: Boolean) {
            if (fraction == this.fraction) return
            animation?.cancel()
            if (animate) this.animation = this.animator {
                animate<ActionView>(
                    from = this@ActionView.fraction.toFloat(),
                    to = fraction.toFloat()) {
                    duration(200L)
                    onUpdate {
                        setFraction((it.animatedValue as Float).toDouble())
                    }
                }
            }.apply {
                start()
            }
            else setFraction(fraction)
        }

        override fun getFraction(): Double {
            return fraction
        }
        override fun setFraction(fraction: Double) {
            this.fraction = Fraction.adjustValue(fraction) {
                onMax = { isEnabled = true }
                onMin = { isEnabled = false }
                onChange = { _, newValue ->
                    drawingCircle.setSweepAngleFraction(newValue.toFloat())
                }
            }
            if (indicator) invalidate()
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            drawingCircle.bounds.set(
                0f + paddingLeft / 2,
                0f + paddingTop / 2,
                width.toFloat() - (paddingRight / 2),
                height.toFloat() - (paddingBottom / 2),
            )
        }

        override fun dispatchDraw(canvas: Canvas?) {
            super.dispatchDraw(canvas)
            if (fraction == 0.0 || fraction == 1.0 || !indicator) return
            drawingCircle.draw(canvas!!)
        }

    }
    internal class CounterView @JvmOverloads internal constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), Content.Progress {

        companion object {
            private const val TAG: String = "CounterView-TAG"
        }

        private var drawable: MaterialShapeDrawable? = null
        private var drawingCircle = DrawingCircle(RectF(), Paint().apply {
            setColor(context.themeColor(ThemeColor.SECONDARY_VARIANT))
            this.strokeWidth = context.dpToPx(2f)
            style = Paint.Style.STROKE
            setFlags(Paint.ANTI_ALIAS_FLAG)
        })

        private var fraction: Double = 0.0
        private var animation: Animator? = null

        internal var indicator: Boolean = true

        fun setTint(color: Int) {
            background.setTint(color)
        }

        init {
            drawable = MaterialShapeDrawable(
                ShapeAppearanceModel.builder()
                    .setAllCorners(RoundedCornerTreatment())
                    .setAllCornerSizes(RelativeCornerSize(0.5f))
                    .build()
            ).apply {
                initializeElevationOverlay(context)
                this.z = this@CounterView.z
            }
            background = drawable

            setTint(themeColor(ThemeColor.SECONDARY))
        }

        fun setProgress(@FractionValue fraction: Double, animate: Boolean) {
            if (fraction == this.fraction) return
            animation?.cancel()
            if (animate) this.animation = this.animator {
                animate<ActionView>(
                    from = this@CounterView.fraction.toFloat(),
                    to = fraction.toFloat()) {
                    duration(200L)
                    onUpdate {
                        setFraction((it.animatedValue as Float).toDouble())
                    }
                }
            }.apply {
                start()
            }
            else setFraction(fraction)
        }

        override fun hide() {
            isVisible = false
        }
        override fun show() {
            isVisible = true
        }

        override fun getFraction(): Double {
            return fraction
        }
        override fun setFraction(fraction: Double) {
            this.fraction = Fraction.adjustValue(fraction) {
                onChange = { _, newValue ->
                    drawingCircle.setSweepAngleFraction(newValue.toFloat())
                }
            }
            if (indicator) invalidate()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            MaterialShapeUtils.setParentAbsoluteElevation(this, drawable!!)
        }

        override fun setAlpha(alpha: Float) {
            super.setAlpha(alpha)
            drawable?.alpha = (alpha * 255).toInt()
        }

        override fun setElevation(elevation: Float) {
            super.setElevation(elevation)
            onZChanged()
        }
        override fun setTranslationZ(translationZ: Float) {
            super.setTranslationZ(translationZ)
            onZChanged()
        }
        override fun setZ(z: Float) {
            super.setZ(z)
            onZChanged()
        }

        private fun onZChanged() {
            drawable?.elevation = elevation
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            drawingCircle.bounds.set(
                0f + paddingLeft,
                0f + paddingTop,
                width.toFloat() - (paddingRight),
                height.toFloat() - (paddingBottom),
            )
        }

        override fun dispatchDraw(canvas: Canvas?) {
            super.dispatchDraw(canvas)
            if (fraction == 0.0 || !indicator) return
            drawingCircle.draw(canvas!!)
        }

    }
    internal class ImagePreview @JvmOverloads internal constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : ShapeableImageView(context, attrs, defStyleAttr) {

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(measuredHeight, measuredHeight)
        }

    }
}
