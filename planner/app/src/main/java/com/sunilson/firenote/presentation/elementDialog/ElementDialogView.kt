package com.sunilson.firenote.presentation.elementDialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.LinearLayout
import com.sunilson.firenote.R
import com.sunilson.firenote.data.models.Category
import com.sunilson.firenote.presentation.adapters.CategorySpinnerAdapter
import com.sunilson.firenote.presentation.adapters.ColorAddArrayAdapter
import com.sunilson.firenote.presentation.shared.colors
import com.sunilson.firenote.presentation.shared.views.ColorElementView
import kotlinx.android.synthetic.main.alertdialog_body_add_element.view.*

open class ElementDialogView(
        context: Context)
    : LinearLayout(context), AdapterView.OnItemSelectedListener {

    private val colorAdapter = ColorAddArrayAdapter(context)
    private val categorySpinnerAdapter = CategorySpinnerAdapter(context)
    private var selectedColor = 0
    var selectedCategory: Category? = null
        protected set
    var title: String
        set(value) = add_element_title.setText(value)
        get() = add_element_title.text.toString()

    private val view: View
    private val imm: InputMethodManager

    init {
        val inflater = LayoutInflater.from(context)
        view = inflater.inflate(R.layout.alertdialog_body_add_element, this, true)
        imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        context.colors().forEach {
            colorAdapter.add(it)
        }

        //Initialize Color List
        colorlist.adapter = colorAdapter
        colorlist.setOnItemClickListener { _, view, position, _ ->
            val colorView = view as ColorElementView
            if (!colorView.isChecked) {
                selectedColor = colorAdapter.getItem(position)!!.color
                colorAdapter.uncheckAll()
                colorAdapter.setCheckedPosition(position)
                colorView.isChecked = true
            }
        }
        colorlist.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                imm.hideSoftInputFromWindow(add_element_title.windowToken, 0)
            }

            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}
        })
        add_element_categorySpinner.setOnTouchListener { _, _ ->
            add_element_title.clearFocus()
            add_element_categorySpinner.requestFocus()
            false
        }

        //Initialize Category List
        add_element_categorySpinner.adapter = categorySpinnerAdapter
        add_element_categorySpinner.onItemSelectedListener = this
        add_element_categorySpinner.prompt = resources.getString(R.string.spinner_prompt)

        add_element_title.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) imm.hideSoftInputFromWindow(v.windowToken, 0)
        }

        selectColor(0)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedCategory = (parent?.adapter as CategorySpinnerAdapter).data[position]
    }

    protected fun selectColor(position: Int) {
        colorAdapter.setCheckedPosition(position)
        (colorAdapter.getView(position, null, null) as ColorElementView).isChecked = true
        selectedColor = colorAdapter.getItem(position).color
    }
}