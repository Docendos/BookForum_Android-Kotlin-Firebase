package com.example.bookapp

import android.widget.Filter

class FilterPdfAdmin : Filter {

    var filterList: ArrayList<ModelPdf>
    var adapterBookAdmin: AdapterPdfAdmin

    constructor(filterList: ArrayList<ModelPdf>, adapterBookAdmin: AdapterPdfAdmin) : super() {
        this.filterList = filterList
        this.adapterBookAdmin = adapterBookAdmin
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint:CharSequence? = constraint
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().lowercase()
            var filteredModels = ArrayList<ModelPdf>()

            for (i in filterList.indices) {
                if (filterList[i].title.lowercase().contains(constraint)) {
                    filteredModels.add(filterList[i])
                }
            }

            results.count = filteredModels.size
            results.values = filteredModels

        } else {
            results.count = filterList.size
            results.values = filterList
        }

        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        adapterBookAdmin.bookArrayList = results!!.values as ArrayList<ModelPdf>
        adapterBookAdmin.notifyDataSetChanged()
    }

}