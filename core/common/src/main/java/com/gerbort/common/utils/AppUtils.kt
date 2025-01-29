package com.gerbort.common.utils

import com.gerbort.common.BuildConfig

const val IS_ADMIN_MODE = BuildConfig.FLAVOR == "admin"
const val IS_USER_MODE = BuildConfig.FLAVOR == "user"