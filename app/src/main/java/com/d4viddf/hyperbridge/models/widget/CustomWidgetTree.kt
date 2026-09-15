package com.d4viddf.hyperbridge.models.widget

/**
 * Pure tree-rewrite helpers backing the Studio's property inspector: every edit is
 * "find the node with this id and transform it" without the caller having to walk the tree.
 */

fun CustomWidgetDocument.replaceNode(id: String, transform: (CustomWidgetNode) -> CustomWidgetNode): CustomWidgetDocument {
    val newRoot = root.replaceNode(id, transform) as? LayoutContainer ?: root
    return copy(root = newRoot)
}

fun CustomWidgetNode.replaceNode(id: String, transform: (CustomWidgetNode) -> CustomWidgetNode): CustomWidgetNode {
    if (this.id == id) return transform(this)
    return when (this) {
        is LayoutContainer -> copy(children = children.map { it.replaceNode(id, transform) })
        else -> this
    }
}

fun CustomWidgetDocument.addChild(parentId: String, child: CustomWidgetNode): CustomWidgetDocument {
    val newRoot = root.addChild(parentId, child) as? LayoutContainer ?: root
    return copy(root = newRoot)
}

fun CustomWidgetNode.addChild(parentId: String, child: CustomWidgetNode): CustomWidgetNode {
    return when (this) {
        is LayoutContainer -> if (this.id == parentId) {
            copy(children = children + child)
        } else {
            copy(children = children.map { it.addChild(parentId, child) })
        }
        else -> this
    }
}

fun CustomWidgetDocument.removeNode(id: String): CustomWidgetDocument {
    if (root.id == id) return this // never remove the root
    return copy(root = root.removeNode(id) as? LayoutContainer ?: root)
}

fun CustomWidgetNode.removeNode(id: String): CustomWidgetNode {
    return when (this) {
        is LayoutContainer -> copy(children = children.filter { it.id != id }.map { it.removeNode(id) })
        else -> this
    }
}

fun CustomWidgetNode.findNode(id: String): CustomWidgetNode? {
    if (this.id == id) return this
    if (this is LayoutContainer) {
        for (child in children) {
            child.findNode(id)?.let { return it }
        }
    }
    return null
}

fun CustomWidgetDocument.findNode(id: String): CustomWidgetNode? = root.findNode(id)

/** Recursively counts every node in the tree, root included. */
fun CustomWidgetNode.countNodes(): Int {
    var count = 1
    if (this is LayoutContainer) {
        children.forEach { count += it.countNodes() }
    }
    return count
}
