package com.scrolllock.app.detection

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object NodeUtils {
    const val MAX_TRAVERSAL_DEPTH = 15
    const val MAX_NODE_COUNT = 200

    fun findNodeById(root: AccessibilityNodeInfo?, viewId: String): AccessibilityNodeInfo? {
        if (root == null) return null
        return findNodeByIdInternal(root, viewId, 0)
    }

    private fun findNodeByIdInternal(node: AccessibilityNodeInfo, viewId: String, depth: Int): AccessibilityNodeInfo? {
        if (depth > MAX_TRAVERSAL_DEPTH) return null
        if (node.viewIdResourceName == viewId) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByIdInternal(child, viewId, depth + 1)
            if (result != null) return result
        }
        return null
    }

    fun findNodesById(root: AccessibilityNodeInfo?, viewId: String): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        findNodesByIdInternal(root, viewId, 0, results)
        return results
    }

    private fun findNodesByIdInternal(node: AccessibilityNodeInfo, viewId: String, depth: Int, results: MutableList<AccessibilityNodeInfo>) {
        if (depth > MAX_TRAVERSAL_DEPTH || results.size >= MAX_NODE_COUNT) return
        if (node.viewIdResourceName == viewId) results.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByIdInternal(child, viewId, depth + 1, results)
        }
    }

    fun getNodeText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        extractText(node, sb, 0)
        return sb.toString()
    }

    private fun extractText(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (depth > MAX_TRAVERSAL_DEPTH) return
        node.text?.let { sb.append(it).append(" ") }
        node.contentDescription?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractText(child, sb, depth + 1)
        }
    }

    fun getContentDescription(node: AccessibilityNodeInfo?): String {
        return node?.contentDescription?.toString() ?: ""
    }

    fun getBounds(node: AccessibilityNodeInfo?): Rect? {
        if (node == null) return null
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }

    fun isVisible(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (!node.isVisibleToUser) return false
        val bounds = getBounds(node) ?: return false
        return bounds.width() > 0 && bounds.height() > 0
    }

    fun isSelected(node: AccessibilityNodeInfo?): Boolean {
        return node?.isSelected == true
    }

    fun findByText(root: AccessibilityNodeInfo?, text: String, exact: Boolean = false): AccessibilityNodeInfo? {
        if (root == null) return null
        return findByTextInternal(root, text, exact, 0)
    }

    private fun findByTextInternal(node: AccessibilityNodeInfo, text: String, exact: Boolean, depth: Int): AccessibilityNodeInfo? {
        if (depth > MAX_TRAVERSAL_DEPTH) return null
        val nodeText = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val match = if (exact) {
            nodeText == text || desc == text
        } else {
            nodeText.contains(text, ignoreCase = true) || desc.contains(text, ignoreCase = true)
        }
        if (match) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findByTextInternal(child, text, exact, depth + 1)
            if (result != null) return result
        }
        return null
    }

    fun countNodesWithId(root: AccessibilityNodeInfo?, viewId: String): Int {
        if (root == null) return 0
        return findNodesById(root, viewId).size
    }

    fun hasDescendantWithId(root: AccessibilityNodeInfo?, viewId: String): Boolean {
        return findNodeById(root, viewId) != null
    }
}
