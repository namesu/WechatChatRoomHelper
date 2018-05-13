package com.zdy.project.wechat_chatroom_helper.plugins.main.adapter

import android.database.Cursor
import android.widget.ImageView
import com.gh0u1l5.wechatmagician.spellbook.C
import com.gh0u1l5.wechatmagician.spellbook.WechatGlobal
import com.gh0u1l5.wechatmagician.spellbook.mirror.mm.ui.Classes.MMBaseAdapter
import com.gh0u1l5.wechatmagician.spellbook.mirror.mm.ui.conversation.Classes
import com.gh0u1l5.wechatmagician.spellbook.mirror.mm.ui.conversation.Classes.ConversationWithCacheAdapter
import com.gh0u1l5.wechatmagician.spellbook.util.ReflectionUtil
import com.zdy.project.wechat_chatroom_helper.plugins.PluginEntry
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.text.FieldPosition

object Classes {


    private val WechatClasses by WechatGlobal.wxLazy("WechatClasses") {
        WechatGlobal.wxClasses!!.filter { it.contains(WechatGlobal.wxPackageName) }.map { XposedHelpers.findClass(it, PluginEntry.classloader) }
    }


    val ConversationWithAppBrandListView: Class<*> by WechatGlobal.wxLazy("ConversationWithAppBrandListView") {
        ReflectionUtil.findClassIfExists("${WechatGlobal.wxPackageName}.ui.conversation.ConversationWithAppBrandListView", WechatGlobal.wxLoader)
    }

    val ClassesByCursor by WechatGlobal.wxLazy("ClassesByCursor") {
        WechatClasses.filter { it.interfaces.contains(Cursor::class.java) }
                .flatMap {
                    val clazz = it
                    WechatClasses.filter { it.interfaces.contains(clazz) }
                }
    }

    val SetAvatarClass by WechatGlobal.wxLazy("SetAvatarClass") {
        WechatClasses.filter { it.name.contains("com.tencent.mm.pluginsdk.ui") }
                .filter { it.declaredClasses.isNotEmpty() }
                .firstOrNull { it.declaredClasses.any { it.methods.map { it.name }.contains("doInvalidate") } }
                ?.declaredClasses
                ?.firstOrNull {
                    it.methods.any {
                        it.parameterTypes.isNotEmpty() &&
                                it.parameterTypes[0].name == ImageView::class.java.name
                    }
                }!!
    }
    val SetAvatarMethod by WechatGlobal.wxLazy("SetAvatarMethod") {
        SetAvatarClass.methods.firstOrNull {
            it.parameterTypes.isNotEmpty() && it.parameterTypes[0].name == ImageView::class.java.name
        }!!.name
    }

    val ConversationClickListener: Class<*> by WechatGlobal.wxLazy("ConversationClickListener") {
        ReflectionUtil.findClassesFromPackage(WechatGlobal.wxLoader!!, WechatGlobal.wxClasses!!, "${WechatGlobal.wxPackageName}.ui.conversation")
                .filterByMethod(null, "onItemClick", C.AdapterView, C.View, C.Int, C.Long)
                .firstOrNull()

    }

    val SetConversationString = Classes.ConversationWithCacheAdapter.declaredMethods
            .filter { !it.isAccessible }
            .filter { it.returnType == CharSequence::class.java }
            .firstOrNull { it.parameterTypes.size == 1 }!!

    fun getConversationTimeString(adapter: Any, conversationTime: Long): CharSequence {

        SetConversationString.let {

            val aeClass = XposedHelpers.findClass(it.parameterTypes[0].name, PluginEntry.classloader)
            val constructor = aeClass.constructors.filter { it.parameterTypes.size == 1 }.firstOrNull { it.parameterTypes[0] == String::class.java }

            constructor?.let {
                val obj = constructor.newInstance("")

                aeClass.getField("field_status").set(obj, 0)
                aeClass.getField("field_conversationTime").set(obj, conversationTime)

                return XposedHelpers.callMethod(adapter, SetConversationString.name, obj) as CharSequence
            }
        }
        return ""
    }


    fun getConversationAvatar(field_username: String, imageView: ImageView) =
            XposedHelpers.callStaticMethod(SetAvatarClass, SetAvatarMethod, imageView, field_username)


    fun getConversationContent(adapter: Any, bean: Any, position: Int) {


        val parameterizedType = ConversationWithCacheAdapter.genericSuperclass as ParameterizedType
        val typeArguments = parameterizedType.actualTypeArguments

        val paramAE = (typeArguments[1] as Class<*>)
        var paramD = ConversationWithCacheAdapter.declaredClasses.first { it.fields.map { it.name }.contains("nickName") }


        val getContentMethod = ConversationWithCacheAdapter.methods
                .filter { it.parameterTypes.size == 3 }
                .first {
                    it.parameterTypes[0].simpleName == paramAE.simpleName &&
                            it.parameterTypes[1].simpleName == Int::class.java.simpleName &&
                            it.parameterTypes[2].simpleName == Boolean::class.java.simpleName
                }


//        XposedHelpers.callMethod(adapter,getContentMethod.name,bean,position,)
    }

}