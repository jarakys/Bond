/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ec.bond.di

import com.ec.bond.activity.ui.chat.ChatFragment
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingFragment
import com.ec.bond.activity.ui.contactlist.ContactListFragment
import com.ec.bond.activity.ui.contactlistselect.ContactListSelectFragment
import com.ec.bond.activity.ui.home.HomeFragment
import com.ec.bond.activity.ui.imageviewer.ImageViewerFragment
import com.ec.bond.activity.ui.picker.PickerFragment

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Suppress("unused")
@Module
abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun contributeChatFragment(): ChatFragment


    @ContributesAndroidInjector
    abstract fun contributeContactListFragment(): ContactListFragment

    @ContributesAndroidInjector
    abstract fun contributeChatListFragment(): ChatBrowsingFragment

    @ContributesAndroidInjector
    abstract fun contributeHomeFragment(): HomeFragment

    @ContributesAndroidInjector
    abstract fun contributeImageViewerFragment(): ImageViewerFragment

    @ContributesAndroidInjector
    abstract fun contributePickerFragment(): PickerFragment

    @ContributesAndroidInjector
    abstract fun contributeContactListSelectFragment(): ContactListSelectFragment
}
