package com.rhyans.betterbanner

import android.content.Context
import android.view.View
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.Button
import com.aliucord.views.Divider
import com.aliucord.views.TextInput
import com.discord.views.CheckedSetting
import com.lytefast.flexinput.R

class BetterBannerSettings(private val apiUrl: String) : SettingsPage() {
    override fun onViewBound(view: View) {
        super.onViewBound(view)
        
        setActionBarTitle("BetterBanners")
        
        addView(Button(context).apply {
            text = "Discord Server"
            setOnClickListener { 
                Utils.launchUrl("https://discord.gg/SPKDvU9ryW") 
            }
        })
        
        addView(Divider(context))
        
        addView(Button(context).apply {
            text = "Recarregar API"
            setOnClickListener {
                (activity as? Plugin)?.fetchData()
            }
        })
        
        addView(Divider(context))
        
        addView(TextInput(context).apply {
            hint = "API URL"
            text = apiUrl
            isEnabled = false
        })
    }
}
