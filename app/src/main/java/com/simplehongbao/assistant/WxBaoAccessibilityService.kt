package com.simplehongbao.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
        var isEnabled = false
        var grabNormalRedPacket = true
        var grabShuffledRedPacket = true
        var responseDelayMs = 200L
        var ignoreGrabbedGroups = false

        private val handler = Handler(Looper.getMainLooper())
    }

    private val grabbedPositions = mutableSetOf<String>()

    override fun onServiceConnected() {
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            notificationTimeout = 100
        }
        serviceInfo.packageNames = arrayOf("com.tencent.mm")
        serviceInfo.flags = AccessibilityServiceInfo.DEFAULT
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        setServiceInfo(serviceInfo)

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

        when {
            isInChatWindow(rootNode) -> findAndGrabRedPacket(rootNode)
            isInRedPacketDetail(rootNode) -> openRedPacket(rootNode)
            isInRedPacketList(rootNode) -> grabNextRedPacket(rootNode)
        }

        rootNode.recycle()
    }

    private fun isInChatWindow(rootNode: AccessibilityNodeInfo): Boolean {
        val packageName = rootNode.packageName?.toString() ?: return false
        if (packageName != "com.tencent.mm") return false

        return findNodeByText(rootNode, "红包") != null ||
                findNodeByContentDesc(rootNode, "红包") != null ||
                findNodeByText(rootNode, "恭喜发财") != null ||
                findNodeByContentDesc(rootNode, "恭喜发财，大吉大利") != null
    }

    private fun findAndGrabRedPacket(rootNode: AccessibilityNodeInfo) {
        val redPacketNodes = findNodesByText(rootNode, "红包")

        for (node in redPacketNodes) {
            val parent = node.parent
            val key = parent?.className?.toString() + "_" +
                    parent?.contentDescription?.toString() + "_" +
                    System.currentTimeMillis()
            
            if (grabbedPositions.contains(key)) continue

            tryClick(node)
            grabbedPositions.add(key)

            if (grabbedPositions.size > 50) {
                grabbedPositions.clear()
            }
        }
    }

    private fun isInRedPacketDetail(rootNode: AccessibilityNodeInfo): Boolean {
        val packageName = rootNode.packageName?.toString() ?: return false
        if (packageName != "com.tencent.mm") return false

        return findNodeByText(rootNode, "开") != null ||
                findNodeByContentDesc(rootNode, "开") != null
    }

    private fun openRedPacket(rootNode: AccessibilityNodeInfo) {
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
            break
        }
    }

    private fun tryClick(node: AccessibilityNodeInfo) {
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
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

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        return findNodesByText(root, text).firstOrNull()
    }

    private fun findNodesByText(root: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root) { node ->
            node.text?.toString()?.contains(text, ignoreCase = true) == true
        }.forEach { result.add(it) }
        return result
    }

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

    private fun findAllNodes(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) {
                result.add(node)
            }

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

    override fun onDestroy() {
        super.onDestroy()
        grabbedPositions.clear()
    }
}
