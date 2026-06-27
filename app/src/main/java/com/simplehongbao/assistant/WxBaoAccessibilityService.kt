package com.simplehongbao.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * 微信红包无障碍服务 - 精简版
 * 核心功能：监听微信消息，自动抢红包
 * 相比原版去除了：广告SDK、推送SDK、多余的权限
 */
class WxBaoAccessibilityService : AccessibilityService() {

    companion object {
        // 全局开关
        var isEnabled = false
        // 抢普通红包
        var grabNormalRedPacket = true
        // 抢拼手气红包
        var grabShuffledRedPacket = true
        // 响应延迟（毫秒）：越小越快，推荐 100-500
        var responseDelayMs = 200L
        // 是否忽略已领取过的群（防重复）
        var ignoreGrabbedGroups = false

        private val handler = Handler(Looper.getMainLooper())
    }

    // 记录已抢的红包位置，防止重复抢
    private val grabbedPositions = mutableSetOf<String>()

    override fun onServiceConnected() {
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            // 反馈类型：所有类型
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            // 通知过滤：只监听微信
            notificationTimeout = 100
            // 响应延迟
            classesToSet = null
        }
        serviceInfo.packageNames = arrayOf("com.tencent.mm")
        serviceInfo.touchExplorationStateEnabled = false
        serviceInfo.isRetainWindowOnFullscreen = true
        serviceInfo.flags = AccessibilityServiceInfo.DEFAULT
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        this.accessibilityServiceInfo = serviceInfo

        // 启动时提示
        Toast.makeText(this, "红包助手已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isEnabled) return

        val rootNode = try {
            getRootInActiveWindow()
        } catch (e: Exception) {
            return
        }

        if (rootNode == null) return

        // 检测场景
        when {
            // 1. 检测微信聊天界面 - 发现红包消息
            isInChatWindow(rootNode) -> {
                findAndGrabRedPacket(rootNode)
            }
            // 2. 检测红包详情页 - 点击"Open"按钮
            isInRedPacketDetail(rootNode) -> {
                openRedPacket(rootNode)
            }
            // 3. 检测红包列表页 - 抢下一个
            isInRedPacketList(rootNode) -> {
                grabNextRedPacket(rootNode)
            }
        }

        rootNode.recycle()
    }

    /**
     * 判断是否在微信聊天窗口
     */
    private fun isInChatWindow(rootNode: AccessibilityNodeInfo): Boolean {
        // 检查是否是微信界面
        val packageName = rootNode.packageName?.toString() ?: return false
        if (packageName != "com.tencent.mm") return false

        // 检查是否有聊天列表相关的UI特征
        return findNodeByText(rootNode, "红包") != null ||
                findNodeByContentDesc(rootNode, "红包") != null ||
                findNodeByText(rootNode, "恭喜发财") != null || // 恭喜发财，大吉大利
                findNodeByContentDesc(rootNode, "恭喜发财，大吉大利") != null
    }

    /**
     * 在红包消息列表中查找并点击红包
     */
    private fun findAndGrabRedPacket(rootNode: AccessibilityNodeInfo) {
        // 查找所有包含"红包"文本的节点
        val redPacketNodes = findNodesByText(rootNode, "红包")

        for (node in redPacketNodes) {
            // 获取节点的父级信息作为唯一标识
            val parent = node.parent
            val key = parent?.className?.toString() + "_" +
                    parent?.contentDescription?.toString() + "_" +
                    SystemClock.now()
            
            // 避免重复点击
            if (grabbedPositions.contains(key)) continue

            // 尝试点击
            tryClick(node)
            grabbedPositions.add(key)

            // 限制同时检测的数量
            if (grabbedPositions.size > 50) {
                grabbedPositions.clear()
            }
        }
    }

    /**
     * 判断是否在红包详情页
     */
    private fun isInRedPacketDetail(rootNode: AccessibilityNodeInfo): Boolean {
        val packageName = rootNode.packageName?.toString() ?: return false
        if (packageName != "com.tencent.mm") return false

        return findNodeByText(rootNode, "开") != null ||
                findNodeByContentDesc(rootNode, "开") != null
    }

    /**
     * 在红包详情页点击"开"按钮
     */
    private fun openRedPacket(rootNode: AccessibilityNodeInfo) {
        // 延迟响应，模拟人类操作
        handler.postDelayed({
            val openButton = findNodeByText(rootNode, "开")
            if (openButton != null) {
                tryClick(openButton)
            } else {
                val openDesc = findNodeByContentDesc(rootNode, "开")
                if (openDesc != null) {
                    tryClick(openDesc)
                }
            }
        }, responseDelayMs)
    }

    /**
     * 在红包列表页抢下一个
     */
    private fun isInRedPacketList(rootNode: AccessibilityNodeInfo): Boolean {
        val packageName = rootNode.packageName?.toString() ?: return false
        if (packageName != "com.tencent.mm") return false

        return findNodeByText(rootNode, "微信红包") != null ||
                findNodeByText(rootNode, "领取") != null
    }

    private fun grabNextRedPacket(rootNode: AccessibilityNodeInfo) {
        val grabNodes = findNodesByText(rootNode, "领取")
        for (node in grabNodes) {
            tryClick(node)
            break // 只抢第一个
        }
    }

    /**
     * 尝试点击节点
     */
    private fun tryClick(node: AccessibilityNodeInfo) {
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 尝试向上级查找可点击的父节点
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    break
                }
                parent = parent.parent
            }
        }
    }

    /**
     * 在节点树中查找包含指定文本的节点
     */
    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        return findNodesByText(root, text).firstOrNull()
    }

    /**
     * 查找所有包含指定文本的节点
     */
    private fun findNodesByText(root: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root) { node ->
            node.text?.toString()?.contains(text, ignoreCase = true) == true
        }.forEach { result.add(it) }
        return result
    }

    /**
     * 查找所有包含指定内容描述的节点
     */
    private fun findNodeByContentDesc(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        return findNodesByContentDesc(root, text).firstOrNull()
    }

    private fun findNodesByContentDesc(root: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root) { node ->
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true
        }.forEach { result.add(it) }
        return result
    }

    /**
     * 递归查找所有节点
     */
    private fun findAllNodes(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) {
                result.add(node)
            }

            // 只向下遍历，避免循环
            val childCount = node.childCount
            for (i in 0 until childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return result
    }

    override fun onInterrupt() {
        Toast.makeText(this, "红包助手已中断", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceAboutToDestroy() {
        super.onServiceAboutToDestroy()
        grabbedPositions.clear()
    }
}

// Android 的 SystemClock 兼容
import android.os.SystemClock
