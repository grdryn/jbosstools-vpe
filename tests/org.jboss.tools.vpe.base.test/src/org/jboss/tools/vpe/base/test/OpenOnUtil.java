/*******************************************************************************
 * Copyright (c) 2007-2009 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.base.test;

import java.lang.reflect.Method;
import static junit.framework.Assert.assertEquals;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.jboss.tools.common.model.ui.editor.EditorPartWrapper;
import org.jboss.tools.common.model.ui.texteditors.XMLTextEditorStandAlone;
import org.jboss.tools.common.text.ext.hyperlink.AbstractHyperlink;
import org.jboss.tools.jst.jsp.jspeditor.JSPMultiPageEditor;

/**
 * @author Sergey Dzmitrovich
 * 
 */
public class OpenOnUtil {

	/**
	 * method does open on action in editor
	 * 
	 * @param textEditor
	 * @param lineNumber
	 * @param lineOffset
	 * @throws Throwable
	 */
	public static final void performOpenOnAction(
			StructuredTextEditor textEditor, int offset) throws Throwable {

		// hack to get hyperlinks detectors, no other was have been founded
		Method method = AbstractTextEditor.class
				.getDeclaredMethod("getSourceViewerConfiguration"); //$NON-NLS-1$
		method.setAccessible(true);
		SourceViewerConfiguration sourceViewerConfiguration = (SourceViewerConfiguration) method
				.invoke(textEditor);
		IHyperlinkDetector[] hyperlinkDetectors = sourceViewerConfiguration
				.getHyperlinkDetectors(textEditor.getTextViewer());

		for (IHyperlinkDetector iHyperlinkDetector : hyperlinkDetectors) {
			IHyperlink[] hyperLinks = iHyperlinkDetector.detectHyperlinks(
					textEditor.getTextViewer(), new Region(offset, 0), false);
			if (hyperLinks != null && hyperLinks.length > 0
					&& hyperLinks[0] instanceof AbstractHyperlink) {
				AbstractHyperlink abstractHyperlink = (AbstractHyperlink) hyperLinks[0];
				abstractHyperlink.open();
				break;
			}
		}

	}
	
	/**
	 * Function for checking openOn functionality
	 * 
	 * @param editorInput
	 * @param editorId
	 * @param lineNumber
	 * @param lineOffset
	 * @param openedOnFileName
	 * @throws Throwable
	 * 
	 * @author mareshkau
	 */
	
	public static final void checkOpenOnInEditor(IEditorInput editorInput,String editorId,int lineNumber, int lineOffset, String openedOnFileName) throws Throwable {
		StructuredTextEditor textEditor = getStructuredTextEditorPart(editorInput, editorId);
		int openOnPosition = TestUtil.getLinePositionOffcet(
				textEditor.getTextViewer(), lineNumber, lineOffset);
		// hack to get hyperlinks detectors, no other was have been founded
		Method method = AbstractTextEditor.class
				.getDeclaredMethod("getSourceViewerConfiguration"); //$NON-NLS-1$
		method.setAccessible(true);
		SourceViewerConfiguration sourceViewerConfiguration = (SourceViewerConfiguration) method
				.invoke(textEditor);
		IHyperlinkDetector[] hyperlinkDetectors = sourceViewerConfiguration
				.getHyperlinkDetectors(textEditor.getTextViewer());
		for (IHyperlinkDetector iHyperlinkDetector : hyperlinkDetectors) {
			IHyperlink[] hyperLinks = iHyperlinkDetector.detectHyperlinks(
					textEditor.getTextViewer(), new Region(openOnPosition, 0),
					false);
			if (hyperLinks != null && hyperLinks.length > 0
					&& hyperLinks[0] instanceof AbstractHyperlink) {
				AbstractHyperlink abstractHyperlink = (AbstractHyperlink) hyperLinks[0];
				abstractHyperlink.open();
				break;
			}
		}
		IEditorPart activeEditor = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		assertEquals("Active page should be ",  //$NON-NLS-1$
				openedOnFileName, activeEditor.getEditorInput().getName());
	}
	
	private static StructuredTextEditor getStructuredTextEditorPart(IEditorInput editorInput,String editorId) throws PartInitException{
		IEditorPart editorPart = PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.openEditor(editorInput, editorId);
		StructuredTextEditor textEditor = null;
		/*
		 * https://issues.jboss.org/browse/JBIDE-9808
		 * After "JSPMultiPageEditorPart extends MultiPageEditorPart"
		 * the comparison condition should be updated.
		 */
		if (editorPart instanceof JSPMultiPageEditor) {
			textEditor = ((JSPMultiPageEditor) editorPart).getSourceEditor();
		} else if (editorPart instanceof MultiPageEditorPart) {
			StructuredTextEditor structuredTextEditor = findStructEditor((MultiPageEditorPart) editorPart, editorInput);
			((MultiPageEditorPart) editorPart).setActiveEditor(structuredTextEditor);
			textEditor = structuredTextEditor;
		} else if (editorPart instanceof EditorPartWrapper
				&& (((EditorPartWrapper) editorPart).getEditor()) instanceof MultiPageEditorPart) {
			StructuredTextEditor structuredTextEditor = findStructEditor((MultiPageEditorPart) 
					((EditorPartWrapper) editorPart).getEditor(), editorInput);
			((MultiPageEditorPart) ((EditorPartWrapper) editorPart).getEditor()).setActiveEditor(structuredTextEditor);
			textEditor = structuredTextEditor;
		} else if (editorPart instanceof EditorPartWrapper
				&& (((EditorPartWrapper) editorPart).getEditor()) instanceof StructuredTextEditor) {
			textEditor = (StructuredTextEditor) (((EditorPartWrapper) editorPart).getEditor());
		}
		return textEditor;
	}

	private static StructuredTextEditor findStructEditor (MultiPageEditorPart part, IEditorInput input){
		IEditorPart[] editorParts = part.findEditors(input);
		for (int i = 0; i < editorParts.length; i++) {
			if (editorParts[i] instanceof StructuredTextEditor) {
				return (StructuredTextEditor) editorParts[i];
			}
		}
		return null;
	}
	
}
