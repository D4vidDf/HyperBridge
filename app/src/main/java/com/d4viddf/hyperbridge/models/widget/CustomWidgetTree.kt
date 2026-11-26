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
            val newChildren = children + child
            val withChildren = copy(children = newChildren)
            if (this.id == "root") {
                withChildren
            } else {
                withChildren.copy(
                    bounds = bounds.copy(
                        widthDp = withChildren.adaptedContentWidth() ?: bounds.widthDp,
                        heightDp = withChildren.adaptedContentHeight() ?: bounds.heightDp
                    )
                )
            }
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
        is LayoutContainer -> {
            val remaining = children.filter { it.id != id }.map { it.removeNode(id) }
            val withRemaining = copy(children = remaining)
            if (this.id == "root") {
                withRemaining
            } else {
                withRemaining.copy(
                    bounds = bounds.copy(
                        widthDp = withRemaining.adaptedContentWidth() ?: bounds.widthDp,
                        heightDp = withRemaining.adaptedContentHeight() ?: bounds.heightDp
                    )
                )
            }
        }
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
 * Calculates the absolute Cartesian position (x, y in dp) of a node relative to the canvas root.
 * Root itself is at (0, 0). For any descendant, its absolute position is the sum of its own
 * bounds offset plus the bounds offsets and internal padding of all its ancestor containers below root.
 */
fun CustomWidgetDocument.absolutePositionOf(id: String): Pair<Int, Int> {
    if (id == root.id) return Pair(0, 0)
    val node = findNode(id) ?: return Pair(0, 0)

    fun findAncestors(current: LayoutContainer, targetId: String): List<LayoutContainer>? {
        for (child in current.children) {
            if (child.id == targetId) {
                return listOf(current)
            }
            if (child is LayoutContainer) {
                val subPath = findAncestors(child, targetId)
                if (subPath != null) {
                    return listOf(current) + subPath
                }
            }
        }
        return null
    }

    val ancestors = findAncestors(root, id) ?: emptyList()
    val containerOffsetX = ancestors.drop(1).sumOf { it.bounds.x + it.paddingDp }
    val containerOffsetY = ancestors.drop(1).sumOf { it.bounds.y + it.paddingDp }

    return Pair(containerOffsetX + node.bounds.x, containerOffsetY + node.bounds.y)
}

/**
 * Groups the node into a new LayoutContainer with a generated ID, replacing the node in-place.
 * The child node's relative coordinates are reset to (0, 0) so that its canvas position is preserved.
 */
fun CustomWidgetDocument.groupNode(id: String, layout: ContainerLayout = ContainerLayout.BOX): Pair<CustomWidgetDocument, String?> {
    if (root.id == id) return Pair(this, null)
    val parent = parentOf(id) ?: return Pair(this, null)
    val index = parent.children.indexOfFirst { it.id == id }
    if (index < 0) return Pair(this, null)

    val targetNode = parent.children[index]
    val groupId = java.util.UUID.randomUUID().toString()
    val childInside = targetNode.withBounds(targetNode.bounds.copy(x = 0, y = 0))
    val tempContainer = LayoutContainer(
        id = groupId,
        name = "Group",
        layout = layout,
        bounds = targetNode.bounds,
        children = listOf(childInside)
    )
    val adaptedW = tempContainer.adaptedContentWidth() ?: targetNode.effectiveWidth()
    val adaptedH = tempContainer.adaptedContentHeight() ?: targetNode.effectiveHeight()
    val groupContainer = tempContainer.copy(
        bounds = targetNode.bounds.copy(widthDp = adaptedW, heightDp = adaptedH)
    )

    val updatedChildren = parent.children.toMutableList().apply {
        set(index, groupContainer)
    }
    val updatedParent = parent.copy(children = updatedChildren)
    return Pair(replaceNode(parent.id) { updatedParent }, groupId)
}

/**
 * Dissolves a LayoutContainer and inlines its children into its parent container.
 * Each child's coordinates are converted back to the parent container's coordinate space.
 */
fun CustomWidgetDocument.ungroupNode(containerId: String): CustomWidgetDocument {
    if (root.id == containerId) return this
    val parent = parentOf(containerId) ?: return this
    val index = parent.children.indexOfFirst { it.id == containerId }
    if (index < 0) return this

    val targetNode = parent.children[index] as? LayoutContainer ?: return this
    val shiftedChildren = targetNode.children.map { child ->
        child.withBounds(
            child.bounds.copy(
                x = child.bounds.x + targetNode.bounds.x + targetNode.paddingDp,
                y = child.bounds.y + targetNode.bounds.y + targetNode.paddingDp
            )
        )
    }
    val updatedChildren = parent.children.toMutableList().apply {
        removeAt(index)
        addAll(index, shiftedChildren)
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
 * Moves a node into another container, preserving its visual position on the canvas
 * by translating coordinates between the source and target containers.
 */
fun CustomWidgetDocument.moveInto(nodeId: String, targetContainerId: String): CustomWidgetDocument {
    if (nodeId == root.id || nodeId == targetContainerId) return this
    val nodeToMove = findNode(nodeId) ?: return this
    if (nodeToMove is LayoutContainer && nodeToMove.findNode(targetContainerId) != null) return this
    val targetContainer = findNode(targetContainerId) as? LayoutContainer ?: return this

    val (nodeAbsX, nodeAbsY) = absolutePositionOf(nodeId)
    val (targetAbsX, targetAbsY) = if (targetContainerId == root.id) {
        Pair(0, 0)
    } else {
        val (tx, ty) = absolutePositionOf(targetContainerId)
        Pair(tx + targetContainer.paddingDp, ty + targetContainer.paddingDp)
    }

    val newRelX = nodeAbsX - targetAbsX
    val newRelY = nodeAbsY - targetAbsY
    val adjustedNode = nodeToMove.withBounds(nodeToMove.bounds.copy(x = newRelX, y = newRelY))

    val withoutNode = removeNode(nodeId)
    return withoutNode.addChild(targetContainerId, adjustedNode)
}
