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

/**
 * Moves a node [delta] places within its parent. Inside a BOX or ABSOLUTE container the child
 * order *is* the stacking order, so this is what "bring forward" / "send back" does in the
 * Studio's layer list (#328).
 */
fun CustomWidgetDocument.moveNode(id: String, delta: Int): CustomWidgetDocument =
    copy(root = root.moveNode(id, delta) as? LayoutContainer ?: root)

fun CustomWidgetNode.moveNode(id: String, delta: Int): CustomWidgetNode {
    if (this !is LayoutContainer) return this
    val index = children.indexOfFirst { it.id == id }
    if (index < 0) return copy(children = children.map { it.moveNode(id, delta) })

    val target = (index + delta).coerceIn(0, children.lastIndex)
    if (target == index) return this
    val reordered = children.toMutableList()
    reordered.add(target, reordered.removeAt(index))
    return copy(children = reordered)
}

/** The node's parent container, or null for the root. */
fun CustomWidgetNode.parentOf(id: String): LayoutContainer? {
    if (this !is LayoutContainer) return null
    if (children.any { it.id == id }) return this
    children.forEach { child -> child.parentOf(id)?.let { return it } }
    return null
}

fun CustomWidgetDocument.parentOf(id: String): LayoutContainer? = root.parentOf(id)

/**
 * Recursively creates a copy of this node and all of its descendants with freshly generated UUIDs.
 */
fun CustomWidgetNode.withFreshIds(): CustomWidgetNode {
    val newId = java.util.UUID.randomUUID().toString()
    return when (this) {
        is TextNode -> copy(id = newId)
        is ImageNode -> copy(id = newId)
        is ProgressNode -> copy(id = newId)
        is ButtonNode -> copy(id = newId)
        is ShapeNode -> copy(id = newId)
        is LayoutContainer -> copy(
            id = newId,
            children = children.map { it.withFreshIds() }
        )
    }
}

/**
 * Duplicates a node with fresh IDs and inserts it right after the target node in its parent container.
 * If in an ABSOLUTE container, offsets the clone by 8dp so it is immediately visible on the canvas.
 * Returns the updated document and the new node's ID.
 */
fun CustomWidgetDocument.duplicateNode(id: String): Pair<CustomWidgetDocument, String?> {
    if (root.id == id) return Pair(this, null)
    val parent = parentOf(id) ?: return Pair(this, null)
    val index = parent.children.indexOfFirst { it.id == id }
    if (index < 0) return Pair(this, null)

    val targetNode = parent.children[index]
    val clone = targetNode.withFreshIds()
    val adjustedClone = if (parent.layout == ContainerLayout.ABSOLUTE) {
        val bounds = clone.bounds
        clone.withBounds(bounds.copy(x = bounds.x + 8, y = bounds.y + 8))
    } else {
        clone
    }

    val updatedChildren = parent.children.toMutableList().apply {
        add(index + 1, adjustedClone)
    }
    val updatedParent = parent.copy(children = updatedChildren)
    return Pair(replaceNode(parent.id) { updatedParent }, adjustedClone.id)
}

/**
 * Groups the node into a new LayoutContainer with a generated ID, replacing the node in-place.
 */
fun CustomWidgetDocument.groupNode(id: String, layout: ContainerLayout = ContainerLayout.BOX): Pair<CustomWidgetDocument, String?> {
    if (root.id == id) return Pair(this, null)
    val parent = parentOf(id) ?: return Pair(this, null)
    val index = parent.children.indexOfFirst { it.id == id }
    if (index < 0) return Pair(this, null)

    val targetNode = parent.children[index]
    val groupId = java.util.UUID.randomUUID().toString()
    val groupContainer = LayoutContainer(
        id = groupId,
        name = "Group",
        layout = layout,
        bounds = targetNode.bounds,
        children = listOf(targetNode)
    )

    val updatedChildren = parent.children.toMutableList().apply {
        set(index, groupContainer)
    }
    val updatedParent = parent.copy(children = updatedChildren)
    return Pair(replaceNode(parent.id) { updatedParent }, groupId)
}

/**
 * Dissolves a LayoutContainer and inlines its children into its parent container.
 */
fun CustomWidgetDocument.ungroupNode(containerId: String): CustomWidgetDocument {
    if (root.id == containerId) return this
    val parent = parentOf(containerId) ?: return this
    val index = parent.children.indexOfFirst { it.id == containerId }
    if (index < 0) return this

    val targetNode = parent.children[index] as? LayoutContainer ?: return this
    val updatedChildren = parent.children.toMutableList().apply {
        removeAt(index)
        addAll(index, targetNode.children)
    }
    val updatedParent = parent.copy(children = updatedChildren)
    return replaceNode(parent.id) { updatedParent }
}

/**
 * Moves a node to the very top/front of its parent's stacking order.
 */
fun CustomWidgetDocument.moveNodeToFront(id: String): CustomWidgetDocument {
    val parent = parentOf(id) ?: return this
    val index = parent.children.indexOfFirst { it.id == id }
    if (index < 0 || index == parent.children.lastIndex) return this
    val delta = parent.children.lastIndex - index
    return moveNode(id, delta)
}

/**
 * Moves a node to the very back/bottom of its parent's stacking order.
 */
fun CustomWidgetDocument.moveNodeToBack(id: String): CustomWidgetDocument {
    val parent = parentOf(id) ?: return this
    val index = parent.children.indexOfFirst { it.id == id }
    if (index <= 0) return this
    return moveNode(id, -index)
}

/**
 * Moves a node into another container.
 */
fun CustomWidgetDocument.moveInto(nodeId: String, targetContainerId: String): CustomWidgetDocument {
    if (nodeId == root.id || nodeId == targetContainerId) return this
    val nodeToMove = findNode(nodeId) ?: return this
    if (nodeToMove is LayoutContainer && nodeToMove.findNode(targetContainerId) != null) return this

    val withoutNode = removeNode(nodeId)
    return withoutNode.addChild(targetContainerId, nodeToMove)
}
