package com.example.player.model

import androidx.annotation.StringRes
import com.example.player.R

enum class SortOption(@StringRes val labelRes: Int) {
    DATE_DESC(R.string.sort_date_desc),
    DATE_ASC(R.string.sort_date_asc),
    NAME_ASC(R.string.sort_name_asc),
    SIZE_DESC(R.string.sort_size_desc),
    DURATION_DESC(R.string.sort_duration_desc)
}
