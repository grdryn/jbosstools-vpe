/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.vpe.ui.palette.model;

import org.jboss.tools.common.model.XModelObject;

public class PaletteModelHelper {

	public static boolean isGroup(XModelObject o) {
		return o.getAttributeValue("element type").equals("group"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static boolean isSubGroup(XModelObject o) {
		return o.getAttributeValue("element type").equals("sub-group"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	

}
