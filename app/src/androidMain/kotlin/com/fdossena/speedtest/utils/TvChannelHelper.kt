package com.fdossena.speedtest.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import android.util.Log
import com.fdossena.speedtest.ui.MainActivity
import com.mr.speed.test.R

object TvChannelHelper {
    private const val CHANNEL_NAME = "Mr Speed Quick Actions"
    private const val PREF_TV_CHANNEL_ID = "tv_channel_id"
    private const val PREFS_NAME = "tv_prefs"

    fun updateChannel(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var channelId = prefs.getLong(PREF_TV_CHANNEL_ID, -1L)

        // 1. Create or validate the channel
        if (channelId == -1L || !isChannelExisting(context, channelId)) {
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(CHANNEL_NAME)
                .setAppLinkIntentUri(Uri.parse("mrspeed://home"))
                .build()

            val channelUri = context.contentResolver.insert(
                TvContractCompat.Channels.CONTENT_URI,
                channel.toContentValues()
            )
            if (channelUri == null) return

            channelId = ContentUris.parseId(channelUri)
            prefs.edit().putLong(PREF_TV_CHANNEL_ID, channelId).apply()

            // Set the logo
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mr_speed_largeicon_cyan)
            if (bitmap != null) {
                ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap)
            }

            // Make the channel visible by default
            try {
                Log.d("TvChannelHelper", "Requesting channel browsable for $channelId")
                TvContractCompat.requestChannelBrowsable(context, channelId)
            } catch (e: Exception) {
                Log.e("TvChannelHelper", "Failed to request channel browsable", e)
            }
        } else {
            Log.d("TvChannelHelper", "Channel $channelId already exists and is validated.")
        }

        try {
            Log.d("TvChannelHelper", "Force updating programs for channel $channelId")
            clearPrograms(context, channelId)
            addStartTestProgram(context, channelId)
            addLeaderboardProgram(context, channelId)
        } catch (e: Exception) {
            Log.e("TvChannelHelper", "Failed to update programs", e)
        }
    }

    private fun isChannelExisting(context: Context, channelId: Long): Boolean {
        val cursor = context.contentResolver.query(
            TvContractCompat.buildChannelUri(channelId),
            null, null, null, null
        )
        val exists = cursor != null && cursor.count > 0
        cursor?.close()
        return exists
    }

    private fun clearPrograms(context: Context, channelId: Long) {
        context.contentResolver.delete(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            "${TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID} = ?",
            arrayOf(channelId.toString())
        )
    }

    private fun addStartTestProgram(context: Context, channelId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_TEST, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val program = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE) 
            .setTitle("Run Network Check")
            .setDescription("Verify your Firestick's connection quality and streaming readiness immediately.")
            .setIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))
            .setPosterArtUri(Uri.parse("android.resource://${context.packageName}/drawable/mr_speed_largeicon_cyan"))
            .build()

        context.contentResolver.insert(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            program.toContentValues()
        )
    }

    private fun addLeaderboardProgram(context: Context, channelId: Long) {
        val program = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
            .setTitle("Provider Leaderboard")
            .setDescription("See which ISPs are dominating the global network rankings today.")
            .setIntentUri(Uri.parse("mrspeed://leaderboard"))
            .setPosterArtUri(Uri.parse("android.resource://${context.packageName}/drawable/mr_speed_largeicon_cyan"))
            .build()

        context.contentResolver.insert(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            program.toContentValues()
        )
    }
}
