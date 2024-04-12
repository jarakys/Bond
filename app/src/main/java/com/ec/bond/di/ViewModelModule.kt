/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ec.bond.activity.ui.chat.ChatViewModel
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.activity.ui.contactlist.ContactListViewModel
import com.ec.bond.activity.ui.contactlistselect.ContactListSelectViewModel
import com.ec.bond.activity.ui.home.HomeViewModel
import com.ec.bond.activity.ui.imageviewer.ImageViewModel
import com.ec.bond.utils.MasmakViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap


@Suppress("unused")
@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    abstract fun bindUserViewModel(chatViewModel: ChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContactListViewModel::class)
    abstract fun bindContactListViewModel(contactListViewModel: ContactListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChatBrowsingViewModel::class)
    abstract fun bindChatListListViewModel(chatBrowsingViewModel: ChatBrowsingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(homeViewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ImageViewModel::class)
    abstract fun bindImageViewModel(imageViewModel: ImageViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContactListSelectViewModel::class)
    abstract fun bindContactListSelectViewModel(contactListSelectViewModel: ContactListSelectViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MasmakViewModelFactory): ViewModelProvider.Factory
}
