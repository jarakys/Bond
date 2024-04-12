package com.ec.bond.utils

import androidx.core.content.FileProvider

public class GenericFileProvider() : FileProvider() {
//    // UriMatcher used to match against incoming requests
//    private var uriMatcher: UriMatcher? = null
//    override fun onCreate(): Boolean {
//        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
//
//        // Add a URI to the matcher which will match against the form
//        // 'content://com.task.nousdigital.provider/*'
//        // and return 1 in the case that the incoming Uri matches this pattern
//        uriMatcher!!.addURI(AUTHORITY, "*", 1)
//        return true
//    }
//
//    @Throws(FileNotFoundException::class)
//    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
//        return when (uriMatcher!!.match(uri)) {
//            1 -> {
//
//                // The desired file name is specified by the last segment of the
//                // path
//                // E.g.
//                // 'content://com.task.nousdigital.provider/Test.png'
//                // Take this and build the path to the file in the cache
//                val fileLocation =
//                        (context!!.cacheDir.toString() + File.separator
//                                + uri.lastPathSegment)
//
//                // Create & return a ParcelFileDescriptor pointing to the file
//                // Note: I don't care what mode they ask for - they're only getting
//                // read only
//                ParcelFileDescriptor.open(
//                        File(
//                                fileLocation
//                        ), ParcelFileDescriptor.MODE_READ_ONLY
//                )
//            }
//            else -> throw FileNotFoundException(
//                    "Unsupported uri: "
//                            + uri.toString()
//            )
//        }
//    }
//
//    // //////////////////////////////////////////////////////////////
//    // Not used
//    // //////////////////////////////////////////////////////////////
//    override fun update(
//            uri: Uri, contentvalues: ContentValues?, s: String?,
//            `as`: Array<String>?
//    ): Int {
//        return 0
//    }
//
//    override fun delete(
//            uri: Uri,
//            s: String?,
//            `as`: Array<String>?
//    ): Int {
//        return 0
//    }
//
//    override fun insert(
//            uri: Uri,
//            contentvalues: ContentValues?
//    ): Uri? {
//        return null
//    }
//
//    override fun getType(uri: Uri): String? {
//        return null
//    }
//
//    override fun query(
//            uri: Uri,
//            projection: Array<String>?,
//            s: String?,
//            as1: Array<String>?,
//            s1: String?
//    ): Cursor? {
//        return null
//    }
//
    companion object {
        private const val CLASS_NAME = "CachedFileProvider"

        // The authority is the symbolic name for the provider class
        const val AUTHORITY = "com.spe2eeapp.masmak.fileprovider"
    }
}