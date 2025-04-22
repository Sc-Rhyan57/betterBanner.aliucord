package com.rhyans.betterbanner

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.patcher.Patcher
import com.aliucord.utils.RxUtils
import com.discord.api.user.User
import com.discord.models.user.CoreUser
import com.discord.stores.StoreUserProfile
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.images.MGImages
import com.discord.stores.StoreStream
import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.Button
import com.aliucord.api.SettingsAPI
import com.aliucord.utils.DimenUtils
import com.aliucord.views.DangerButton
import android.view.View
import android.widget.TextView
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

@AliucordPlugin
class BetterBanners : Plugin() {
    private var updateDisposable: Disposable? = null
    private val bannersData = AtomicReference<List<UserBannerData>>(emptyList())

    override fun start(context: Context) {
        fetchBannerData()
        startPeriodicUpdates()

        // Patch getUserBannerUrl method
        try {
            val userProfile = StoreStream.getUsers().javaClass
            
            Patcher.hook(
                userProfile.getDeclaredMethod(
                    "getUserBannerUrl", 
                    Long::class.java,
                    Int::class.java
                ),
                Hook { callFrame ->
                    val userId = callFrame.args[0] as Long
                    
                    // Check if user has a custom banner in our data
                    val customBanner = bannersData.get().find { it.uid == userId.toString() }
                    
                    // If user doesn't have a default banner but has a custom one, return it
                    if (customBanner != null) {
                        val user = StoreStream.getUsers().getUser(userId)
                        if (user?.getBanner() == null) {
                            callFrame.result = customBanner.img
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            logger.error("Failed to patch getUserBannerUrl", e)
            Utils.showToast("BetterBanners: Falha ao inicializar o plugin", true)
        }
    }

    override fun stop(context: Context) {
        Patcher.unpatchAll()
        updateDisposable?.dispose()
    }

    private fun fetchBannerData() {
        Http.Request("https://raw.githubusercontent.com/Sc-Rhyan57/USERBANNER/refs/heads/main/data.json")
            .execute()
            .subscribeOn(Schedulers.io())
            .subscribe({ response ->
                try {
                    val jsonArray = JSONArray(response.text())
                    val bannerList = ArrayList<UserBannerData>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        bannerList.add(
                            UserBannerData(
                                id = obj.getString("_id"),
                                uid = obj.getString("uid"),
                                img = obj.getString("img"),
                                orientation = obj.getString("orientation")
                            )
                        )
                    }
                    
                    val oldData = bannersData.get()
                    if (oldData != bannerList) {
                        bannersData.set(bannerList)
                        logger.info("BetterBanners: Dados atualizados com sucesso!")
                        Utils.showToast("BetterBanners atualizado!", false)
                    }
                } catch (e: Exception) {
                    logger.error("BetterBanners: Erro ao processar dados", e)
                }
            }, { error ->
                logger.error("BetterBanners: Falha ao buscar dados", error)
                Utils.showToast("BetterBanners: API não respondeu!", true)
            })
    }

    private fun startPeriodicUpdates() {
        updateDisposable?.dispose()
        updateDisposable = Observable.interval(1, 60, TimeUnit.SECONDS)
            .observeOn(Schedulers.io())
            .subscribe({
                fetchBannerData()
            }, { error ->
                logger.error("BetterBanners: Erro no agendamento de atualizações", error)
            })
    }

    override fun getSettingsPage(): Page = Page()

    data class UserBannerData(
        val id: String,
        val uid: String,
        val img: String,
        val orientation: String
    )

    inner class Page : SettingsPage() {
        private val customBannerApiLink: String = "https://betterbanners.vercel.app/v1"
        
        override fun onViewBound(view: View) {
            super.onViewBound(view)
            setActionBarTitle("BetterBanners")
            
            val context = view.context
            
            val reloadBtn = Button(context).apply {
                text = "Recarregar API"
                setOnClickListener {
                    fetchBannerData()
                    Utils.showToast("Tentando recarregar a API de banners...", false)
                }
            }
            
            val discordBtn = Button(context).apply {
                text = "Discord Server"
                setOnClickListener {
                    Utils.openURL("https://discord.gg/SPKDvU9ryW")
                }
            }
            
            val apiLinkText = TextView(context).apply {
                text = "Banner API: $customBannerApiLink"
                setPadding(DimenUtils.defaultPadding, DimenUtils.defaultPadding, DimenUtils.defaultPadding, DimenUtils.defaultPadding)
            }
            
            linearLayout.apply {
                addView(discordBtn)
                addView(reloadBtn)
                addView(apiLinkText)
            }
        }
    }
}
