/*
Android Rivers is an app to read and discover news using RiverJs, RSS and OPML format.
Copyright (C) 2012 Dody Gunawinata (dodyg@silverkeytech.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package com.silverkeytech.android_rivers

import android.os.Bundle
import android.util.Log
import android.view.View
import com.actionbarsherlock.app.ActionBar
import com.actionbarsherlock.view.Menu
import com.actionbarsherlock.view.MenuItem
import org.holoeverywhere.app.Activity
import com.slidingmenu.lib.SlidingMenu
import org.holoeverywhere.addon.SlidingMenu.SlidingMenuA

import com.silverkeytech.android_rivers.meta_weblog.showBlogConfigurationDialog
import com.silverkeytech.android_rivers.meta_weblog.showPostBlogDialog

import android.support.v4.app._HoloActivity
import org.holoeverywhere.ArrayAdapter
import com.silverkeytech.android_rivers.meta_weblog.Blog
import java.util.Random
import android.content.Intent

enum class MainActivityMode {
    RIVER
    RSS
    COLLECTION
    PODCASTS
}

public open class MainWithFragmentsActivity(): Activity() {
    class object {
        public val TAG: String = javaClass<MainWithFragmentsActivity>().getSimpleName()
        public val REPLACEMENT_HOME_ID : Int = 16908332
    }

    val DEFAULT_SUBSCRIPTION_LIST = "http://hobieu.apphb.com/api/1/default/riverssubscription"

    var mode: MainActivityMode = MainActivityMode.RIVER

    var currentTheme: Int? = null
    var isOnCreate: Boolean = true

    fun getSlidingIconBasedOnTheme(theme : Int) : Int{
        return when (theme){
            R.style.Holo_Theme -> R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark
            R.style.Holo_Theme_Light -> R.drawable.abs__ic_menu_moreoverflow_holo_light
            else -> R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?): Unit {
        currentTheme = this.getVisualPref().getTheme()
        isOnCreate = true
        setTheme(currentTheme!!)

        getMain().flags.reset()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_with_fragments)

        val actionBar: ActionBar = getSupportActionBar()!!
        //actionBar.setIcon(getSlidingIconBasedOnTheme(getSlidingIconBasedOnTheme(currentTheme!!)))
        actionBar.setDisplayShowHomeEnabled(false)

        showAndHide(showRiver = true)
        setTitle()

        //sliding menu setup
        val slidingMenuAddon = requireSlidingMenu()
        val slidingMenu = slidingMenuAddon.getSlidingMenu()!!
        val slidingMenuView = makeMenuView(savedInstanceState)
        slidingMenuAddon.setBehindContentView(slidingMenuView)
        fillSlidingMenuNavigation(getMainNavigationItems(), slidingMenuView,  { item -> handleSideNavigationItemClick(item)})
        slidingMenuAddon.setSlidingActionBarEnabled(true)
        slidingMenu.setEnabled(true)
        slidingMenu.setBehindWidth(computeSideMenuWidth())
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN)
        slidingMenu.setSlidingEnabled(true)
    }

    fun handleSideNavigationItemClick(item : NavItem){
        when (item.id){
            SLIDE_MENU_TRY_OUT ->
                startTryoutActivity(this)
            SLIDE_MENU_WRITE ->{
                showBlogConfigurationDialog(this){
                    res ->
                        showPostBlogDialog(this@MainWithFragmentsActivity){
                            res2 ->
                                val username = res.get(1).value!!
                                val password = res.get(2).value!!
                                val post = res2.get(0).value!!
                                Log.d(TAG, "Username $username - Password $password - Post content $post")

                                startBlogPostingService(this@MainWithFragmentsActivity,
                                        hashMapOf(Params.BLOG_SERVER to "https://androidrivers.wordpress.com/xmlrpc.php",
                                        Params.BLOG_USERNAME to username, Params.BLOG_PASSWORD to password),
                                        hashMapOf(Params.POST_CONTENT to post))
                        }
                }
            }
            SLIDE_MENU_GOOGLE_NEWS ->{
                startGoogleNewsSearchActivity(this@MainWithFragmentsActivity)
            }
            SLIDE_MENU_KAYAK_DEALS ->{
                startKayakFlightDealsActivity(this@MainWithFragmentsActivity)
            }
            SLIDE_MENU_PRAISE ->{
                startActivity(Intent.createChooser(shareActionIntent(getSpreadText(), "http://goo.gl/kShgp"), "Spread Android Rivers") )
            }
            SLIDE_MENU_FEEDBACK ->{
                val prompt = createSingleInputDialog(this@MainWithFragmentsActivity, "Make this better!", "", "Give your feedback here") {
                    dlg, feedback ->
                        if (feedback.isNullOrEmpty()){
                            toastee("Please write your feedback")
                        }else{
                            startOpenEmailActivity(this@MainWithFragmentsActivity, "dodyg@silverkeytech.com", "Make this better!", feedback!!)
                        }
                }
                prompt.show()
            }
            else -> { }
        }
    }

    fun getSpreadText() : String{
        val i = Random(153.toLong()).nextInt(6)
        return when(i){
            0 -> "Android Rivers is awesome"
            1 -> "Get check out this free news reader"
            2 -> "Download Android Rivers"
            3 -> "Try out this amazing news app"
            4 -> "Android Rivers is great"
            5 -> "Anxiety free news reader"
            else -> {
                "Check out this free podcast client for Android"
            }
        }
    }

    protected override fun onCreateConfig(savedInstanceState: Bundle?): _HoloActivity.Holo? {
        val config = super.onCreateConfig(savedInstanceState);
        config!!.requireSlidingMenu = true;
        return config;
    }

    protected override fun onResume() {
        super.onResume()
        //skip if this event comes after onCreate
        if (!isOnCreate){
            Log.d(TAG, "RESUMING Current Theme $currentTheme vs ${this.getVisualPref().getTheme()}")
            //detect if there has been any theme changes
            if (currentTheme != null && currentTheme!! != this.getVisualPref().getTheme()){
                Log.d(TAG, "Theme changes detected - updating theme")
                restart()
            } else {
                showAccordingToCurrentMode()

                if (getMain().flags.isRssJustBookmarked && mode == MainActivityMode.RSS){
                    getMain().flags.reset()
                    Log.d(TAG, "RSS is just bookmarked")
                    //it doesn't do anything at the moment - this is an artifact of old design
                } else
                    if (getMain().flags.isRiverJustBookmarked && mode == MainActivityMode.RIVER){
                        getMain().flags.reset()
                        Log.d(TAG, "River is just bookmarked")
                        //it doesn't do anything at the moment - this is an artifact of old design
                    }
            }
        }else {
            Log.d(TAG, "RESUMING AFTER CREATION")
            isOnCreate = false
        }
    }

    public override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu != null){
            menu.findItem(R.id.main_menu_updates).andHide()

            val backward = menu.findItem(SWITCH_BACKWARD)
            val forward = menu.findItem(SWITCH_FORWARD)
            when(mode){
                MainActivityMode.RIVER -> {
                    backward!!.setEnabled(false)
                }
                MainActivityMode.RSS -> {
                    backward!!.setEnabled(true)
                }
                MainActivityMode.COLLECTION -> {
                    backward!!.setEnabled(true)
                    forward!!.setEnabled(false)
                }
                MainActivityMode.PODCASTS -> {
                    backward!!.setEnabled(true)
                }
                else -> {
                }
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    public override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = getSupportMenuInflater()!!
        inflater.inflate(R.menu.main_fragments_menu, menu)

        //fixed top menu
        menu?.add(0, REPLACEMENT_HOME_ID, 0, ":")
        ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menu?.add(0, SWITCH_BACKWARD, 0, "<")
        ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menu?.add(0, SWITCH_FORWARD, 0, ">")
        ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menu?.add(0, EXPLORE, 0, "+")
        ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        return super.onCreateOptionsMenu(menu)
    }

    fun showAndHide(showRiver: Boolean = false, showRss: Boolean = false, showCollection: Boolean = false, showPodcasts: Boolean = false) {
        val riverFragment = this.findFragmentById(R.id.main_rivers_fragment)
        val rssFragment = this.findFragmentById(R.id.main_rss_fragment)
        val collectionFragment = this.findFragmentById(R.id.main_collection_fragment)
        val podcastsFragment = this.findFragmentById(R.id.main_podcast_fragment)

        val transaction = this.beginFragmentTransaction()

        if (showRiver) transaction.show(riverFragment) else transaction.hide(riverFragment)
        if (showRss) transaction.show(rssFragment) else transaction.hide(rssFragment)
        if (showCollection) transaction.show(collectionFragment) else transaction.hide(collectionFragment)
        if (showPodcasts) transaction.show(podcastsFragment) else transaction.hide(podcastsFragment)

        transaction.commit()
    }

    fun showAccordingToCurrentMode() {
        when(mode){
            MainActivityMode.RIVER -> showAndHide(showRiver = true)
            MainActivityMode.RSS -> showAndHide(showRss = true)
            MainActivityMode.COLLECTION -> showAndHide(showCollection = true)
            MainActivityMode.PODCASTS -> showAndHide(showPodcasts = true)
            else -> {
            }
        }
    }

    fun setTitle() {
        val actionBar = getSupportActionBar()!!
        when(mode){
            MainActivityMode.RIVER -> actionBar.setTitle("Rivers")
            MainActivityMode.RSS -> actionBar.setTitle("RSS")
            MainActivityMode.COLLECTION -> actionBar.setTitle("Collections")
            MainActivityMode.PODCASTS -> actionBar.setTitle("Podcasts")
            else -> {
            }
        }
    }

    public override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.getItemId()) {
            REPLACEMENT_HOME_ID ->{
                requireSlidingMenu().toggle()
                return true
            }
            EXPLORE -> {
                downloadOpml(this, PreferenceDefaults.CONTENT_OUTLINE_MORE_NEWS_SOURCE, "Get more news")
                return true
            }
            SWITCH_FORWARD -> {
                changeModeForward()
                displayModeContent(mode)
                setTitle()
                this.supportInvalidateOptionsMenu()
                return true
            }
            SWITCH_BACKWARD -> {
                changeModeBackward()
                displayModeContent(mode)
                setTitle()
                this.supportInvalidateOptionsMenu()
                return true
            }
            R.id.main_menu_help ->{
                downloadOpml(this, PreferenceDefaults.CONTENT_OUTLINE_HELP_SOURCE, getString(R.string.help)!!)
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    fun displayModeContent(mode: MainActivityMode) {
        when(mode){
            MainActivityMode.RIVER -> {
                showAndHide(showRiver = true)
            }
            MainActivityMode.RSS -> {
                showAndHide(showRss = true)
            }
            MainActivityMode.COLLECTION ->{
                showAndHide(showCollection = true)
            }
            MainActivityMode.PODCASTS -> {
                showAndHide(showPodcasts = true)
            }
            else -> {
            }
        }
    }

    //this is one directional forward
    fun changeModeForward() {
        val currentMode = mode
        mode = when (currentMode){
            MainActivityMode.RIVER -> MainActivityMode.RSS
            MainActivityMode.RSS -> MainActivityMode.PODCASTS
            MainActivityMode.PODCASTS -> MainActivityMode.COLLECTION
            else -> MainActivityMode.RIVER
        }
    }

    //this is one directional bacwkward
    fun changeModeBackward() {
        val currentMode = mode
        mode = when (currentMode){
            MainActivityMode.COLLECTION -> MainActivityMode.PODCASTS
            MainActivityMode.PODCASTS -> MainActivityMode.RSS
            MainActivityMode.RSS -> MainActivityMode.RIVER
            else -> MainActivityMode.RIVER
        }
    }

    val SWITCH_BACKWARD: Int = 0
    val SWITCH_FORWARD: Int = 1
    val EXPLORE: Int = 2

    fun requireSlidingMenu(): SlidingMenuA {
        return requireAddon(javaClass<org.holoeverywhere.addon.SlidingMenu>())!!.activity(this)!!
    }

    fun makeMenuView(savedInstanceState : Bundle?) : View  {
        return getLayoutInflater()!!.inflate(R.layout.main_slide_menu)!!
    }

    fun computeSideMenuWidth() : Int{
        return getResources()!!.getFraction(R.dimen.sliding_menu_width, getResources()!!.getDisplayMetrics()!!.widthPixels, 1).toInt()
    }
}

data class Location(val x: Int, val y: Int)

fun getLocationOnScreen(item: View): Location {
    val loc = intArray(0, 1)
    item.getLocationOnScreen(loc)
    val popupX = loc[0]
    val popupY = loc[1]

    return Location(popupX, popupY)
}