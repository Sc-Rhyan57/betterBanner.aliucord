package com.rhyans.betterbanner

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.api.user.User
import com.discord.utilities.rest.RestAPI
import org.json.JSONArray

@AliucordPlugin
class BetterBanners : Plugin() {
    private val log = Logger("BetterBanners")
    private var data: JSONArray? = null
    private val apiUrl = "https://raw.githubusercontent.com/Sc-Rhyan57/USERBANNER/main/data.json"
    
    init {
        settingsTab = SettingsTab(BetterBannerSettings::class.java).withArgs(apiUrl)
    }

    @SuppressLint("SetTextI18n")
    override fun start(context: Context) {
        fetchData()
        
        patcher.patch(
            "com.discord.api.user.User",
            "getBanner",
            Hook { it ->
                val user = it.thisObject as User
                val customBanner = data?.find { 
                    it.optString("uid") == user.id.toString() 
                }
                if (user.banner == null && customBanner != null) {
                    return@Hook customBanner.optString("img")
                }
            }
        )
        
        // Atualiza a cada 60 segundos
        Utils.threadPool.scheduleAtFixedRate({ fetchData() }, 60, 60, java.util.concurrent.TimeUnit.SECONDS)
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    private fun fetchData() {
        try {
            val response = Http.Request.new(apiUrl)
                .execute()
                .json(JSONArray::class.java)
            
            if (data?.toString() != response.toString()) {
                data = response
                log.info("Dados atualizados com sucesso!")
                Utils.showToast("BetterBanners atualizado!")
            }
        } catch (e: Throwable) {
            log.error("API não respondeu!", e)
            Utils.showToast("Falha ao atualizar BetterBanners")
        }
    }
}
