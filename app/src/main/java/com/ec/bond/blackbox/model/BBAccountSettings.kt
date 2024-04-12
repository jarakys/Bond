package com.ec.bond.blackbox.model

data class BBAccountSettings(var calendar: String,
                             var language: String,
                             var onlineVisibility: String,
                             var autoDownloadPhotos: String,
                             var autoDownloadAudio : String,
                             var autoDownloadVideos : String,
                             var autoDownloadDocuments : String,
                             var maximumFileSizeMB: String,
                             var supportInAppChatNumber: String,
                             var supportInAppCallNumber: String,
                             var allowedFileType: String,
                             var whiteListedUrls: String,
                             var blackListedUrls: String,
                             var canShareUrl: String
)
