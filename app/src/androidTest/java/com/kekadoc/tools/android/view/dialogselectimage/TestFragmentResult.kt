package com.kekadoc.tools.android.view.dialogselectimage

import androidx.fragment.app.testing.launchFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kekadoc.tools.android.dialog.DialogSelectImage
import com.kekadoc.tools.android.dialog.example.DialogSelectAvatar
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestFragmentResult {

    companion object {
        private const val TAG: String = "TestFragmentResult-TAG"
    }

    @Test
    fun resultEmptyCorrect() {
        val data = DialogSelectImage.createInputData(0, 1, 12)
        val scenario = launchFragment<DialogSelectAvatar>(
                fragmentArgs = data,
                themeResId = R.style.Theme_MaterialComponents_DayNight_DarkActionBar
        )
        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                DialogSelectImage.requestResultKey,
                fragment,
                { _, result ->
                    DialogSelectImage.handleResult(result,
                        object : DialogSelectImage.OnResultListener {
                            override fun onEmptyResult() {
                                //Success
                            }
                            override fun onSingleResult(index: Int) {
                                error("Fail")
                            }
                            override fun onMultiResult(array: IntArray) {
                                error("Fail")
                            }
                            override fun onError(throwable: Throwable) {
                                throw throwable
                            }
                        })
                })
            fragment.parentFragmentManager.setFragmentResult(DialogSelectImage.requestInputKey, data)
        }
        onView(withId(R.id.buttonSelectImage)).perform(click())
    }

    @Test
    fun resultSingleCorrect() {
        val data = DialogSelectImage.createInputData(1, 1, 12)
        val expected = 5
        val scenario = launchFragment<DialogSelectAvatar>(
            fragmentArgs = data,
            themeResId = R.style.Theme_MaterialComponents_DayNight_DarkActionBar
        )
        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                DialogSelectImage.requestResultKey,
                fragment,
                { _, result ->
                    DialogSelectImage.handleResult(result,
                        object : DialogSelectImage.OnResultListener {
                            override fun onEmptyResult() {
                                error("Fail")
                            }
                            override fun onSingleResult(index: Int) {
                                assert(index == expected)
                            }
                            override fun onMultiResult(array: IntArray) {
                                error("Fail")
                            }
                            override fun onError(throwable: Throwable) {
                                throw throwable
                            }
                        })
                })
            fragment.parentFragmentManager.setFragmentResult(DialogSelectImage.requestInputKey, data)
        }
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(expected, click()))
        onView(withId(R.id.buttonSelectImage)).perform(click())
    }

    @Test
    fun resultMultiCorrect() {
        val data = DialogSelectImage.createInputData(0, 3, 12)
        val expected = intArrayOf(3, 6)
        val scenario = launchFragment<DialogSelectAvatar>(
            fragmentArgs = data,
            themeResId = R.style.Theme_MaterialComponents_DayNight_DarkActionBar
        )
        scenario.onFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResultListener(
                DialogSelectImage.requestResultKey,
                fragment,
                { _, result ->
                    DialogSelectImage.handleResult(result,
                        object : DialogSelectImage.OnResultListener {
                            override fun onEmptyResult() {
                                error("Fail")
                            }
                            override fun onSingleResult(index: Int) {
                                error("Fail")
                            }
                            override fun onMultiResult(array: IntArray) {
                                assert(array.contentEquals(expected))
                            }
                            override fun onError(throwable: Throwable) {
                                throw throwable
                            }
                        })
                })
            fragment.parentFragmentManager.setFragmentResult(DialogSelectImage.requestInputKey, data)
        }
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click()))
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(6, click()))
        onView(withId(R.id.buttonSelectImage)).perform(click())
    }

}