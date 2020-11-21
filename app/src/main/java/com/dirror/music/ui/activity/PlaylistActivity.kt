package com.dirror.music.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.recyclerview.widget.LinearLayoutManager
import com.dirror.music.MyApplication
import com.dirror.music.R
import com.dirror.music.adapter.DetailPlaylistAdapter
import com.dirror.music.music.netease.Playlist
import com.dirror.music.music.netease.PlaylistUtil
import com.dirror.music.music.standard.SongPicture
import com.dirror.music.music.standard.StandardSongData
import com.dirror.music.ui.base.BaseActivity
import com.dirror.music.ui.dialog.PlaylistDialog
import com.dirror.music.util.*
import kotlinx.android.synthetic.main.activity_playlist.*
import kotlinx.android.synthetic.main.layout_play.view.*

/**
 * 歌单 Activity
 * 最新要求：兼容 网易和 QQ
 */
class PlaylistActivity : BaseActivity(R.layout.activity_playlist) {

    private lateinit var musicBroadcastReceiver: MusicBroadcastReceiver // 音乐广播接收

    override fun initData() {
        val intentFilter = IntentFilter() // Intent 过滤器
        intentFilter.addAction("com.dirror.music.MUSIC_BROADCAST") // 只接收 "com.dirror.foyou.MUSIC_BROADCAST" 标识广播
        musicBroadcastReceiver = MusicBroadcastReceiver() //
        registerReceiver(musicBroadcastReceiver, intentFilter) // 注册接收器
    }

    override fun initView() {
        val playlistId = intent.getLongExtra("long_playlist_id", -1)

        initPlaylistInfo(playlistId)
        initPlaylist(playlistId) {
            initRecycleView(it)
            // ivBackground.visibility = View.INVISIBLE
        }
    }

    override fun initListener() {
        includePlay.setOnClickListener {
            startActivity(Intent(this, PlayActivity::class.java))
            overridePendingTransition(
                R.anim.anim_slide_enter_bottom,
                R.anim.anim_no_anim
            )
        }
        includePlay.ivPlay.setOnClickListener {
            MyApplication.musicBinderInterface?.changePlayState()
        }
        includePlay.ivPlaylist.setOnClickListener {
            PlaylistDialog(this).show()
        }
        clNav.setOnClickListener {
            detailPlaylistAdapter.playFirst()
        }
    }

    /**
     * 初始化歌单信息
     */
    private fun initPlaylistInfo(id: Long) {
        PlaylistUtil.getPlaylistInfo(id) {
            it.coverImgUrl?.let { it1 -> GlideUtil.load(it1, ivCover) }
            runOnUiThread {
                tvName.text = it.name
                tvDescription.text = it.description
            }
        }
    }

    inner class MusicBroadcastReceiver: BroadcastReceiver() {
        // 接收
        override fun onReceive(context: Context, intent: Intent) {
            refreshLayoutPlay()
            refreshPlayState()
        }
    }

    private fun refreshPlayState() {
        if (MyApplication.musicBinderInterface?.getPlayState() == true) {
            includePlay.ivPlay.setImageResource(R.drawable.ic_bq_control_pause)
        } else {
            includePlay.ivPlay.setImageResource(R.drawable.ic_bq_control_play)
        }
    }

    override fun onStart() {
        super.onStart()
        refreshLayoutPlay()
        refreshPlayState()
    }

    /**
     * 刷新下方播放框
     * 可能导致 stick 丢失
     */
    private fun refreshLayoutPlay() {
        MyApplication.musicBinderInterface?.getNowSongData()?.let { standardSongData ->
            includePlay.tvName.text = standardSongData.name
            includePlay.tvArtist.text = standardSongData.artists?.let { parseArtist(it) }
            GlideUtil.load(SongPicture.getSongPictureUrl(standardSongData, SongPicture.TYPE_LARGE)) {

                    includePlay.ivCover.setImageBitmap(it)

            }
            // GlideUtil.load(SongPicture.getSongPictureUrl(standardSongData, SongPicture.TYPE_LARGE), includePlay.ivCover, includePlay.ivCover)
        }
//        if (song != null) {
//            GlideUtil.load(SongPicture.getSongPictureUrl(song, SongPicture.TYPE_LARGE), includePlay.ivCover, includePlay.ivCover)
//            includePlay.tvName.text = song.name
//            includePlay.tvArtist.text = song.artists?.let { parseArtist(it) }
//        }
    }

    private fun initPlaylist(id: Long, success: (ArrayList<StandardSongData>) -> Unit) {
        Playlist.getPlaylist(id, {
            success.invoke(it)
        }, {

        })
    }

    var detailPlaylistAdapter = DetailPlaylistAdapter(ArrayList())

    private fun initRecycleView(songList: ArrayList<StandardSongData>) {
        runOnMainThread {
            val linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this@PlaylistActivity)
            detailPlaylistAdapter = DetailPlaylistAdapter(songList)
            rvPlaylist.layoutManager =  linearLayoutManager
            rvPlaylist.adapter = detailPlaylistAdapter
            tvPlayAll.text = "播放全部(${songList.size})"

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 解绑
        unregisterReceiver(musicBroadcastReceiver)
    }


}