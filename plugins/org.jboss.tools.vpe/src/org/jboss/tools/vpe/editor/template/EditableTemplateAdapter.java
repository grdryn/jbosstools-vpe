/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.editor.template;

import java.util.List;

import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.jboss.tools.vpe.editor.context.VpePageContext;
import org.jboss.tools.vpe.editor.mapping.VpeAttributeData;
import org.jboss.tools.vpe.editor.mapping.VpeElementData;
import org.jboss.tools.vpe.editor.mapping.VpeElementMapping;
import org.jboss.tools.vpe.editor.selection.VpeSelectionController;
import org.jboss.tools.vpe.editor.util.TemplateManagingUtil;
import org.jboss.tools.vpe.editor.util.TextUtil;
import org.jboss.tools.vpe.editor.util.VisualDomUtil;
import org.mozilla.interfaces.nsIDOMElement;
import org.mozilla.interfaces.nsIDOMKeyEvent;
import org.mozilla.interfaces.nsIDOMNode;
import org.mozilla.interfaces.nsISelection;
import org.mozilla.interfaces.nsISelectionController;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Abstract class to handle keyEvent.
 * 
 * Default implementation of ITemplateKeyEventHandler. In result of work
 * handleKeyPress() call one from handle* methods.
 * 
 * You must override some handle* methods if you want change work of your
 * handler *
 * 
 * Default implementation of ITemplateNodesManager.
 * 
 * @author Sergey Dzmitrovich
 * 
 */
public abstract class EditableTemplateAdapter extends VpeAbstractTemplate
		implements ITemplateKeyEventHandler, ITemplateNodesManager,
		ITemplateSelectionManager {

	/**
	 * name of "view" tag
	 */
	private static final String VIEW_TAGNAME = "view"; //$NON-NLS-1$

	/**
	 * name of "locale" attribute
	 */
	private static final String LOCALE_ATTRNAME = "locale"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.vpe.editor.template.VpeTemplateKeyEventHandler#handleKeyPress(org.jboss.tools.vpe.editor.context.VpePageContext,
	 *      org.mozilla.interfaces.nsIDOMKeyEvent)
	 */
	public boolean handleKeyPress(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {

		long keyCode = keyEvent.getKeyCode();

		if (keyCode == nsIDOMKeyEvent.DOM_VK_ENTER) {
			return handleEnter(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_LEFT)
				&& (!keyEvent.getShiftKey())) {
			return handleLeft(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_UP)
				&& (!keyEvent.getShiftKey())) {
			return handleUp(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_RIGHT)
				&& (!keyEvent.getShiftKey())) {
			return handleRight(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_DOWN)
				&& (!keyEvent.getShiftKey())) {
			return handleDown(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_HOME)
				&& (!keyEvent.getShiftKey())) {
			return handleHome(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_END)
				&& (!keyEvent.getShiftKey())) {
			return handleEnd(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_BACK_SPACE)
				&& (!keyEvent.getShiftKey())) {
			return handleLeftDelete(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_DELETE)
				&& (!keyEvent.getShiftKey())) {
			return handleRightDelete(pageContext, keyEvent);

		} else if ((keyCode == nsIDOMKeyEvent.DOM_VK_PAGE_UP)
				&& (!keyEvent.getShiftKey())) {
			return handlePageUp(pageContext, keyEvent);

		} else if (keyEvent.getCharCode() != 0) {
			return handleCharacter(pageContext, keyEvent);

		} else if ((keyEvent.getKeyCode() == nsIDOMKeyEvent.DOM_VK_INSERT)
				&& keyEvent.getShiftKey()) {
			return handleInsert(pageContext, keyEvent);
		}

		return false;
	}

	/**
	 * Default handling of a pressing the "insert" event - always return false.
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleInsert(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		return false;
	}

	/**
	 * Default handling of a pressing a character event
	 * 
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleCharacter(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {

		// get selection
		nsISelection selection = getCurrentSelection(pageContext);

		// get visual node which is focused
		nsIDOMNode visualNode = selection.getFocusNode();

		VpeElementMapping elementMapping = getElmentMapping(pageContext,
				visualNode);

		if (elementMapping == null || elementMapping.getElementData() == null) {
			return false;
		}

		VpeElementData elementData = elementMapping.getElementData();

		// if node editable
		if (isNodeEditable(pageContext, visualNode, elementData)) {

			// get source node
			Node node = getSourceNode(pageContext, visualNode, elementData);

			if (node == null)
				return false;

			// get focus and anchor offsets
			int focusOffset = selection.getFocusOffset();
			int anchorOffset = selection.getAnchorOffset();

			// initialization offset and length selected string
			int startOffset = 0;
			int length = 0;

			// set start offset and length selected string
			if (focusOffset < anchorOffset) {
				startOffset = focusOffset;
				length = anchorOffset - focusOffset;
			} else {
				startOffset = anchorOffset;
				length = focusOffset - anchorOffset;
			}

			// get inserted string
			long charCode = keyEvent.getCharCode();
			char[] s = new char[1];
			s[0] = (char) charCode;
			String str = new String(s);
			if (TextUtil.containsKey(s[0])) {
				str = TextUtil.getValue(s[0]);
			}

			// get value
			String oldValue = node.getNodeValue();

			// create new value
			String newValue = oldValue.substring(0, startOffset) + str
					+ oldValue.substring(startOffset + length);

			node.setNodeValue(newValue);

			// set selection
			setSourceSelection(pageContext, node, startOffset + 1, 0);

		}
		return true;
	}

	/**
	 * Default handling of a pressing the "page up" event - always return false.
	 * 
	 * Override this method for a handling of a pressing the "page up" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handlePageUp(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		return false;
	}

	/**
	 * Default implementation of a handling of a pressing the "delete" event
	 * 
	 * Override this method for a handling of a pressing the "delete" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleRightDelete(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		// get selection
		nsISelection selection = getCurrentSelection(pageContext);

		// get visual node which is focused
		nsIDOMNode visualNode = selection.getFocusNode();

		VpeElementMapping elementMapping = getElmentMapping(pageContext,
				visualNode);

		if (elementMapping == null || elementMapping.getElementData() == null) {
			return false;
		}

		VpeElementData elementData = elementMapping.getElementData();

		// if node editable
		if (isNodeEditable(pageContext, visualNode, elementData)) {

			// get source node
			Node node = getSourceNode(pageContext, visualNode, elementData);

			if (node == null)
				return false;

			// get focus and anchor offsets
			int focusOffset = selection.getFocusOffset();
			int anchorOffset = selection.getAnchorOffset();

			// initialization offset and length selected string
			int startOffset = 0;
			int length = 0;

			// set start offset and length selected string
			// if text was not selected then will be deleted next character
			if (focusOffset == anchorOffset) {

				// if offset is end of text will do nothing
				if (focusOffset == node.getNodeValue().length()) {
					setSourceSelection(pageContext, node, focusOffset, 0);
					return true;
				}

				startOffset = focusOffset;
				length = 1;
			}
			// if some text was selected
			else if (focusOffset < anchorOffset) {
				startOffset = focusOffset;
				length = anchorOffset - focusOffset;
			} else {
				startOffset = anchorOffset;
				length = focusOffset - anchorOffset;
			}

			// get old value
			String oldValue = node.getNodeValue();

			// create new value
			String newValue = oldValue.substring(0, startOffset)
					+ oldValue.substring(startOffset + length);

			// set new value
			node.setNodeValue(newValue);

			// set new selection
			setSourceSelection(pageContext, node, startOffset, 0);

		}

		return true;
	}

	/**
	 * Default handling of a pressing the "backspace" event
	 * 
	 * Override this method to handle the "backspace" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleLeftDelete(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {

		// get selection
		nsISelection selection = getCurrentSelection(pageContext);

		// get visual node which is focused
		nsIDOMNode visualNode = selection.getFocusNode();

		// if node editable
		VpeElementMapping elementMapping = getElmentMapping(pageContext,
				visualNode);

		if (elementMapping == null || elementMapping.getElementData() == null) {
			return false;
		}

		VpeElementData elementData = elementMapping.getElementData();

		// if node editable
		if (isNodeEditable(pageContext, visualNode, elementData)) {

			// get source node
			Node node = getSourceNode(pageContext, visualNode, elementData);

			if (node == null)
				return false;

			// get focus and anchor offsets
			int focusOffset = selection.getFocusOffset();
			int anchorOffset = selection.getAnchorOffset();

			// initialization offset and length selected string
			int startOffset = 0;
			int length = 0;

			// set start offset and length selected string
			// if text was not selected then will be deleted previous character
			if (focusOffset == anchorOffset) {
				// if offset is start of text then will do nothing
				if (focusOffset == 0) {

					setSourceSelection(pageContext, node, 0, 0);
					return true;
				}
				// set start offset to previous character
				startOffset = focusOffset - 1;
				length = 1;
			}
			// if some text was selected
			else if (focusOffset < anchorOffset) {
				startOffset = focusOffset;
				length = anchorOffset - focusOffset;
			} else {
				startOffset = anchorOffset;
				length = focusOffset - anchorOffset;
			}

			// get old value
			String oldValue = node.getNodeValue();

			// create new value
			String newValue = oldValue.substring(0, startOffset)
					+ oldValue.substring(startOffset + length);

			// set new value
			node.setNodeValue(newValue);

			// set new selection
			setSourceSelection(pageContext, node, startOffset, 0);

		}

		return true;
	}

	/**
	 * Default handling of a pressing the "end" event - always return false.
	 * 
	 * Override this method to handle of a pressing the "end" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleEnd(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		return false;
	}

	/**
	 * Default handling of a pressing the "home" event - always return false.
	 * 
	 * Override this method to handle of a pressing the "home" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleHome(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		return false;
	}

	/**
	 * Default handling of a pressing the "down" event - always return false.
	 * 
	 * Override this method to handle of a pressing the "down" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleDown(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		return false;
	}

	/**
	 * Default handling of a pressing the "right" event - always return false.
	 * 
	 * Override this method to handle of a pressing the "right" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleRight(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {

		// get selection
		nsISelection selection = getCurrentSelection(pageContext);

		// get visual node which is focused
		nsIDOMNode visualNode = selection.getFocusNode();

		VpeElementMapping elementMapping = getElmentMapping(pageContext,
				visualNode);

		if (elementMapping == null || elementMapping.getElementData() == null) {
			return false;
		}

		VpeElementData elementData = elementMapping.getElementData();

		// get source node
		Node node = getSourceNode(pageContext, visualNode, elementData);

		if (node == null)
			return false;

		// get focus and anchor offsets
		int focusOffset = selection.getFocusOffset();

		// if node editable
		if (isNodeEditable(pageContext, visualNode, elementData)) {

			if (focusOffset != node.getNodeValue().length()) {
				setSourceSelection(pageContext, node, focusOffset + 1, 0);
			} else
				setSourceSelection(pageContext, node, focusOffset, 0);
		}
		return true;

	}

	/**
	 * Default handling of a pressing the "up" event - always return false.
	 * 
	 * Override this method to handle of a pressing the "up" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleUp(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		return false;
	}

	/**
	 * Default handling of a pressing the "left" event - always return false.
	 * 
	 * Override this method to handle of a pressing the "left" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleLeft(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {

		// get selection
		nsISelection selection = getCurrentSelection(pageContext);

		// get visual node which is focused
		nsIDOMNode visualNode = selection.getFocusNode();

		VpeElementMapping elementMapping = getElmentMapping(pageContext,
				visualNode);

		if (elementMapping == null || elementMapping.getElementData() == null) {
			return false;
		}

		VpeElementData elementData = elementMapping.getElementData();

		// if node editable
		if (isNodeEditable(pageContext, visualNode, elementData)) {

			// get source node
			Node node = getSourceNode(pageContext, visualNode, elementData);

			if (node == null)
				return false;

			// get focus and anchor offsets
			int focusOffset = selection.getFocusOffset();

			if (focusOffset != 0) {
				setSourceSelection(pageContext, node, focusOffset - 1, 0);
			} else {
				setSourceSelection(pageContext, node, 0, 0);
			}
		}
		return true;
	}

	/**
	 * Default handling of a pressing the "enter" event - always return false.
	 * 
	 * Override to handling of a pressing the "enter" event
	 * 
	 * @param pageContext -
	 *            context of vpe
	 * @param keyEvent -
	 *            event
	 * @return whether handled event
	 */
	protected boolean handleEnter(VpePageContext pageContext,
			nsIDOMKeyEvent keyEvent) {
		return true;
	}

	/**
	 * 
	 * @param pageContext
	 * @param visualNode
	 * @param elementData
	 * @return
	 */
	protected VpeAttributeData getAttributeData(VpePageContext pageContext,
			nsIDOMNode visualNode, VpeElementData elementData) {

		// if input data is correct
		if ((visualNode != null) && (elementData != null)
				&& (elementData.getAttributesData() != null)) {

			List<VpeAttributeData> attributesMapping = elementData
					.getAttributesData();

			for (VpeAttributeData attributeData : attributesMapping) {

				// if visual nodes equals
				if (visualNode.equals(attributeData.getVisualAttr()))
					return attributeData;
			}
		}

		return null;

	}

	/**
	 * 
	 * @param pageContext
	 * @param node
	 * @param elementData
	 * @return
	 */
	protected VpeAttributeData getAttributeData(VpePageContext pageContext,
			Node node, VpeElementData elementData) {

		// if input data is correct
		if ((node != null) && (elementData != null)
				&& (elementData.getAttributesData() != null)) {

			List<VpeAttributeData> attributesMapping = elementData
					.getAttributesData();

			for (VpeAttributeData attributeData : attributesMapping) {

				// if source nodes equals
				if (node.equals(attributeData.getSourceAttr()))
					return attributeData;
			}
		}
		return null;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.vpe.editor.template.VpeTemplateNodesManager#getSourceNode(org.jboss.tools.vpe.editor.context.VpePageContext,
	 *      org.mozilla.interfaces.nsIDOMNode)
	 */
	public Node getSourceNode(VpePageContext pageContext,
			nsIDOMNode visualNode, VpeElementData elementData) {

		// get attribute data
		VpeAttributeData attributeData = getAttributeData(pageContext,
				visualNode, elementData);

		if (attributeData != null)
			return attributeData.getSourceAttr();

		return null;
	}

	/**
	 * 
	 * @param pageContext
	 * @param node
	 * @param elementData
	 * @return
	 */
	public nsIDOMNode getVisualNode(VpePageContext pageContext, Node node,
			VpeElementData elementData) {

		VpeAttributeData attributeData = getAttributeData(pageContext, node,
				elementData);
		if (attributeData != null)
			return attributeData.getVisualAttr();

		return null;
	}

	/**
	 * 
	 * @param pageContext
	 * @param visualNode
	 * @param elementData
	 * @return
	 */
	public boolean isNodeEditable(VpePageContext pageContext,
			nsIDOMNode visualNode, VpeElementData elementData) {

		VpeAttributeData attributeData = getAttributeData(pageContext,
				visualNode, elementData);

		if (attributeData != null)
			return attributeData.isEditable();

		return false;
	}

	/**
	 * 
	 */
	public boolean isNodeEditable(VpePageContext pageContext, Node node,
			VpeElementData elementData) {

		VpeAttributeData attributeData = getAttributeData(pageContext, node,
				elementData);

		if (attributeData != null) {
			return attributeData.isEditable();
		}
		return false;
	}

	/**
	 * 
	 * @param pageContext
	 * @param node
	 * @param offset
	 * @param length
	 */
	protected void setSourceSelection(VpePageContext pageContext, Node node,
			int offset, int length) {

		int start = getStartOffsetNode(node);

		pageContext.getSourceBuilder().getStructuredTextViewer()
				.setSelectedRange(start + offset, length);
		pageContext.getSourceBuilder().getStructuredTextViewer().revealRange(
				start + offset, length);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.vpe.editor.template.ITemplateSelectionManager#setSelection(org.jboss.tools.vpe.editor.context.VpePageContext,
	 *      org.mozilla.interfaces.nsISelection)
	 */
	public void setSelection(VpePageContext pageContext, nsISelection selection) {

		nsIDOMNode focusedVisualNode = selection.getFocusNode();

		if (focusedVisualNode == null)
			return;

		VpeElementMapping elementMapping = pageContext.getDomMapping()
				.getNearElementMapping(focusedVisualNode);
		if (elementMapping == null)
			return;

		int focusOffset;
		int length;

		VpeAttributeData attributeData = getAttributeData(pageContext,
				focusedVisualNode, elementMapping.getElementData());

		boolean isEditable = isNodeEditable(pageContext, focusedVisualNode,
				elementMapping.getElementData());

		Node focusedSourceNode;
		if (attributeData == null) {

			focusedSourceNode = elementMapping.getSourceNode();
			focusedVisualNode = elementMapping.getVisualNode();

			focusOffset = 0;
			length = 0;

		} else {

			focusedSourceNode = getSourceNode(pageContext, focusedVisualNode,
					elementMapping.getElementData());

			if (focusedSourceNode == null)
				focusedSourceNode = elementMapping.getSourceNode();

			if (isEditable) {

				focusOffset = selection.getFocusOffset();
				length = selection.getAnchorOffset() - focusOffset;

			} else {

				focusOffset = 0;
				length = getLengthNode(focusedSourceNode);
			}

		}

		setSourceSelection(pageContext, focusedSourceNode, focusOffset, length);

		if (focusedVisualNode.getNodeType() != nsIDOMNode.ELEMENT_NODE)
			focusedVisualNode = focusedVisualNode.getParentNode();

		pageContext.getVisualBuilder().setSelectionRectangle(
				(nsIDOMElement) focusedVisualNode
						.queryInterface(nsIDOMElement.NS_IDOMELEMENT_IID));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.vpe.editor.template.ITemplateSelectionManager#setVisualSelectionBySource(org.jboss.tools.vpe.editor.context.VpePageContext,
	 *      int, int)
	 */
	public void setVisualSelectionBySource(VpePageContext pageContext,
			VpeSelectionController selectionController, int focus, int anchor) {

		// get source node
		Node sourceNode = TemplateManagingUtil.getSourceNodeByPosition(
				pageContext, focus);

		// get element mapping
		VpeElementMapping elementMapping = pageContext.getDomMapping()
				.getNearElementMapping(sourceNode);

		// get focused attribute
		Node focusNode = getFocusedNode(sourceNode, elementMapping
				.getElementData(), focus);

		Node anchorNode = getFocusedNode(sourceNode, elementMapping
				.getElementData(), anchor);

		int visualFocus = 0;
		int visualAnchor = 0;
		nsIDOMNode visualNode = null;
		if ((focusNode == anchorNode)
				&& isNodeEditable(pageContext, focusNode, elementMapping
						.getElementData())) {

			visualNode = getVisualNode(pageContext, focusNode, elementMapping
					.getElementData());
			if (visualNode != null) {
				String text = focusNode.getNodeValue();
				int start = getStartOffsetNode(focusNode);
				focus = focus - start;
				anchor = anchor - start;
				visualFocus = TextUtil.visualInnerPosition(text, focus);
				visualAnchor = TextUtil.visualInnerPosition(text, anchor);
			}

		}

		if (visualNode == null) {
			visualNode = elementMapping.getVisualNode();

		}
		nsISelection selection = selectionController
				.getSelection(nsISelectionController.SELECTION_NORMAL);

		if (visualNode.getNodeType() == nsIDOMNode.TEXT_NODE) {
			selection.collapse(visualNode, visualFocus);

			// if(visualFocus!=visualAnchor)
			// selection.extend(visualNode, visualAnchor );
		}
		else {
			selection.collapse(visualNode, 0);
		}
		if (visualNode.getNodeType() != nsIDOMNode.ELEMENT_NODE) {
			visualNode = visualNode.getParentNode();
		}
		pageContext.getVisualBuilder().setSelectionRectangle(
				(nsIDOMElement) visualNode
						.queryInterface(nsIDOMElement.NS_IDOMELEMENT_IID));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.vpe.editor.template.VpeTemplateNodesManager#getFocusedNode(org.w3c.dom.Node,
	 *      int)
	 */
	public Node getFocusedNode(Node sourceNode, VpeElementData elementData,
			int offset) {

		if ((elementData != null) && (elementData.getAttributesData() != null)) {

			List<VpeAttributeData> attributesMapping = elementData
					.getAttributesData();

			for (VpeAttributeData attributeData : attributesMapping) {

				if ((offset >= (getStartOffsetNode(attributeData
						.getSourceAttr())))
						&& (offset <= (getEndOffsetNode(attributeData
								.getSourceAttr()))))
					return attributeData.getSourceAttr();
			}
		}

		return sourceNode;
	}

	/**
	 * get start offset of node
	 * 
	 * @param node
	 * @return
	 */
	protected int getStartOffsetNode(Node node) {

		if (node instanceof IDOMAttr) {
			return ((IDOMAttr) node).getValueRegionStartOffset() + 1;
		} else if (node instanceof IndexedRegion) {
			return ((IndexedRegion) node).getStartOffset();
		}
		return 0;
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	protected int getLengthNode(Node node) {

		if (node instanceof IDOMAttr) {
			return ((IDOMAttr) node).getValueSource().length();
		} else if (node instanceof IndexedRegion) {
			return ((IndexedRegion) node).getEndOffset()
					- ((IndexedRegion) node).getStartOffset();
		}
		return 0;
	}

	/**
	 * get end offset of node
	 * 
	 * @param node
	 * @return
	 */
	protected int getEndOffsetNode(Node node) {

		if (node instanceof IndexedRegion) {
			return ((IndexedRegion) node).getEndOffset();
		}
		return 0;
	}

	/**
	 * 
	 */
	public boolean openBundle(VpePageContext pageContext,
			nsIDOMNode visualNode, VpeElementData elementData) {

		Node node = getSourceNode(pageContext, visualNode, elementData);

		// so as nsIDOMMouseEvent doesn't give simple selected nsIDOMText as
		// target, but nsiSelection can give simple "text"
		// TODO may be, there is a better way to get selected simple nsIDOMText
		if (node == null) {

			// get selection
			nsISelection selection = pageContext.getEditPart().getController()
					.getXulRunnerEditor().getSelection();

			// get visual node which is focused
			nsIDOMNode tempNode = selection.getFocusNode();

			node = getSourceNode(pageContext, tempNode, elementData);

		}

		if (node == null)
			return false;

		return pageContext.getBundle().openBundle(node.getNodeValue(),
				getPageLocale(pageContext, node));

	}

	/**
	 * 
	 * @param pageContext
	 * @param sourceElement
	 * @return
	 */
	private String getPageLocale(VpePageContext pageContext, Node sourceNode) {

		while (sourceNode != null) {

			if (VIEW_TAGNAME.equals(sourceNode.getLocalName())) {
				break;
			}
			sourceNode = sourceNode.getParentNode();
		}

		if ((sourceNode == null) || !(sourceNode instanceof Element)
				|| !(((Element) sourceNode).hasAttribute(LOCALE_ATTRNAME)))
			return null;

		String locale = ((Element) sourceNode).getAttribute(LOCALE_ATTRNAME);

		return locale;

	}

	/**
	 * 
	 * @param pageContext
	 * @param node
	 * @return
	 */
	protected VpeElementMapping getElmentMapping(VpePageContext pageContext,
			nsIDOMNode node) {

		return pageContext.getDomMapping().getNearElementMapping(node);

	}

	protected nsISelection getCurrentSelection(VpePageContext pageContext) {
		return pageContext.getEditPart().getController().getXulRunnerEditor()
				.getSelection();
	}
}